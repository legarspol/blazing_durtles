# CLAUDE.md — Blazing Durtles

**Read this first, every run.** This is the migration of a legacy Java Android WaniKani client into a clean, Android-first Kotlin app. Full detail: `docs/MIGRATION_PLAN.md` (execution plan) and `docs/AUDIT.md` (inventory + dispositions). This file is the short contract every agent follows.

## Golden rule

**Never touch files outside the ticket's scope list.** Each ticket names the files/packages it may change. Do not edit, rename, move, reformat, or "drive-by fix" anything else — not imports in a neighbouring class, not an unrelated warning, not formatting. If a change seems required outside scope, **stop and flag it** for a separate ticket instead of doing it. Small, in-scope diffs are the whole point (they make review sharp and keep the app releasable between merges).

Migration tickets follow `docs/TICKET_TEMPLATE.md` (Context / Files in scope / Acceptance criteria / Out of scope / Size — no L tickets, split them). Its **Files in scope** list is the scope list this rule refers to. Agent-implementable tickets are labeled `ai-task` + `ready`; human-only tickets are `human-task`. The automated coder → reviewer → fix pipeline is defined in `.github/workflows/migration-bot.yml` and `.github/prompts/*.md`. **Never edit anything under `.github/`** (workflows or prompts), and never touch `review.md` in a normal ticket.

## Stack (do not deviate without an explicit decision)

