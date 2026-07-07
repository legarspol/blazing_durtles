# Smouldering Durtles — KMP Migration Audit

Decision-oriented audit for porting this app toward Kotlin Multiplatform. Every
row is a disposition, not a description. Raw inventory was gathered by script;
the reasoning below is derived from that output.

## Baseline facts

| Metric | Value |
|---|---|
| Language | 100% Java (0 Kotlin files) |
| Source files (`wk` package) | 351 `.java` (+16 `javax.annotation` stubs) |
| Main LOC | ~63,900 |
| Modules | single `:app` Android application module |
| minSdk / targetSdk / compileSdk | 21 / 35 / 35 |
| App type | WaniKani client (fork lineage: Flaming Durtles) |
| Room schema version | 68 in source, but **migrations are irrelevant** — the new "Blazing Durtles" app is a fresh install with no prior DB. Only the current schema (v68 shape) needs to be recreated; the 40 migrations and their tests are **DELETE**. |
| Tests | 2 instrumented files, 21 methods; **no `src/test` unit dir at all** |

**Disposition legend** (a KMP migration of a Java app — nothing "stays as-is" in
shared code, so KEEP means "keep on the Android target"):

- **KEEP** — remains Android-target code (`androidMain` or the `:app` UI module); do not move to shared.
- **PORT** — mechanical Java→Kotlin translation into `commonMain`; logic preserved. Only appropriate for classes that already have a single responsibility.
- **DECOMPOSE** — a god-class doing several jobs at once; the migration must *split it first*, then port the pieces. Porting it whole would launder a design smell into Kotlin. Enumerated in §8.
- **REWRITE** — moves to shared but must be redesigned against a KMP idiom (coroutines, `expect/actual`, Ktor, kotlinx.serialization).
- **DELETE** — not migrated; replaced by new UI (Compose Multiplatform / platform-native) or dropped.

> **On PORT vs DECOMPOSE:** "port to Kotlin" is the right move for the many small, single-purpose classes (enums, DTOs, value types). It is the *wrong* move for the handful of oversized classes that fuse persistence + domain + mapping + presentation. A KMP split is the natural forcing function: shared `commonMain` wants a clean domain, Room-KMP entities want to be plain row holders, and the pure logic is exactly what we want to unit-test. So the big classes are dispositioned **DECOMPOSE**, not PORT.

> **DB strategy update:** Room now supports KMP (stable since 2.7.0, May 2025; current 2.8.3, Android/iOS/JVM). The DB layer therefore **does not need a SQLDelight rewrite** — it is a **PORT** (Java→Kotlin Room, `annotationProcessor`→KSP), keeping the existing `@Entity`/`@Dao` structure. Combined with "no legacy DB to migrate," the highest-risk subsystem in the original audit is substantially de-risked.

---

## 1. Package dispositions

