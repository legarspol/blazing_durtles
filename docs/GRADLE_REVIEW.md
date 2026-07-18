# Gradle Modernization Review

Companion to the version-catalog conversion. This pass did **two** things and kept them
strictly separate:

1. **Applied now (behavior-preserving):** extracted every version into
   `gradle/libs.versions.toml`, moved `app/build.gradle.kts` and the root `build.gradle.kts` to the
   catalog + modern `plugins {}` DSL, and dropped genuinely dead lines. No dependency was
   added, removed, or version-bumped. `./gradlew :app:dependencies` resolves identically.
2. **Recorded here (NOT applied):** every "should this go or change" verdict. These are
   behavioral and belong to migration tickets, not a catalog refactor — applying them
   silently would break the small-diff rule in `CLAUDE.md`.

**Legend:** `KEEP` fine as-is · `BUMP` same lib, newer version · `CHANGE` reshape/replace in place ·
`GO` remove entirely (tracked by a migration phase) · `WATCH` conditional, decide later.

Phase references point at `docs/MIGRATION_PLAN.md`.

---

## What the conversion changed (mechanical)

- **New:** `gradle/libs.versions.toml` — auto-detected by Gradle, no `settings.gradle.kts` edit needed.
- **`app/build.gradle.kts`:** `apply plugin:` → `plugins { alias(...) }`; `minSdkVersion`/`targetSdkVersion` → `minSdk`/`targetSdk`; `namespace`+`compileSdk` hoisted to the top of `android {}`; all coordinates → `libs.*`. Dropped `implementation fileTree(include: ['*.jar'], dir: 'libs')` — there is no `app/libs/` dir, so it was a no-op.
- **Root `build.gradle.kts`:** plugin versions → catalog aliases; `rootProject.buildDir` (deprecated in Gradle 9) → `rootProject.layout.buildDirectory`.
- **Not done:** Groovy → Kotlin DSL (`.gradle.kts`). Higher-risk, unrelated to the catalog; offered as a follow-up.

---

## `app/build.gradle.kts` — line by line

### Android config

| Item | Current | Verdict | Note |
|---|---|---|---|
| `compileSdk` / `targetSdk` | 35 | `WATCH` | SDK 36 (Android 16) is out; IDE flags 37. Bump deliberately with a device test, not blindly. |
| `minSdk` | 21 | `KEEP` | Matches the plan. Costs you core-library desugaring (below); acceptable. |
| `applicationId` vs `namespace` | `com.blazingdurtles.android` vs `com.smouldering_durtles.wk` | `WATCH` | Intentional rebrand split — new app id, old source package. Harmless, but the eventual package rename is a large mechanical ticket; note it's still pending. |
| `sourceCompatibility` / `targetCompatibility` | `VERSION_1_8` | `CHANGE` | **Java 8 is the biggest smell here.** Move to 17 (AGP 9 default toolchain). With AGP-9 built-in Kotlin, also set the Kotlin `jvmTarget` to match, else Java/Kotlin bytecode targets drift. |
| `coreLibraryDesugaringEnabled` + `desugar_jdk_libs` | on, 2.0.4 | `WATCH` | Needed for `java.time`/NIO on API 21. Plan prefers `kotlinx-datetime`; once `java.time` usage is gone this may be droppable. Keep until then. |
| `annotationProcessorOptions` room.schemaLocation | via `annotationProcessor` | `CHANGE` | **Phase 0**: Room → KSP. Replace `annotationProcessor room-compiler` with `ksp`, and move the schema dir to `ksp { arg("room.schemaLocation", ...) }`. Java `annotationProcessor` + Kotlin KSP on the same processor is the classic slow/duplicate-gen trap. |
| `testInstrumentationRunner` / `testApplicationId` | set | `WATCH` | Only matters for instrumented tests, which the plan **deletes** (moving to JVM unit tests). Goes with them. |
| `sourceSets androidTest.assets += schemas` | set | `WATCH` | Same fate as instrumented tests. |
| `testOptions { reportDir / resultsDir }` | custom dirs | `CHANGE` | `reportDir`/`resultsDir` are deprecated (removed in recent AGP). If a sync ever warns/breaks, switch to `testOptions.reportDir`→ Gradle's `Test` task `reports` or just drop the custom dirs. |
| `buildFeatures { buildConfig, resValues }` | true | `KEEP` | Explicit is correct now that these are opt-in in AGP 8+/9. |
| `dependenciesInfo { includeInApk/Bundle = false }` | false | `KEEP` | Deliberate privacy choice (strips the signed dependency blob). Fine. |

