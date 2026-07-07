# CLAUDE.md — Blazing Durtles

**Read this first, every run.** This is the migration of a legacy Java Android WaniKani client into a clean, Android-first Kotlin app. Full detail: `docs/MIGRATION_PLAN.md` (execution plan) and `docs/AUDIT.md` (inventory + dispositions). This file is the short contract every agent follows.

## Golden rule

**Never touch files outside the ticket's scope list.** Each ticket names the files/packages it may change. Do not edit, rename, move, reformat, or "drive-by fix" anything else — not imports in a neighbouring class, not an unrelated warning, not formatting. If a change seems required outside scope, **stop and flag it** for a separate ticket instead of doing it. Small, in-scope diffs are the whole point (they make review sharp and keep the app releasable between merges).

Migration tickets follow `docs/TICKET_TEMPLATE.md` (Context / Files in scope / Acceptance criteria / Out of scope / Size — no L tickets, split them). Its **Files in scope** list is the scope list this rule refers to. Agent-implementable tickets are labeled `ai-task` + `ready`; human-only tickets are `human-task`. The automated coder → reviewer → fix pipeline is defined in `.github/workflows/migration-bot.yml` and `.github/prompts/*.md`. **Never edit anything under `.github/`** (workflows or prompts), and never touch `review.md` in a normal ticket.

## Stack (do not deviate without an explicit decision)

| Concern | Choice |
|---|---|
| Language | Kotlin (Java migrated incrementally; Java/Kotlin interop must keep building) |
| DI | **Hilt** (allowed everywhere, incl. domain/data) |
| UI | **Jetpack Compose** (Android idioms first) |
| Persistence | **Room** (Kotlin + KSP). No migrations — fresh install, v68 schema only |
| Networking | **Ktor client + kotlinx.serialization** (replaces OkHttp + Jackson) |
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

- **Single `:app` module.** No `commonMain`/`androidMain` source sets yet; the KMP split is deferred to future iOS/desktop work.
- Package convention (base `com.smouldering_durtles.wk`):
  - `…/domain`, `…/data` — **Android-framework-free**: no `Context`, `View`, resources, or `android.*` framework imports. Behind interfaces for anything platform-specific. Hilt annotations are OK here.
  - `…/ui` — Jetpack Compose screens + ViewModels.
  - `…/platform` — everything Android-specific: widget, notifications, alarms, WorkManager, `Context`.
- Build config: `compileSdk 35`, `minSdk 21`, `targetSdk 35`, namespace `com.smouldering_durtles.wk`.

## Code conventions

- **Kotlin, idiomatic.** Constructor injection; no static singletons/god-objects — the old ones (`WkApplication.getInstance()`, `GlobalSettings` statics, `WebClient.getInstance()`, `Session` singleton) become Hilt-injected dependencies.
- **One responsibility per class.** Decomposition is a general rule, not just for the god-classes in `AUDIT.md` §7. When porting a Java class, split it (persistence / mapping / domain / presentation / I/O) rather than translating 1:1 — but split by *responsibility*, not line count; leave small cohesive classes alone.
- **Crash-fast.** Do **not** port the legacy `ObjectSupport.safe()` swallow-and-ignore pattern. Let exceptions propagate to Crashlytics. Wrap only at genuine process boundaries (a coroutine worker that must not kill the app), and there **report + log — never silently continue**.
- **Strings as plain Kotlin.** All strings (UI and non-UI) live in typed Kotlin objects (`object XStrings { val y = "…" }`) in the clean packages. No `res/strings.xml`, no `stringResource()`. English-only — no localization.
- **Async via coroutines/Flow**; expose state as `StateFlow` from ViewModels. No `LiveData`, no new `AsyncTask`/`Handler`.
- Match the surrounding code's naming and idiom; don't reformat untouched code.

## Definition of Done (a ticket is not done until all hold)

1. **In scope only** — diff touches only the ticket's listed files; nothing else changed.
2. **Builds** — `./gradlew assembleDebug` green with Java + Kotlin coexisting.
3. **Tested** — for any non-trivial logic, characterization tests were written *first* (JVM `kotlin.test`/JUnit) to lock current behavior, and `./gradlew test` is green. Behavior parity with the old code unless the ticket says otherwise.
4. **No regressions in the risky paths** — answer grading (`FuzzyMatching`) and SRS scheduling (`SrsSystem`) have explicit before/after parity when touched (they fail silently in the UI).
5. **Conventions honored** — no `safe()` swallowing, no new god-classes, no banned libraries, strings in Kotlin.
6. **App still runs** — the review + lesson end-to-end flow works (drive it via the `/run` skill for anything with runtime surface, not just tests).
7. **Observability** — new code lets exceptions reach Crashlytics; key flows emit the agreed analytics events.

## Phase order (see `docs/MIGRATION_PLAN.md`)

0 Foundation (toolchain) → 1 Delete dead code → 2 Data layer → 3 Domain layer → 4 UI (Compose, mostly human). **Burn/resurrect + web scraping are deferred** (lowest priority; kept as legacy Java, instrumented, decided later).