| Package | Files | LOC | Android-touch | Purpose | Disposition | Justification |
|---|---|---|---|---|---|---|
| `(root wk)` | 8 | 4,621 | 3/8 | App singleton, settings, constants, ID | **SPLIT** | `Constants`/`StableIds`/`Identification` PORT; `GlobalSettings` (3,541 LOC, 192 accessors) **DECOMPOSE** (§7); `WkApplication`/`Actment` KEEP (Android). |
| `activities` | 22 | 5,151 | 21/22 | Android `Activity` screens | **DELETE** | UI shell; replaced by Compose MP or per-platform hosts. No shared value. |
| `fragments` | 13 | 4,242 | 12/13 | Android `Fragment` screens | **DELETE** | Same as activities. |
| `views` | 39 | 8,280 | 38/39 | Custom Android `View` subclasses | **DELETE** | Framework-bound rendering; re-implement in Compose. Largest single sink of throwaway code. |
| `proxy` | 2 | 927 | 1/2 | `ViewProxy` null-safe view wrapper | **DELETE** | Exists only to paper over Android view lifecycles; obsolete under Compose. |
| `adapter/search` | 21 | 1,673 | 14/21 | RecyclerView search adapters | **DELETE** | Adapter pattern dies with RecyclerView; the *filter/model* types inside PORT (see §6). |
| `adapter/sessionlog` | 15 | 1,433 | 7/15 | RecyclerView session-log adapters | **DELETE** | Same; extract row view-models before deleting. |
| `components` | 13 | 1,543 | 10/13 | `Preference` widgets + a few serializers | **DELETE** | Preference-framework UI; keep only the 3 date/pitch serializers (fold into §2 JSON). |
| `api` | 3 | 236 | 0/3 | `ApiState`, `RateLimiter` | **PORT** | Pure logic, zero Android. Trivial. |
| `api/model` | 22 | 2,592 | 0/22 | Jackson WaniKani DTOs | **REWRITE** | Pure but re-annotate to kotlinx.serialization (16× `@JsonProperty`, 2× custom ser/deser). |
| `enums` | 28 | 4,076 | 7/28 | Domain + a few UI enums | **PORT** | 21/28 are pure domain (`SubjectType`, `SessionType`, …) → PORT. 7 touch Android (`ActiveTheme`, `NotificationCategory`, animation enums) → KEEP/rewrite behavior. |
| `model` | 29 | 5,514 | 4/29 | **Core business logic** (`Session`, `SrsSystem`, `TimeLine`, `Question`) | **PORT** + **DECOMPOSE** | The domain heart; mostly Android-free, highest-value, test-scaffold first (§4). Most types PORT, but `Session` (1,616 LOC singleton with UI reach-through) is a **DECOMPOSE** target (§7). |
| `db` | 4 | 1,231 | 3/4 | Room `AppDatabase`, `Converters` | **PORT** | Room KMP: `AppDatabase`→Kotlin, drop all 40 `Migration`s (fresh install). `Converters` → Kotlin `@TypeConverter`. |
| `db/dao` | 14 | 3,223 | 13/14 | Room DAOs (`SubjectSyncDao` 948 LOC) | **PORT** + **DECOMPOSE** | Small DAOs PORT to Room KMP `@Dao`. `SubjectSyncDao` is not a DAO — DECOMPOSE (§8): only 12 of its methods are `@Query`; the rest are sync/mapping/JSON. |
| `db/model` | 17 | 4,170 | 11/17 | 9 Room `@Entity` + row types (`Subject` 1,919) | **PORT** + **DECOMPOSE** | Small entities PORT. `Subject` (138 public methods) is a god-object — DECOMPOSE (§8) into a plain entity + domain types. |
| `jobs` | 21 | 1,350 | 0/21 | Serial background jobs (19 `Job` subclasses) | **REWRITE** | No Android imports but coupled to `JobIntentService` + globals; redesign as coroutine work units. |
| `tasks` | 20 | 2,344 | 1/20 | 18 `ApiTask` network subclasses | **REWRITE** | Network + retry/persist logic; rebuild on Ktor + coroutines. |
| `services` | 8 | 1,428 | 7/8 | Services, workers, alarm receiver, widget | **KEEP** | Platform scheduling glue (`androidMain` + iOS equivalents via `expect/actual`). See §5. |
| `livedata` | 26 | 2,174 | 10/26 | Observable app-state holders | **REWRITE** | `LiveData` → `StateFlow`. Mechanical but 26 files; the data they hold is domain (PORT-adjacent). |
| `util` | 25 | 7,392 | 12/25 | Mixed bag (async, matching, audio, text) | **SPLIT** | `FuzzyMatching`/`TextUtil`/`KanaUtil` PORT; `ObjectSupport` **DISSOLVE** (§7); `AsyncTask`/`AudioUtil`/`WebClient`/`FontStorageUtil` REWRITE; `ViewUtil` **DELETE** (§7). |
| `adapter` (root) | 1 | 26 | 0/1 | marker/interface | **PORT** | Trivial. |

**Rollup:** ~19,700 LOC (activities+fragments+views+proxy+adapters+components) is **DELETE** — nearly a third of the codebase is Android UI that does not migrate. The durable core is `model` + `enums` + `api` + the domain half of `util` (~14k LOC PORT) plus the DB/network/async subsystems that must be **REWRITE**.