| Concern | Choice |
|---|---|
| Language | Kotlin (Java migrated incrementally; Java/Kotlin interop must keep building) |
| DI | **Hilt** in `:app`. `:core` carries plain constructor `@Inject` (`javax.inject`) that the `:app` Hilt graph wires — no Hilt/Dagger processor in `:core` (Hilt is Android-only) |
| UI | **Jetpack Compose** (Android idioms first) |
| Persistence | **Room** (Kotlin + KSP). No migrations — fresh install, v68 schema only |
| Networking | **Ktor client + kotlinx.serialization** (replaces `HttpsURLConnection` + Jackson; OkHttp stays as Ktor's engine) |
| Preferences | **DataStore (Preferences)** (replaces SharedPreferences / androidx.preference) |
| Reactive state | **StateFlow / Flow** (replaces LiveData) |
| Async | **Coroutines** (replaces custom AsyncTask, Handler/Looper, both JobIntentService queues) |
| Presentation | **androidx ViewModel/Lifecycle** |
| Images | **None** — Glide is being removed; do not add an image library |
| Crash reporting | **Firebase Crashlytics** |
| Telemetry | **Firebase Analytics** (opt-out + PRIVACY-POLICY.md update ship together) |
| Dates | **kotlinx-datetime** (prefer over `java.time`) |

Not in the stack: SQLDelight, Koin, Retrofit, Moshi, Gson, RxJava, Compose Multiplatform (deferred), Compose string resources.

## Module layout

- **Two modules: `:core` + `:app`.** No `commonMain`/`androidMain` source sets yet; the KMP *platform* split is still deferred, but `:core` is the seam it will extract through.
  - **`:core`** — pure Kotlin/JVM, **no Android SDK on its classpath**. Holds the `…/domain` layer. Framework-freedom is *compiler-enforced* here: `android.*`, `Context`, `View`, and resources simply won't resolve. DI is plain constructor `@Inject` (`javax.inject`), wired by `:app`'s Hilt graph — no Hilt/Dagger processor in `:core`. Its dependencies are deliberately tiny: `javax.inject`, `kotlinx-datetime`, and `kotlinx-coroutines-core` (declared `api` — domain signatures expose `Flow`/`suspend`, so `:app` compiles against it).
  - **`:app`** — the Android module, depends on `:core`. Holds `…/ui` (Compose + ViewModels), `…/platform` (widget, notifications, alarms, WorkManager, `Context`), the still-legacy Java, and — for now — the `…/data` package.
- Package convention (base `com.smouldering_durtles.wk`):
  - `…/domain` (in `:core`) — **Android-framework-free, compiler-enforced**. Platform needs sit behind interfaces implemented in `:app`.
  - `…/data` (in `:app` for now) — avoids the Android *framework* by convention; promotion to its own module is deferred until its Room/Ktor shape settles.
  - `…/ui` — Jetpack Compose screens + ViewModels.
  - `…/platform` — everything Android-specific: widget, notifications, alarms, WorkManager, `Context`.
  - `…/di` — Hilt modules and qualifiers only. Nothing with behaviour lives here.
- Build config (`:app`): `compileSdk 37`, `minSdk 23`, `targetSdk 37`, namespace `com.smouldering_durtles.wk`. (`minSdk` was 21; Firebase BOM 33+ declares `minSdk 23` across every SDK, so Crashlytics/Analytics forced the bump.)
- **All dependency versions live in `gradle/libs.versions.toml`** — declare new ones there and reference them as `libs.*`. Never inline a version string in a build script.

## Code conventions

- **Kotlin, idiomatic.** Constructor injection; no static singletons/god-objects — the old ones (`WkApplication.getInstance()`, `GlobalSettings` statics, `WebClient.getInstance()`, `Session` singleton) become Hilt-injected dependencies.
- **Hilt annotations go on Kotlin classes only.** KSP is the only annotation processor (no KAPT); it can't see Java sources, and Hilt can't be split across processors. So `@HiltAndroidApp`/`@AndroidEntryPoint`/`@HiltViewModel` must be on Kotlin — convert or **wrap** the Java class first (the app entry point does this: `WkHiltApplication : WkApplication()`). `android.disallowKotlinSourceSets=false` in `gradle.properties` lets KSP register sources under AGP 9's built-in Kotlin.
- **One responsibility per class.** Decomposition is a general rule, not just for the god-classes in `AUDIT.md` §7. When porting a Java class, split it (persistence / mapping / domain / presentation / I/O) rather than translating 1:1 — but split by *responsibility*, not line count; leave small cohesive classes alone.
- **Crash-fast.** Do **not** port the legacy `ObjectSupport.safe()` swallow-and-ignore pattern. Let exceptions propagate to Crashlytics. Wrap only at genuine process boundaries (a coroutine worker that must not kill the app), and there **report + log — never silently continue**.
- **Strings as plain Kotlin.** All strings (UI and non-UI) live in typed Kotlin objects (`object XStrings { val y = "…" }`) in the clean packages. No `res/strings.xml`, no `stringResource()`. English-only — no localization.
- **Async via coroutines/Flow**; expose state as `StateFlow` from ViewModels. No `LiveData`, no new `AsyncTask`/`Handler`.
- **Never construct a `CoroutineScope` or reference `Dispatchers.*` directly.** Inject them: `@ApplicationScope CoroutineScope` for fire-and-forget work with no other owner, `@IoDispatcher CoroutineDispatcher` for blocking I/O (both from `…/di/CoroutinesModule`). Work a `ViewModel` or `Lifecycle` already owns belongs on `viewModelScope`/`lifecycleScope` so it is cancelled with its owner. Qualifiers on constructor parameters need an explicit `@param:` target (Dagger reads the parameter; Kotlin is changing the untargeted default — KT-73255). The app scope uses a `SupervisorJob` so one child's failure cannot cancel its siblings and deaden the scope for the rest of the process — but note that isolates *cancellation*, not *failure*: an uncaught exception still crashes the app, which crash-fast wants, so do **not** install a blanket `CoroutineExceptionHandler`.
- Match the surrounding code's naming and idiom; don't reformat untouched code.

## Definition of Done (a ticket is not done until all hold)

1. **In scope only** — diff touches only the ticket's listed files; nothing else changed.
2. **Builds** — `./gradlew assembleDebug` green with Java + Kotlin coexisting.
3. **Tested** — for any non-trivial logic, characterization tests were written *first* (JVM `kotlin.test`/JUnit) to lock current behavior, and `./gradlew test` is green. Behavior parity with the old code unless the ticket says otherwise.
4. **No regressions in the risky paths** — answer grading (`FuzzyMatching`) and SRS scheduling (`SrsSystem`) have explicit before/after parity when touched (they fail silently in the UI).
5. **Conventions honored** — no `safe()` swallowing, no new god-classes, no banned libraries, strings in Kotlin.
6. **App still runs** — the review + lesson end-to-end flow works. For anything with runtime surface, tests are not enough: install and drive it on a device or emulator (`android run --device=<serial> --apks=app/build/outputs/apk/debug/app-debug.apk`, then `android screen capture` / `android layout` to check the result).
7. **Observability** — new code lets exceptions reach Crashlytics; key flows emit the agreed analytics events.

## Phase order (see `docs/MIGRATION_PLAN.md`)

0 Foundation (toolchain + stand up `:core`) → 1 Delete dead code → 2 Data layer → 3 Domain layer → 4 UI (Compose, mostly human). **Burn/resurrect + web scraping are deferred** (lowest priority; kept as legacy Java, instrumented, decided later).

**Where we are:** Phase 0 is **done** — Kotlin/Java interop, Hilt on KSP, Compose, Ktor + kotlinx.serialization, DataStore, coroutines (incl. the app-lifetime `@ApplicationScope`), Crashlytics + Analytics (consent-gated, opt-out shipped), and `:core` all stand up. Room deliberately stays on `annotationProcessor` until the Phase 2 entity/DAO port flips it to KSP.

Phase 1 is **done** — the Room migrations + `DatabaseMigrationTest`, Glide + `welcome.gif`, the vendored jsr305 files, the genuinely-unused classes, and the `Converters`/`ApiTaskService` swallow that silently dropped queued API tasks are all gone. Enabling R8 (#38) is parked as a `human-task`: it is unblocked by the jsr305 deletion but still needs its keep-list settled, and `isMinifyEnabled` is still `false`.

**Phase 2 is next and fully ticketed** (#54–#62). Start with #54 (DAO + `Converters` characterization tests) — #55 is the atomic `db/`→Kotlin + KSP flip and is checked against those tests plus the committed schema hash. Note the exported Room schema at `app/schemas/com.smouldering_durtles.wk.db.AppDatabase/68.json` (`identityHash 6ac2b0d4d7c4b19e7da78e91b2a07dae`) is the safety net for the whole data-layer port: Room verifies it against `room_master_table` on every open, so any diff to that file means existing installs will refuse to launch. Treat an unexplained change to it as a release blocker, and remember KSP needs its own `room.schemaLocation` argument — the `javaCompileOptions` one does not carry over.
