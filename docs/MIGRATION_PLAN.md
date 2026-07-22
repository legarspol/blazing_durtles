# Blazing Durtles — Migration Plan

## Context

Smouldering Durtles is a ~64k-LOC, 100% Java, single-module Android WaniKani client (fork lineage: Flaming Durtles). The code is old and tangled: god-classes fuse persistence + domain + UI, exceptions are swallowed pervasively via `safe()`, background work runs on a hand-rolled `AsyncTask` plus three separate schedulers, and strings are handled ad-hoc. This plan modernizes it into a clean, idiomatic **Android-first Kotlin app** — shipping as a new install named **"Blazing Durtles"** — using current Jetpack/Kotlin libraries and patterns, architected so a later Kotlin Multiplatform split (for eventual iOS/desktop) is mostly mechanical. Full package-level inventory and dispositions live in `docs/AUDIT.md`; this document is the execution plan.

## Non-goals

- **No internationalization.** The WaniKani API is English-only; the app stays English-only. No locale resources, no translation infrastructure. (This is *why* strings can be plain Kotlin — see below.)
- **No iOS/desktop target yet.** We build and ship **Android only**. KMP-readiness is a *secondary* benefit. We *do* extract the domain layer into a plain-Kotlin **`:core` module now** — so framework-freedom is compiler-enforced and the KMP seam already exists — but we do **not** stand up `commonMain`/`androidMain` source sets or target a second platform. The real KMP extraction happens when iOS/desktop work begins: it turns `:core` into a KMP module and swaps its DI wiring (see DI).
- **No big-bang rewrite.** Migration is incremental; the Android app stays buildable (Java/Kotlin interop) throughout.

> The only intentional *additions* in this migration are **observability** (crash reporting + telemetry) — not user-facing features. Existing behavior is otherwise held at parity.

## Confirmed decisions & stack

| Concern | Decision | Notes |
|---|---|---|
| Module layout | **Two modules: `:core` (pure-Kotlin domain) + `:app` (Android)** | Domain framework-freedom compiler-enforced; `commonMain`/second platform still deferred. Data stays a package in `:app` until its Room/Ktor shape settles. |
| Language | Java → **Kotlin**, incrementally | Java/Kotlin interop keeps the app building mid-migration. |
| DI | **Hilt in `:app`; plain `@Inject` (`javax.inject`) in `:core`** | `:core` classes carry constructor `@Inject` annotations that the `:app` Hilt graph wires — no Hilt/Dagger processor in `:core` (Hilt is Android-only). KMP extraction later swaps the wiring for a KMP DI; the `@Inject` annotations largely carry over. |
| UI | **Jetpack Compose**, Android idioms first | Old Activities/Fragments/Views/adapters/proxy are DELETE, rebuilt screen-by-screen. |
| Persistence | **Room** (Kotlin + KSP) | No migrations — fresh install recreates the current v68 schema only. Room is KMP-capable, so no future DB swap. |
| Networking | **Ktor client + kotlinx.serialization** | Replaces OkHttp + Jackson. (Retrofit+Moshi is the more conventional Android-only pick; Ktor keeps the KMP path open and runs fine on Android via the OkHttp engine.) |
| Preferences | **DataStore (Preferences)** | Replaces SharedPreferences + androidx.preference. **KMP-supported** (1.1.0+, rec 1.2.1) — safe forward choice. |
| Reactive state | **StateFlow / Flow** | Replaces `LiveData`. |
| Async | **Coroutines** | Replaces custom `AsyncTask`, `Handler`/`Looper`, both `JobIntentService` queues. |
| Presentation | **androidx ViewModel/Lifecycle** | KMP-capable artifacts; used as-is now. |
| Images | **None — remove Glide entirely** | Glide's only use is a welcome GIF in `AboutActivity`; subjects have no remote images. Delete the GIF + Glide + glide-transformations; add no replacement. |
| Crash reporting | **Firebase Crashlytics** | Backs the crash-fast policy. |
| Telemetry | **Firebase Analytics** | Feature-usage instrumentation (pairs with Crashlytics). Drives the burn/resurrect keep/drop decision. Requires a privacy-policy update + opt-out. |

## Architecture: two modules, clean packages