---

## 2. Library replacement table

| Concern | Current | Files | KMP replacement | Difficulty | Note |
|---|---|---|---|---|---|
| HTTP client | OkHttp 5.0.0-alpha.6 + okhttp-urlconnection | `ApiTask`, `WebClient`, `PitchInfoUtil`, `DbLogger` | Ktor Client | **Medium** | REST side is easy; `WebClient` does HTML-form login + cookie jar scraping of wanikani.com — the hard 20%. |
| JSON | Jackson databind 2.15 | 39 files (22 DTOs annotated) | kotlinx.serialization | **Medium-High** | Re-annotate DTOs; port 2 custom (`PitchInfo`, `WaniKaniApiDate`) ser/deser pairs. |
| JSON (stray) | Gson 2.10.1 | `BackupActivity` only | kotlinx.serialization | **Easy** | Single use; collapse into the above. |
| Database | Room 2.6.1 (Java, 9 entities, 13 DAOs, v68) | `db/*` | **Room KMP (2.8.x + KSP)** | **Medium** | No library swap — same annotations, Java→Kotlin + `annotationProcessor`→KSP. No migrations to reproduce (fresh install). Was the "Hard" row; now de-risked. |
| Async | custom `AsyncTask`, `Handler`/`Looper`, `JobIntentService` | see §3 | Kotlin coroutines + Flow | **Medium** | Idiom change, broadly mechanical once the job/task queues are redesigned. |
| Reactive state | `LiveData` (androidx) | 42 files reference | `StateFlow` / `SharedFlow` | **Medium** | 26 `livedata/*` holders + observers across UI. |
| Images | Glide 4.12 + glide-transformations | `SubjectCardBinder` (1) | Coil 3 (KMP) | **Easy** | Single binding site. |
| Settings | SharedPreferences + androidx.preference | `GlobalSettings` + `components` | multiplatform-settings or DataStore | **Medium** | The values PORT; `androidx.preference` UI is DELETE. |
| Encrypted settings | androidx.security-crypto (`EncryptedPreferenceDataStore`) | `components` | **Drop — plain app-sandboxed storage** | **Easy** | See §7. androidx.security-crypto is itself deprecated; the WaniKani token doesn't warrant per-platform secure storage. Removes an `expect/actual` surface. |
| HTML parsing | jsoup 1.16.1 | `WebClient`, tag handler | Ktoml/Ksoup (Kotlin fork) | **Medium** | Tied to the scraping login flow. |
| Animation | Lottie 5.0.3 | UI only | Compose Lottie (`compottie`) | **Easy** | UI-layer, deferred with UI rewrite. |
| Color picker | Pikolo 2.0.2 | UI only | Compose equivalent | **Easy** | DELETE with UI. |
| Desugaring | desugar_jdk_libs (java.time) | build | kotlinx-datetime | **Medium** | Replace `java.time`/`java.util` date use in `model`/`Session`/`TimeLine` with `kotlinx-datetime`. |

---

## 3. Async & threading inventory

