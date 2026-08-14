package com.smouldering_durtles.wk.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.smouldering_durtles.wk.R

/**
 * Fonts: Plus Jakarta Sans (UI) and JetBrains Mono (API tokens and permission scopes).
 *
 * These are bundled as static faces in `res/font/` rather than pulled from Google Fonts at
 * runtime. Downloadable fonts need Play Services, and onboarding is the one flow that runs
 * before the app has ever reached the network — a device without Play Services would silently
 * fall back to the system face and lose the brand identity on the very first screen.
 *
 * Only the weights actually used are shipped. Plus Jakarta Sans needs Normal/Bold/ExtraBold for
 * [BdTypography]; JetBrains Mono needs Medium/SemiBold for the permission chips and the token
 * field. Both families are OFL-1.1 — see `assets/licenses/`.
 */
val Jakarta = FontFamily(
    Font(R.font.plus_jakarta_sans_regular, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans_bold, FontWeight.Bold),
    Font(R.font.plus_jakarta_sans_extrabold, FontWeight.ExtraBold),
)

val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
    Font(R.font.jetbrains_mono_semibold, FontWeight.SemiBold),
)

/** Material 3 type scale tuned to the design's sizes. */
val BdTypography = Typography(
    displaySmall = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.ExtraBold, fontSize = 36.sp, lineHeight = 38.sp, letterSpacing = (-0.6).sp),
    headlineSmall = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, lineHeight = 28.sp, letterSpacing = (-0.3).sp),
    titleLarge = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Bold, fontSize = 21.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Bold, fontSize = 15.5f.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Normal, fontSize = 13.5f.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Bold, fontSize = 12.5f.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Bold, fontSize = 11.5f.sp, lineHeight = 14.sp, letterSpacing = 0.3.sp),
)

/** Overline used for the "WELCOME TO" eyebrow. */
val overlineStyle: TextStyle
    @Composable @ReadOnlyComposable
    get() = MaterialTheme.typography.labelMedium.copy(
        letterSpacing = 2.sp, fontWeight = FontWeight.Bold
    )