### Build types

| Item | Current | Verdict | Note |
|---|---|---|---|
| `release { minifyEnabled false }` | off | `CHANGE` | Shipping release **unshrunk/unobfuscated**. Turn on R8 (`minifyEnabled true` + `shrinkResources true` + a `proguard-rules.pro`) before a real release — especially as Compose/Kotlin land and method count grows. |
| `release { signingConfig signingConfigs.debug }` | debug key | `WATCH` | Release signed with the **debug keystore**. Tolerable for sideload/CI, wrong for any store channel. Flag before distribution. |
| `debug { applicationIdSuffix '.debug' }` + resValues | set | `KEEP` | Clean side-by-side install setup. |

### Dependencies

| Dependency | Ver | Verdict | Note |
|---|---|---|---|
| `androidx.activity:activity-ktx` | 1.6.0 | `GO` (Phase 4) | Old View/Activity stack → Compose. Also badly outdated if kept. |
| `androidx.fragment:fragment-ktx` | 1.3.6 | `GO` (Phase 4) | Fragments die with the Compose rebuild. Very stale version. |
| `androidx.appcompat` | 1.6.1 | `GO` (Phase 4) | Compose doesn't need AppCompat (theme aside). |
| `androidx.constraintlayout` | 2.1.4 | `GO` (Phase 4) | XML layout system → Compose. |
| `androidx.recyclerview` | 1.3.2 | `GO` (Phase 4) | Replaced by `LazyColumn`/`LazyVerticalGrid`. |
| `androidx.preference` | 1.2.1 | `GO` (Phase 2) | → DataStore per stack. `androidx.preference` is effectively unmaintained. |
| `androidx.legacy:legacy-support-core-utils` | 1.0.0 | `GO` | Ancient monolithic legacy support lib. Likely removable now with a small import cleanup — worth an early standalone ticket. |
| `androidx.annotation` | 1.7.1 | `CHANGE` | Odd as `debugImplementation` **and** `androidTestImplementation`. Annotations are compile-time; should be plain `implementation` (or `compileOnly`). Minor. |
| `androidx.room:room-runtime` / `-compiler` / `-testing` | 2.6.1 | `KEEP` + `CHANGE` | Room **stays** (Java→Kotlin+KSP). Convert `-compiler` to `ksp` (Phase 0/2). Note Lottie already pulls `room-ktx` transitively — add it explicitly when you go coroutines/Flow on the DAOs. |
| `androidx.work:work-runtime` | 2.9.0 | `CHANGE` | Keep WorkManager, but use `work-runtime-ktx` for `CoroutineWorker`/`await()` (matches the coroutines direction). Consider a bump. |
| `androidx.security:security-crypto` | 1.1.0-**alpha06** | `WATCH`/`GO` | Two problems: it's an **alpha**, and Jetpack Security is **deprecated by Google**. Its only job is the stored WaniKani website password → tied to the deferred burn/resurrect feature. If that feature is dropped, this goes with it; if kept, migrate off (e.g. Tink directly / EncryptedFile alternative). |
| `com.fasterxml.jackson.core:jackson-databind` | 2.15.0 | `GO` (Phase 2) | → kotlinx.serialization. Also note the Jackson **BOM drags in `gson:2.8.9`** (overridden to 2.10.1) — resolving both JSON stacks today. |
| `com.google.code.gson:gson` | 2.10.1 | `GO` (Phase 2) | Not in the stack (banned). Verify it's actually referenced in code before removal; may already be dead weight behind Jackson. |
| `com.squareup.okhttp3:okhttp` / `-urlconnection` | 5.0.0-**alpha.6** | `GO` (Phase 2) | → Ktor. Independently, pinning a **networking** lib to an alpha is the riskiest single pin in the file. |
| `org.jsoup:jsoup` | 1.16.1 | `WATCH` (Deferred) | Only used by web-scraping for burn/resurrect. Stays until that feature's keep/drop call. |
| `com.airbnb.android:lottie` | 5.0.3 | `WATCH` | Not mentioned in the plan (Glide is the named image removal; Lottie is vector animation, different concern). Confirm what still uses it; if it's only legacy-UI chrome, it exits in Phase 4. Stale version regardless. |
| `com.github.madrapps:pikolo` | 2.0.2 | `GO` (Phase 4) | JitPack color-picker for a settings screen → dies with the old UI. JitPack dep = supply-chain/repro risk; good to shed. |
| `com.github.bumptech.glide:glide` + `:compiler` + `jp.wasabeef:glide-transformations` | 4.12.0 / 4.3.0 | `GO` (Phase 1) | Explicit plan item: delete Glide + transformations + the welcome GIF, no replacement. Three coordinates + one `annotationProcessor` all leave together. |
| `junit:junit` | 4.13.2 | `KEEP` | Fine for JVM tests. Plan writes new characterization tests against `kotlin.test`/JUnit. |
| `androidx.test:runner/core/rules`, `androidx.test.ext:junit`, `androidx.room:room-testing` | — | `GO` | Instrumented-test deps; the 2 instrumented files are being deleted. New tests are JVM `testImplementation`, not `androidTestImplementation`. |