| Mechanism | Where | Disposition |
|---|---|---|
| Custom `util/AsyncTask` (ThreadPoolExecutor, 1→20 threads, backup pool) | 5 files; wrapped by `ObjectSupport.runAsync` / `runAsyncWithProgress` | **REWRITE** → `viewModelScope`/`Dispatchers`. This is the app's primary background-work entry point. |
| `ObjectSupport.runAsync[WithProgress]` (LifecycleOwner-aware) | called broadly (`ObjectSupport` = 129 inbound refs) | **REWRITE** — extract from `ObjectSupport` (which DISSOLVEs, §7) into a coroutine facade; the rest of `ObjectSupport` becomes stdlib or is deleted. |
| `Handler` / `Looper` (main-thread posting, `postDelayed`) | 5 files | **REWRITE** → `Dispatchers.Main` / `delay`. |
| `JobIntentService` (`JobRunnerService`) — serial in-memory job queue | `services/JobRunnerService` + 19 `jobs/*` | **REWRITE** → single-threaded coroutine dispatcher / `Channel`. Jobs are not persisted (survive-restart = no). |
| `ApiTaskService` — background network task queue, DB-persisted | `services/ApiTaskService` + 18 `tasks/*` (via `TaskDefinition` entity) | **REWRITE** → coroutine worker draining a persisted queue. |
| `WorkManager` (periodic) | `BackgroundSyncWorker`, `NotificationWorker`, `LiveWorkInfos` | **KEEP** (androidMain); `expect/actual` scheduler for other platforms. |
| `AlarmManager` + `SCHEDULE_EXACT_ALARM` | `BackgroundAlarmReceiver` | **KEEP** (androidMain). |
| Locks / `synchronized` / `AtomicInteger` | 17 files reference `Lock`/`synchronized`, 1 `Atomic*` | **PORT** case-by-case; audit each for coroutine `Mutex` vs. remove. |
| `RateLimiter` | `api/RateLimiter` | **PORT** — pure, no Android. |

Concurrency shape: **serial job queue** + **persisted serial API-task queue** + a **general executor** behind `runAsync`. Three separate schedulers to unify onto coroutines.

---

## 4. Test coverage map

| Area | Protected? | By what |
|---|---|---|
| Room migrations (48→68) | ⛔ N/A | `DatabaseMigrationTest` (20 methods) — **DELETE**: fresh install has no prior DB to migrate. |
| Notification worker | ✅ Smoke only | `NotificationWorkerTest` — 1 test |
| **Everything else** | ❌ None | — |

Net: after dropping the migration tests, the app has **one smoke test**. The whole domain is unprotected.

Explicitly **unprotected** and high-consequence:

- `model/Session` (1,616 LOC) — session/answer flow. Zero tests.
- `util/FuzzyMatching` (752 LOC) + `OptimalStringAlignmentDistance` — **answer-correctness matching**. Zero tests. A regression here silently marks right answers wrong.
- `model/SrsSystem` (789) / `SrsBreakDown` / `TimeLine` — SRS scheduling math. Zero tests.
- `api/model/*` (22 DTOs) — JSON contract with WaniKani. Zero tests; migrating to kotlinx.serialization has no safety net. **This is now the single most exposed swap** (DB is a like-for-like Room port; DTOs change libraries).
- All 19 jobs + 18 API tasks — zero tests.
- `GlobalSettings` (3,541) — zero tests.

**There is no `src/test` directory.** Everything is instrumented (`androidTest`), so today nothing runs on the JVM. **Action before any port: write JVM characterization tests for `model`, `FuzzyMatching`, and DTO round-trips** — these are the invariants the migration must preserve. (Room-schema fidelity matters less now: no migration correctness to preserve, just the current v68 shape.)

---

## 5. Runtime behavior inventory

| Behavior | Component | Trigger | Migration note |
|---|---|---|---|
| Serial background jobs | `JobRunnerService` (`JobIntentService`) | `schedule(jobClass, data)` → intent enqueue | In-memory, non-persistent. → coroutine queue. |
| Persisted API sync tasks | `ApiTaskService` + `TaskDefinition` entity | DB-backed queue | Survives restart via DB. → coroutine worker. |
| Periodic background sync | `BackgroundSyncWorker` (WorkManager) | periodic | `androidMain`; `expect/actual` elsewhere. |
| Notifications | `NotificationWorker` + `NotificationCompat` | WorkManager + alarm | `POST_NOTIFICATIONS`; per-platform. |
| Exact alarms | `BackgroundAlarmReceiver` | `AlarmManager` + `SCHEDULE_EXACT_ALARM` | Exact-alarm permission; Android-specific scheduling. |
| Home-screen widget | `SessionWidgetProvider` (`AppWidgetProvider` + `RemoteViews`) | system widget updates | Android-only surface; no KMP analog — **KEEP as Android app feature**. |
| Network-state reaction | `NetworkStateChangedJob` + `ACCESS_NETWORK_STATE` | connectivity change | `expect/actual` connectivity. |
| Audio download/playback | `AudioUtil` (1,208 LOC), `DownloadAudioTask` | user/session | Media = per-platform; large REWRITE. |

