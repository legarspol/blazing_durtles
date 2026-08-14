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
| Persistence | **Room** (Kotlin + KSP). No migrations — fresh install, v70 schema only |
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
  - `…/ui` — Jetpack Compose screens + ViewModels. **New work groups by feature inside the layer** — `…/ui/onboarding/`, `…/data/onboarding/` — never one vertical `feature/` package spanning layers, since `:core` is a separate module and could not hold the domain half. Shared, non-feature UI (`…/ui/theme/`) stays put. Migrated screens live in **one Compose host `Activity`** with a nav graph (`…/ui/onboarding/OnboardingActivity`), not one `Activity` each.
  - `…/platform` — everything Android-specific: widget, notifications, alarms, WorkManager, `Context`.
  - `…/di` — Hilt modules and qualifiers only. Nothing with behaviour lives here.
- **Layer vocabulary** (full table in `docs/MIGRATION_PLAN.md`). **Do not name a class `*Service`** — in this codebase that already means `android.app.Service`, and it is the suffix god-objects grow under. Name it for what it does.
  - **DAO** (`data`) — one table's SQL, nothing else. **Repository** / **`*Settings` object** (impl in `data`) — the persistence seam; hides Room/Ktor/DataStore from callers. **Domain class** (`domain`) — entities, value objects, and pure logic with no I/O; the default home for anything interesting. **Coordinator** (`data`) — orchestration genuinely spanning repositories or the API. **ViewModel** (`ui`) — `StateFlow` state, no SQL/HTTP/mapping.
  - Pure logic never lives in a coordinator — no I/O means it belongs in `domain`, where it is testable without mocks or an emulator. No blanket use-case layer; add one only when a single operation has real orchestration to justify it.
  - **A domain-side interface only when domain actually reads it** — otherwise `:core` carries interfaces nothing in it calls, and presentation types get dragged in to make the signatures compile. Verified by call-site audit (see `MIGRATION_PLAN.md`): **every settings read in the domain heart is in `Session` or `Question`**, and only `Review`, `AdvancedLesson`, `AdvancedReview`, `AdvancedSelfStudy`, `AdvancedOther` plus the **ungrouped session-dispatch accessors** (`getBackToBack(sessionType)`, `getOrder…`, `getSubjectComparator`, …) qualify. Everything else is read by the UI, data or platform layers.
  - Such interfaces are shaped by the consumer, not by the storage layout — one `SessionPreferences`, modelled on those ungrouped dispatch accessors (already a consumer-shaped facade), not one interface per settings group.
  - Repositories return Room entities/DTOs in Phase 2 and move to domain types in Phase 3, as the domain model comes into existence. `:core` has no Room on its classpath, so a `domain` interface *cannot* mention a Room entity.
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

**Where we are:** Phase 0 is **done** — Kotlin/Java interop, Hilt on KSP, Compose, Ktor + kotlinx.serialization, DataStore, coroutines (incl. the app-lifetime `@ApplicationScope`), Crashlytics + Analytics (consent-gated, opt-out shipped), and `:core` all stand up. Room moved to KSP with the `db/`→Kotlin port (#55); KSP is now the only annotation processor in the build.

Phase 1 is **done** — the Room migrations + `DatabaseMigrationTest`, Glide + `welcome.gif`, the vendored jsr305 files, the genuinely-unused classes, and the `Converters`/`ApiTaskService` swallow that silently dropped queued API tasks are all gone. Enabling R8 (#38) is parked as a `human-task`: it is unblocked by the jsr305 deletion but still needs its keep-list settled, and `isMinifyEnabled` is still `false`.

**Phase 2 is under way.** The Room half is done: characterization tests (#54), the atomic `db/`→Kotlin + KSP flip (#55), and two schema follow-ups — retiring the dead `subject` columns and giving stars a real column (#64, v68→v69), then dropping the obsolete column nullability and the three `*EntityDefinition` shadow classes (#69, v69→v70).

**What is left, in dependency order.** #58 (port `api/model` DTOs to kotlinx.serialization) is the hinge: **#59** (drop Jackson) and **#60** (Ktor replaces `HttpsURLConnection`) both depend on it, and so does the follow-up that tightens the ~31 still-nullable `TEXT` columns — deliberately deferred out of #69, because proving the WaniKani API always returns a value is only answerable once the DTOs are typed. **#56** (suspend/Flow DAO APIs) and **#57** (decompose `SubjectSyncDao`) depend only on #55 and can go at any time. **#61** (SharedPreferences→DataStore) is labeled `blocked`, though its blocker — the API-token storage decision (#62) and its implementation (#74) — has since closed; re-check the label before picking it up. Two open defects sit outside that chain: **#80** (`tryInsertIdOnly` silently drops assignments for unknown subjects — a live sync bug, pinned by a test in #69) and **#72** (`human-task`: build and test every PR, which is where a schema-diff guard belongs).

**Phase 4 has started, out of order.** The onboarding flow is the first screen rebuilt in Compose: `…/ui/theme` (Material 3, bundled Plus Jakarta Sans + JetBrains Mono, light and dark) and `…/ui/onboarding` (a `ComponentActivity` host with a nav graph over Welcome → Connect → Enter token), replacing `NoApiKeyHelpActivity`, which is deleted along with its layout, menu and help document. It establishes the feature-folder convention above and the single-Compose-host pattern. Note what it deliberately does **not** yet do: there is no `data/` layer, no DataStore, no Ktor call and no Hilt in this flow — the token is format-checked (UUID shape) and written straight through `GlobalSettings.Api.setApiKey`, and the welcome-once flag is a seventh boolean in `GlobalSettings.Tutorials`. Network verification of the token is still `GetUserTask`'s job, unchanged. The theme is driven by `isSystemInDarkTheme()` and does **not** yet bridge `ActiveTheme`'s six user-selectable themes — the next screens hosted here will need that.

**Schema baseline: `app/schemas/com.smouldering_durtles.wk.db.AppDatabase/70.json`, version 70, `identityHash f61c2a69655227276c64735ebfb79afa`.** Exactly one file lives in that directory — a bump renames it, it does not accumulate. Update this line in the same commit as any bump; it is the baseline of record and a stale one sends the next agent off a false starting point.

Room verifies that hash against `room_master_table` on every open, and `AppDatabase` calls `fallbackToDestructiveMigration()`, so a mismatch **wipes the database and re-syncs** rather than refusing to launch. The fork has never shipped, so today that costs nothing; once it does ship, each bump also destroys the local-only data (star ratings, search presets) that no sync can restore. Either way the exported schema is the review signal for the whole data-layer port: **nothing in CI checks it**, so an unreviewed diff to that file is the failure mode to watch for. Treat an unexplained change as a release blocker, bundle deliberate schema changes into one bump where you can, and remember KSP needs its own `room.schemaLocation` argument — the `javaCompileOptions` one does not carry over.