Two Gradle modules — **`:core`** (pure Kotlin/JVM, no Android SDK) and **`:app`** (Android, depends on `:core`):

- **`:core`** holds the `…/domain` layer. Framework-freedom is **compiler-enforced**: with no Android SDK on the classpath, `Context`/`View`/resources/`android.*` won't resolve. Prefer `kotlinx-datetime` over `java.time`. Platform needs sit behind interfaces implemented in `:app`. DI is plain constructor `@Inject` (`javax.inject`), wired by the `:app` Hilt graph — no Hilt/Dagger processor here. `:core` is the seam a later KMP split extracts through, so that split is mostly mechanical.
- **`:app`** holds everything Android: `…/ui` (Compose + ViewModels), `…/platform` (widget, notifications, alarms, background workers, `Context`), the still-legacy Java, and — for now — the `…/data` package (Room/Ktor). Data keeps framework-freedom by *convention*; promoting it to its own module is deferred until its shape settles.
- **god-objects → injected services.** The static singletons (`WkApplication.getInstance()`, `GlobalSettings` statics, `WebClient.getInstance()`, the `Session` singleton) become injected dependencies — the precondition for pulling domain out of the framework and into `:core`.

## Decomposition as a general rule (smaller classes → smaller tickets)

Class breakdown is a **first-class goal of this migration, not a special case for the four named god-classes.** `docs/AUDIT.md` §7 (`Subject`, `SubjectSyncDao`, `GlobalSettings`, `Session`, `ObjectSupport`) is the *worst-offender list*, but the same single-responsibility standard applies to every class we touch: as each Java class is ported, split it along its distinct responsibilities (persistence vs. mapping vs. domain vs. presentation vs. I/O) rather than translating a large class 1:1.

- **Rule of thumb:** a ported Kotlin class should have one reason to change. If it mixes I/O, business rules, and formatting, split first, then port the pieces — each piece with its own tests.
- **Ticketing:** these splits are the natural unit of work. Each extracted responsibility (one value object, one service, one DAO trim) is an independently reviewable, independently shippable ticket. Prefer many small "extract X from Y" tickets over one "port Y" epic. This keeps PRs small, review sharp, and the app releasable between merges.
- **Guard against over-splitting:** decompose by *responsibility*, not by line count. Don't manufacture indirection for its own sake; a small, cohesive class stays as one.

## Strings (KMP-like)

- **All strings — UI and non-UI — as plain Kotlin** grouped into typed objects (e.g. `object SessionStrings { val abandon = "Abandon session" }`), in the clean packages. Composables reference the constants directly.
- **No** `res/strings.xml`, no `stringResource()`, no Compose resource system — its value is localization/density, both non-goals. This is the "more KMP-native" handling: strings are already shared-ready, and moving UI to CMP later needs zero string migration.

## Observability: crash-fast + telemetry

**Crash-fast:** prefer an unhandled, *reported* exception over a swallowed one. The legacy `ObjectSupport.safe()` wrappers (468 sites) are **not** migrated:
- **82% (384 sites) are in DELETE-bound UI** — they vanish with the old UI in Phase 4; Compose removes the fragile callback surface that motivated them. Not hand-stripped.
- The **~84 residual sites in ported code** drop the wrapper and let exceptions propagate to **Crashlytics**. Wrap only at genuine process boundaries (a coroutine worker that must not kill the app) — there, **report + log, never silently continue**. The worker-boundary sites (`services`/`tasks`/`jobs`) get individual review during the async rewrite.

