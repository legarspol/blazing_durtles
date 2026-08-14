# Blazing Durtles — Onboarding v2 ("Less") · Jetpack Compose

Compose (Material 3) translation of the redesigned onboarding line. The mocked OS status
bar and gesture pill from the HTML are **not** reproduced — those are system chrome, not app
views.

## Files

```
ui/theme/Color.kt                 Palette (brand, surfaces, text, dividers, SRS accents)
ui/theme/Type.kt                  Fonts (Plus Jakarta Sans / JetBrains Mono / Noto Sans JP) + type scale
ui/theme/Theme.kt                 BlazingDurtlesTheme (light + dark), color schemes, extended colors, shapes
ui/components/OnboardingComponents.kt   Top bar, buttons, step badge, permission chip
ui/onboarding/WelcomeScreen.kt          01 — Welcome
ui/onboarding/ConnectScreen.kt          02 — Connect
ui/onboarding/EnterTokenScreen.kt       03 — Enter token
ui/onboarding/SyncingScreen.kt          04 — Identified + Syncing
ui/onboarding/OnboardingNavHost.kt      Navigation graph + sample state
```

## What changed from v1 (baked into these composables)

- **Welcome** — no version chip, no "I already have a token", no tagline.
- **Connect** — dropped the "go to Settings → API Tokens" step (the button does it); privacy note moved above the steps.
- **Enter token** — dropped "we'll check it straight away" and the standalone paste button (paste is the field's trailing icon).
- **Syncing** — replaces v1's Verified + All-set: once identified, the field is gone and data pulls in place; auto-advances to the dashboard.

## Dark theme

Fully supported. `BlazingDurtlesTheme(darkTheme = …)` (defaults to `isSystemInDarkTheme()`)
switches both the Material `ColorScheme` and the extended `BdExtendedColors`. Every screen
reads `MaterialTheme.colorScheme.*` and `BdTheme.colors.*`, so no per-screen changes are
needed. Dark tones (warm near-black surfaces, brightened ember + success, lifted SRS chart
colors) are proposed values — the source design ships light-only.

## Gradle deps expected

```kotlin
implementation(platform("androidx.compose:compose-bom:2024.09.00"))
implementation("androidx.compose.material3:material3")
implementation("androidx.compose.material:material-icons-extended")
implementation("androidx.compose.ui:ui-text-google-fonts")
implementation("androidx.navigation:navigation-compose:2.8.0")
```

## Wiring you still own

- `mascot` drawable in `res/drawable/`.
- Google Fonts cert array (`res/values/font_certs.xml`) — referenced in `Type.kt`.
- Token verification + WaniKani deep link (`/settings/personal_access_tokens`) and the sync
  pipeline that drives `SyncUiState`. `OnboardingNavHost` shows where each hooks in;
  `sampleSyncState` is illustrative only.