### Tail

| Item | Verdict | Note |
|---|---|---|
| `tasks.withType(JavaCompile) { -Xlint:unchecked; deprecation }` | `KEEP` (fades) | Useful while Java remains; becomes a no-op as the codebase reaches 100% Kotlin. |

---

## Root `build.gradle.kts`

| Item | Verdict | Note |
|---|---|---|
| `com.android.application` plugin | `KEEP` | Now `alias(libs.plugins.android.application)`. |
| `com.android.library` plugin (`apply false`) | `WATCH` | Currently **unused** (no library module). Per the plan, `:core` is a **pure-Kotlin/JVM** module — it needs `java-library` + the Kotlin **JVM** plugin, *not* `com.android.library` (AGP-9 built-in Kotlin only covers Android modules). Either wire it to `:core` correctly at Phase 0 or drop it until then. |
| manual `clean` task | `KEEP` (modernized) | The root project has no base plugin, so this is what makes `./gradlew clean` wipe the root `build/`. Modernized off deprecated `buildDir`. |

---

## `gradle.properties` (left unchanged — recommendations only)

Behavioral flags; not touched by a catalog refactor. Suggested for a Phase-0 toolchain ticket:

| Setting | Now | Suggestion |
|---|---|---|
| `org.gradle.jvmargs` | `-Xmx1536m` | `CHANGE` → `-Xmx4g -XX:+UseParallelGC` (also add `-Dfile.encoding=UTF-8`). 1.5 GB is tight for AGP 9 + Kotlin + KSP. |
| `org.gradle.parallel` | commented out | `CHANGE` → enable. It matters once `:core` exists (multi-module parallelism). |
| `org.gradle.caching` | absent | `ADD` `=true` — build cache, meaningful once modules split. |
| `org.gradle.unsafe.configuration-cache=true` | old key | `CHANGE` → `org.gradle.configuration-cache=true` (stabilized since Gradle 8.1; the `unsafe.` prefix is legacy). |
| `android.useAndroidX` / `enableJetifier=false` / `nonTransitiveRClass=true` | set | `KEEP` — all correct/modern. |
| `kotlin.code.style=official` | set | `KEEP` — now load-bearing since Kotlin is active. |
| `android.uniquePackageNames`, `android.dependency.useConstraints`, `android.r8.strictFullModeForKeepRules`, `android.generateSyncIssueWhenLibraryConstraintsAreEnabled` | set | `WATCH` — unusual flags, likely inherited cruft/workarounds. Review whether each is still needed; drop the ones that were papering over old issues. |

---

## Highest-value follow-ups (ranked)

1. **Java 8 → 17** (`compileOptions` + Kotlin `jvmTarget`) — biggest modernity gap; unblocks current language/library features.
2. **Room `annotationProcessor` → KSP** — Phase 0; removes the Java-AP/Kotlin split-processing trap.
3. **Fix `com.android.library` vs `:core`** — the plan's `:core` is JVM, not Android; wire the right plugin at Phase 0.
4. **`gradle.properties` toolchain tune** — jvmargs, parallel, caching, the stabilized config-cache key.
5. **Alpha pins** — `okhttp` and `security-crypto` are the two alphas; both are on the deletion/deferred path anyway, so let the migration retire them rather than bumping.
6. **Release hardening** — R8 + real signing config, before any distribution.

The `GO` items (Glide, Jackson, Gson, OkHttp, preference, legacy-support, the old-UI androidx stack, instrumented-test deps) are already sequenced by `MIGRATION_PLAN.md` phases — no action needed here beyond letting the catalog make each removal a one-line delete.