**Telemetry (Firebase Analytics):** instrument key flows — session start/finish, sync, lessons/reviews completed, and **especially burn/resurrect usage** (to decide that feature's fate). Add a **settings opt-out** and update `PRIVACY-POLICY.md` before shipping analytics.

## Platform & background work

| Current | Modern form |
|---|---|
| Initial full sync (~1 min, user waiting) | **Foreground service** — implemented as a WorkManager long-running *foreground* worker (shares constraints/retry with periodic sync; a bare `Service` is an acceptable alternative), with a progress notification. |
| `BackgroundSyncWorker` (periodic 1h) | **Stays WorkManager** periodic. |
| `NotificationWorker` + `BackgroundAlarmReceiver` (exact-alarm notifications) | **Stay** Android platform (receiver + notifications), Kotlin + coroutines internally. |
| `SessionWidgetProvider` (home widget) | **Stays** Android `AppWidgetProvider`. |
| `JobRunnerService` + `ApiTaskService` (two `JobIntentService` queues) | **Replaced by coroutines** — in-session work on immediate scopes, deferrable work on WorkManager. |

## Testing strategy: characterize before you port

The app has **no JVM unit tests** today (only instrumented migration tests, being deleted). For every non-trivial unit, **write characterization tests first** (JVM, `kotlin.test`/JUnit) locking current behavior, then port with tests green. Priority targets (highest consequence, zero coverage now): `model/Session` (session state machine), `util/FuzzyMatching` + `OptimalStringAlignmentDistance` (**answer grading**), `model/SrsSystem`/`TimeLine` (SRS math), `api/model/*` DTO round-trips (the Jackson→kotlinx swap's only safety net).

## Phases

Phase numbering follows the agreed order; **Phase 0** is unavoidable foundation preceding "delete dead code."

### Phase 0 — Foundation
Stand up the toolchain without moving logic: add Kotlin, Hilt, Compose, Coroutines, DataStore, Ktor + kotlinx.serialization, **Crashlytics + Firebase Analytics**; register KSP as the only annotation processor (Hilt runs on it from day one). **Create the empty `:core` (pure-Kotlin) module and wire `:app` → `:core`** so later domain tickets have a home. Verify a green build with Java + Kotlin coexisting and one trivial Kotlin+Hilt+Compose+analytics path working end-to-end. *(Hilt's `@HiltAndroidApp` rides a thin Kotlin `WkHiltApplication` wrapper — see the Hilt/KSP note under Risks.)*

**Room stays on `annotationProcessor` until Phase 2 — deliberately.** KSP processes only Kotlin sources, and every Room entity/DAO is still Java, so pointing Room's processor at KSP before those files are Kotlin produces a build with no generated DAO implementations. The Room→KSP switch is therefore *part of* the Phase 2 entity/DAO port, not a Phase 0 toolchain step, and the two must land together. Glide's `annotationProcessor` entry needs no conversion at all — it leaves with Glide in Phase 1.

### Phase 1 — Delete dead code
Remove genuinely unused classes; delete the **40 Room migrations + `DatabaseMigrationTest`** (fresh install → recreate v68 schema only); **delete `welcome.gif`, the `AboutActivity` GIF display, Glide, and glide-transformations**; trim dead defensive scaffolding where safe. Verify: builds, runs, review/lesson smoke path intact.

### Phase 2 — Data layer → Kotlin (clean packages)
Port, tests-first: **DTOs** (Jackson→kotlinx.serialization, incl. custom `PitchInfo`/`WaniKaniApiDate` serializers), **Room entities + DAOs** (Java→Kotlin, suspend/Flow, **flipping Room from `annotationProcessor` to KSP in the same change** — see Phase 0), **REST networking** (OkHttp→Ktor), **preferences storage** (SharedPreferences→DataStore, one-time read-through migration). **DECOMPOSE `SubjectSyncDao`** (`docs/AUDIT.md` §7): lean `@Dao` (its 12 real `@Query`s) + a `SubjectSyncService` for mapping/JSON/sync/availability. Apply the general decomposition rule to every class touched here. *(Burn/resurrect scraping is explicitly out of this phase — see Deferred.)*

### Phase 3 — Domain layer → Kotlin
Port, tests-first (**into `:core`**): `model/*`, `enums/*`, domain `util/*`. **DECOMPOSE** the god-classes (`docs/AUDIT.md` §7): `Subject` (138 methods → thin entity + pure value objects, media/HTML evicted); `Session` (singleton → injected state machine + `QuestionSelector`, UI reach-through dropped); `GlobalSettings` (192 accessors → ~16 per-feature settings objects over DataStore); **`ObjectSupport` DISSOLVES** (helpers→stdlib, `safe()`→deleted, `runAsync`→coroutine facade). Split every other class along its responsibilities as it's ported. Replace the schedulers with coroutines; `LiveData`→`StateFlow`; stand up the **foreground initial-sync worker** and keep periodic sync on WorkManager. Introduce **ViewModels** exposing `StateFlow` for Phase 4.

### Phase 4 — UI screens (mostly a human task)
Rebuild screens in **Jetpack Compose**, one at a time, each backed by a Phase-3 ViewModel; strings from the Kotlin string objects. Delete the matching old Activity/Fragment/View/adapter/proxy as each screen lands — this is where the 384 UI `safe()` sites disappear. Keep Android-only surfaces (`SessionWidgetProvider`, notifications, alarms) in `platform`, updated to the new domain. **Burn/resurrect screens come last** (see Deferred).

## Deferred: burn/resurrect + web scraping

Burn/resurrect are the *only* reason for the `wanikani.com` login-scraping (`WebClient` + jsoup + CSRF token) and the stored **website password** — the WaniKani API v2 doesn't support these operations. **Lowest priority:** leave the existing Java `WebClient` + `BurnActivity` + `ResurrectActivity` working via interop, **instrument their usage via analytics**, and decide *after* real data whether to (a) rebuild them in Compose + port scraping to Ktor/Ksoup, or (b) drop the feature (also dropping jsoup and website-password storage). No scraping-migration effort is spent until that data exists.

## Library swaps (summary)

OkHttp→Ktor · Jackson/Gson→kotlinx.serialization · SharedPreferences/androidx.preference→DataStore · **Glide→removed** · custom `AsyncTask`/`Handler`/`JobIntentService`→Coroutines · `LiveData`→`StateFlow` · `java.time`→kotlinx-datetime · **+Hilt, +Crashlytics, +Firebase Analytics**. Room **stays** (Java→Kotlin+KSP, both in Phase 2). jsoup **stays until the burn/resurrect decision**. See `docs/AUDIT.md` §2 for difficulty ratings.

## Verification

- **Per unit:** characterization tests written first, kept green across the port (`./gradlew test`).
- **Per phase:** app builds (Java/Kotlin interop) and the review + lesson end-to-end flow runs — drive it via the `/run` skill (build, launch, exercise a session), not just tests.
- **Regression watch:** answer-grading (`FuzzyMatching`) and SRS scheduling (`SrsSystem`) get explicit before/after test parity — no UI tell when they silently break.
- **Crash-fast + telemetry check:** an induced exception in ported code surfaces in Crashlytics (not swallowed); key analytics events fire on session/sync/burn-resurrect.

## Risks & sequencing notes

- **JSON swap** is now the single most-exposed change (DB is a like-for-like Room port) — DTO round-trip tests are mandatory before deleting Jackson.
- **Settings interim:** old preference screens (Phase-4 UI) still read settings while storage moves to DataStore in Phase 2/3 — bridge via the injected settings services until each screen's Phase-4 rebuild.
- **Worker-boundary `safe()`** (`services`/`tasks`/`jobs`) needs case-by-case judgment during the async rewrite — legitimate "report, don't crash the process" boundaries.
- **DI at the `:core` boundary:** `:core` uses plain `@Inject` (`javax.inject`) wired by `:app`'s Hilt graph, so there's no Hilt to strip from domain later; the eventual KMP extraction only swaps that wiring for a KMP DI, which the constructor annotations largely survive. Data, still a package in `:app`, keeps framework-freedom by convention until it earns its own module.
- **Hilt annotations must be on Kotlin (KSP constraint):** KSP processes only Kotlin sources, and Hilt's aggregating processor can't be split across javac-`annotationProcessor` and KSP — so every Hilt annotation (`@HiltAndroidApp`, `@AndroidEntryPoint`, `@HiltViewModel`) has to sit on a Kotlin class. Any legacy Java class that needs one is **converted or wrapped first**. Until the `WkApplication` god-class is decomposed, `@HiltAndroidApp` lives on a thin Kotlin subclass `WkHiltApplication : WkApplication()` (registered as `android:name` in the manifest). Also: AGP 9's built-in Kotlin blocks KSP's source-set registration by default — `android.disallowKotlinSourceSets=false` in `gradle.properties` re-allows it.
- **Analytics/privacy:** shipping Firebase Analytics requires the `PRIVACY-POLICY.md` update + opt-out to land *with* the analytics code, not after.
