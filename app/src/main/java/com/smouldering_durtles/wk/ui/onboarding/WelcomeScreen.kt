package com.smouldering_durtles.wk.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.smouldering_durtles.wk.R
import com.smouldering_durtles.wk.ui.components.BdButton
import com.smouldering_durtles.wk.ui.theme.BdTheme
import com.smouldering_durtles.wk.ui.theme.BlazingDurtlesTheme
import com.smouldering_durtles.wk.ui.theme.overlineStyle

/**
 * Brand recognition, shown once per install. A returning user whose token has gone stale starts
 * at [ConnectScreen] instead — they know what the app is, they just need a new token.
 */
@Composable
fun WelcomeScreen(onGetStarted: () -> Unit) {
    val glow = BdTheme.colors.container
    val base = MaterialTheme.colorScheme.background
    val shadowTint = MaterialTheme.colorScheme.primary

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { inner ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                // Anchored to the top edge, not the middle: the design hangs the warm glow off the
                // top of the screen and lets it fade out above the mascot, which then reads
                // against the plain background. Drawn here rather than passed to `background()`
                // because the centre and radius both depend on the measured size.
                .drawBehind {
                    drawRect(
                        Brush.radialGradient(
                            colors = listOf(glow, base),
                            center = Offset(size.width / 2f, 0f),
                            radius = size.width * 0.9f,
                        )
                    )
                }
                .padding(horizontal = 30.dp, vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier.weight(1f).fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(
                        painter = painterResource(R.drawable.mascot),
                        contentDescription = OnboardingStrings.mascotDescription,
                        modifier = Modifier
                            .size(152.dp)
                            // Tinted with primary for the same reason the button's is: sampled off
                            // the design board, the shadow under the mascot is the ember colour at
                            // roughly a third alpha, not neutral grey.
                            .shadow(
                                elevation = 24.dp,
                                shape = RoundedCornerShape(38.dp),
                                clip = false,
                                ambientColor = shadowTint,
                                spotColor = shadowTint,
                            )
                            .clip(RoundedCornerShape(38.dp)),
                    )
                    Text(
                        OnboardingStrings.welcomeEyebrow,
                        style = overlineStyle,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 26.dp),
                    )
                    Text(
                        OnboardingStrings.appName,
                        style = MaterialTheme.typography.displaySmall,
                        color = BdTheme.colors.textPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
                BdButton(
                    text = OnboardingStrings.getStarted,
                    onClick = onGetStarted,
                    trailingIcon = Icons.AutoMirrored.Rounded.ArrowForward,
                    lifted = true,
                )
            }
        }
    }
}

@Preview(name = "Welcome — light", showBackground = true)
@Composable
private fun WelcomePreviewLight() =
    BlazingDurtlesTheme(darkTheme = false) { WelcomeScreen(onGetStarted = {}) }

@Preview(name = "Welcome — dark", showBackground = true)
@Composable
private fun WelcomePreviewDark() =
    BlazingDurtlesTheme(darkTheme = true) { WelcomeScreen(onGetStarted = {}) }