Permissions in play: `INTERNET`, `ACCESS_NETWORK_STATE`, `POST_NOTIFICATIONS`, `VIBRATE`, `WAKE_LOCK`, `SET_ALARM`, `SCHEDULE_EXACT_ALARM`.

---

## 6. Top 10 riskiest files (most depended-upon × least tested)

Ranked by inbound reference count, weighted by size and test-gap. All have **zero
direct tests** unless noted.

| # | File | Inbound refs | LOC | Why risky |
|---|---|---|---|---|
| 1 | `util/ObjectSupport` | 129 | 548 | Most-referenced file, but **DISSOLVEs** (§7): pure helpers → stdlib, `safe()` → deleted (crash-fast), `runAsync` → coroutine facade. High ref count, but most dependents resolve to stdlib rather than a port. |
| 2 | `WkApplication` | 91 | 369 | Global singleton / `Context` holder; a static god-object every layer reaches into. Blocks shared code until dependency-injected away. |
| 3 | `db/model/Subject` | 77 | 1,919 | 138-method god-entity fusing 6 concerns (§7 **DECOMPOSE**). Zero tests. Risk is in the split, not a mechanical port. |
| 4 | `GlobalSettings` | 76 | 3,541 | Largest file, 192 static accessors read everywhere (§7 **DECOMPOSE** along its 16 groups). No tests; drift risk high. |
| 5 | `db/AppDatabase` | 65 | 785 | v68 schema. **Down-graded**: Room KMP keeps it as-is (Java→Kotlin) and the 40 migrations are deleted, so most of its former risk evaporates. |
| 6 | `model/Session` | 24 | 1,616 | Core review/lesson engine, singleton with UI reach-through (§7 **DECOMPOSE**). Zero tests; characterize the state machine before splitting. |
| 7 | `util/FuzzyMatching` | (low refs) | 752 | Answer-matching correctness. Few callers but each answer graded flows through it; silent wrong-grading on regression. Zero tests. |
| 8 | `tasks/ApiTask` | 22 | 520 | Base class for all 18 network tasks; owns retry/error semantics against WaniKani. OkHttp+Jackson→Ktor+kotlinx double swap. |
| 9 | `model/SrsSystem` | 15 | 789 | SRS interval/stage math driving all scheduling. Zero tests; off-by-one here corrupts every user's queue. |
| 10 | `proxy/ViewProxy` | 54 | 901 | Heavily depended by UI, but DELETE-bound. Risk is *inverse*: 54 call sites must be untangled from it as UI is rewritten, or it drags Android view coupling into the port. |

Honorable mentions: `db/dao/SubjectSyncDao` (948 LOC — the largest DAO and a DECOMPOSE target, see §7), `enums/SubjectType` (21 refs, pure — cheap early win), `db/Converters` (24 refs, type-conversion contract carried over verbatim under Room KMP).

---

## 7. God-class decomposition targets

These four classes account for ~9,000 LOC and must **not** be ported 1:1. Each fuses several responsibilities; the migration splits them, and the pure pieces become the tested `commonMain` core. Responsibilities below are read off the actual method surface.

### `db/model/Subject` — 1,919 LOC, **138 public methods**
Fuses six concerns onto one Room row:

| Concern | Evidence (methods) | Target |
|---|---|---|
| Persistence row | column getters/setters | thin Room-KMP `@Entity` (data class) |
| API→entity mapping | built from `ApiSubject`/assignment DTOs | mapper in the sync service |
| Vocabulary domain | `getAcceptedReadings`, `getOnYomiReadings`, `hasNanori`, `isPrimaryReading`… | pure `Reading`/`Meaning` domain (commonMain, **tested**) |
| SRS / assignment state | `isBurnable`, `isOverdue`, `isResurrectable`, `isEligibleForSessionType`, `getAvailableAt` | pure `AssignmentState` value object (**tested**) |
| Review statistics | `getMeaningCurrentStreak`, `getPercentageCorrect`… | `ReviewStatistics` value object |
| Presentation / media | `getCharactersHtml`, `getParsedStrokeData`, `getPitchInfoFor`, `PronunciationAudioOwner` | move HTML/stroke/audio out of the entity entirely |

