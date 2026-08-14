package com.smouldering_durtles.wk.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Palette extracted from the "Blazing Durtles — onboarding v2" design.
 * The warm coral/ember family on a subtly-warm off-white surface.
 */

// Brand / primary
val Ember          = Color(0xFFC2410C) // primary
val EmberDeep      = Color(0xFFB5350A) // hero gradient end
val EmberBright    = Color(0xFFD8501A) // hero gradient start
val OnEmber        = Color(0xFFFFFFFF)
val EmberInk       = Color(0xFF7A2A06) // text on warm containers

// Surfaces
val Surface        = Color(0xFFFFF8F5) // app background
val SurfaceCard    = Color(0xFFFFF1EA) // cards / list containers
val SurfaceChip    = Color(0xFFFCEBE2) // permission chips
val SurfaceChipAlt = Color(0xFFFCE3D6) // tonal (secondary) button
val Container      = Color(0xFFFFDBC9) // step badges, highlight tiles

// Text
val TextPrimary    = Color(0xFF211A16)
val TextSecondary  = Color(0xFF52443C)
val TextTertiary   = Color(0xFF85736B)
val TextMuted      = Color(0xFFA08C82)
val TextDisabled   = Color(0xFFB49C90)

// Lines / tracks
val OutlineWarm    = Color(0xFFE2C9BD)
val DividerWarm    = Color(0xFFF2E2D8)
val TrackWarm      = Color(0xFFF0DACE)
val PlaceholderBtn = Color(0xFFF2DFD4) // disabled primary button fill

// Status
val Success        = Color(0xFF2E7D32)

// SRS / data-viz accents (WaniKani stage colors, kept for later screens)
val Apprentice     = Color(0xFFDD0093)
val Guru           = Color(0xFF882D9E)
val Master         = Color(0xFF294DDB)
val Enlightened    = Color(0xFF0098E1)
val Burned         = Color(0xFF4A4A4A)

// ---------------------------------------------------------------------------
// DARK THEME  (proposed — the source design ships light-only; these are the
// warm-dark tones from onboarding row 3.)
// ---------------------------------------------------------------------------

// Brand / primary
val EmberDark          = Color(0xFFE85E22) // primary (filled buttons)
val EmberBrightDark    = Color(0xFFFB7A3C) // accents, icons, links, focus
val EmberBrightDeepDk  = Color(0xFFB5350A) // hero gradient end
val OnEmberDark        = Color(0xFFFFFFFF)
val EmberInkDark       = Color(0xFFFFB48A) // text/icons on warm-dark containers

// Surfaces
val SurfaceDark        = Color(0xFF17120F) // app background
val SurfaceCardDark    = Color(0xFF221A15) // cards / list containers
val SurfaceChipDark    = Color(0xFF2B211B) // permission chips
val SurfaceNavDark     = Color(0xFF201814) // bottom navigation
val ContainerDark      = Color(0xFF4D2A17) // step badges, highlight tiles
val TonalBtnDark       = Color(0xFF33261E) // secondary button fill

// Text
val TextPrimaryDark    = Color(0xFFF4E9E2)
val TextSecondaryDark  = Color(0xFFC6B5AA)
val TextTertiaryDark   = Color(0xFF9A887E)
val TextMutedDark      = Color(0xFF7C6B62)
val TextDisabledDark   = Color(0xFF7C6B62)

// Lines / tracks
val DividerDark        = Color(0xFF342821)
val TrackDark          = Color(0xFF33271F)
val PlaceholderBtnDark = Color(0xFF2B211B) // disabled primary button fill
val UncheckedDark      = Color(0xFF4D3E35) // idle radio/step glyph

// Status
val SuccessDark        = Color(0xFF6FCF74)

// SRS / data-viz accents lifted for dark surfaces
val ApprenticeDark     = Color(0xFFFF2CAE)
val GuruDark           = Color(0xFFA24DBB)
val MasterDark         = Color(0xFF4E74F0)
val EnlightenedDark    = Color(0xFF20B4F2)
val BurnedDark         = Color(0xFF8A8A8A)