Note it imports `android.os` and Jackson — proof the "entity" reaches into framework and JSON. A clean Room-KMP entity can't carry that.

### `db/dao/SubjectSyncDao` — 948 LOC, only **12 `@Query`**, ~28 hand-written methods
Not a DAO. It is a sync service wearing a DAO's clothes:

| Concern | Evidence | Target |
|---|---|---|
| Real data access | the 12 `@Query` methods | keep as a lean Room-KMP `@Dao` |
| DTO mapping | `tryUpdateAssignment`, `insertOrUpdateStudyMaterial`, `insertOrUpdateReviewStatistic` (imports `api.model`) | `SubjectSyncService` (commonMain) |
| JSON | `serializeToJsonString` (imports Jackson) | serialization layer (kotlinx) |
| Sync orchestration | `insertOrUpdate`, `patchAssignment`, `patchReviewStatistic` | `SubjectSyncService` |
| Availability business rules | `forceLessonAvailable`, `forceReviewAvailable`, `getPendingReviewItemsHelper` | pure scheduling logic (**tested**), not SQL glue |

Also imports `android.database.sqlite` and `livedata` — a DAO should know about neither.

### `GlobalSettings` — 3,541 LOC, **192 static accessors**, 16 nested groups
Already grouped internally (`Api`, `Display`, `Dashboard`, `Audio`, `Review`, `Keyboard`, `Font`, `Advanced*`, `Tutorials`, …). Decompose along those existing seams into ~16 per-feature settings objects over one multiplatform-settings store, injected — not a global static. This kills the app's second-biggest coupling hub (76 inbound refs) rather than porting a 3.5k-LOC singleton.

### `model/Session` — 1,616 LOC, 53 methods, singleton
The review/lesson engine, but it also reaches into UI:

| Concern | Evidence | Target |
|---|---|---|
| Session state machine | `advance`, `submit`, `skip`, `undoAndRetry`, `startQuiz`, `wrapup` | pure state machine (commonMain, **the** thing to characterize-test first) |
| Question generation | `chooseQuestion`, `getQuestionChoiceReason` | pure `QuestionSelector` |
| Persistence | item load/finish/reset | repository |
| **UI leakage** | `getAdapter`, `getNewFragment`, `getCurrentTypefaceConfiguration` | remove — these belong to the (DELETE-bound) view layer |

Drop the singleton; the engine becomes an injected, testable component and the UI accessors go away with the old UI.

### `util/ObjectSupport` — 548 LOC, 129 inbound refs — **DISSOLVE, don't port**
The single most-referenced file, but the ref count is misleading: it is three unrelated things stapled together, and each leaves by a different door. It should not survive as a class.

| Concern | Methods | Fate |
|---|---|---|
| Pure helpers | `isEmpty`, `isEqual`, `hash`, `join`, `shuffle`, `nextRandomInt`, `orElse`, `listOf`, wait-time formatting | Mostly **become Kotlin stdlib** (`isNullOrEmpty`, `==`, `hashCode`, `joinToString`, `shuffled`, `Random`, `?:`, `listOf`). Only the domain-specific formatters PORT. |
| `safe()` / `safeNullable()` exception swallowers (21 defs) | wrap-and-ignore | **Delete** under the crash-fast policy below. |
| `runAsync` / `runAsyncWithProgress` async facade | LifecycleOwner-bound background exec | **REWRITE** to coroutines (§3). |

So its 129 dependents don't all need a replacement — most resolve to stdlib or vanish. `ObjectSupport` as a named utility ceases to exist.

### Crash-fast error policy (project decision)
**Adopt crash-fast: prefer an unhandled exception that gets reported over a swallowed one.** The `safe()` wrapper exists because Android view/lifecycle callbacks turn any thrown exception into an app crash, so the original code defensively swallowed everything. That defensiveness is not migrated:

- **82% of `safe()` call sites (384 of 468) live in DELETE-bound UI** (`views` 161, `activities` 93, `fragments` 87, `components` 25, adapters 18). They **disappear with the UI** when it's rebuilt in Compose — they are not stripped by hand, they simply have no code left to wrap. This is the mechanism: Compose removes the fragile callback surface that motivated `safe()` in the first place.
- The residual **~84 sites in ported/kept code** (`util` 26, root 20, `services` 14, `livedata` 9, `db` 9, `tasks` 5, `jobs` 1) **drop the wrapper and let exceptions propagate** to a crash reporter. Wrap only at genuine boundaries (a background worker that must not take down the process), and there, log + report — never silently continue.

`util/ViewUtil` (94 LOC — `View`/`TextView`/`Paint`/locale helpers, called only from `views`/`proxy`/`util`) follows the same logic: it is **DELETE**, not PORT. It evaporates with the Android view layer; Compose has its own text/locale handling.

**Principle:** every DECOMPOSE splits into (1) a thin persistence type, (2) pure domain/value objects that go to `commonMain` *with tests*, (3) an application service for mapping/sync, and (4) deletion of any UI reach-through — and (5) defensiveness (`safe()`, view helpers) is *dropped*, not carried, because Compose removes the fragility it guarded against. The pure pieces (2) are the migration's real payload.

---

## Recommended sequencing (derived from the above)

1. **Characterize before touching:** JVM tests for `model` (`Session`, `SrsSystem`, `TimeLine`), `FuzzyMatching`, and DTO JSON round-trips. Nothing else is safe to move without this.
2. **Break the god-objects:** dependency-inject `WkApplication`/`GlobalSettings`/`ObjectSupport` so shared code stops reaching for a static `Context`.
3. **PORT the pure core, DECOMPOSE the god-classes (§7):** small single-purpose types (`enums`, `api`, DTOs, domain `util`) PORT straight to `commonMain`. The four god-classes (`Subject`, `SubjectSyncDao`, `GlobalSettings`, `Session`) get split — extract their pure domain/value objects into tested `commonMain` code, leave thin persistence types behind, and drop UI reach-through. Do **not** port them whole.
4. **REWRITE subsystems behind stable facades:** async (coroutines), JSON (kotlinx — the riskiest swap now), network (Ktor incl. the scraping login). DB is a **PORT** to Room KMP, not a rewrite — schedule it as a low-risk item, no migration reproduction.
5. **DELETE + rebuild UI last:** activities/fragments/views/proxy/adapters → Compose Multiplatform. Keep Android-only surfaces (widget, alarms, WorkManager) on the Android target via `expect/actual`.

---

## 8. Open question resolved: API token storage

**Do we need to store the WaniKani API token securely? — Recommendation: no.**

- The token is a **personal, user-revocable** WaniKani API key. It grants read + limited write (submitting reviews/study materials) to *that user's own* account — no payment data, no third-party access, no account-takeover of anything but their WaniKani study data, and the user can regenerate it on wanikani.com at any time.
- On both Android and iOS, app-private storage is already OS-sandboxed. Encryption (Keystore/Keychain) only adds protection against physical extraction from a rooted/jailbroken device or a device backup — a low-value target for a revocable study-app token.
- The current mechanism, **androidx.security-crypto, is itself deprecated** (no longer recommended by Google), so keeping it is not even the safe status-quo choice.
- Dropping it removes an `expect/actual` secure-storage surface and simplifies the KMP settings layer.

**Decision:** store the token in plain app-sandboxed settings (DataStore / multiplatform-settings), same store as other preferences. If a future threat model changes (e.g. shared-device kiosk use), revisit with a per-platform Keychain/Keystore `expect/actual` — but do not build it speculatively.
