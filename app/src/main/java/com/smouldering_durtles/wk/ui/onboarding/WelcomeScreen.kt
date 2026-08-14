package com.smouldering_durtles.wk.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.smouldering_durtles.wk.R
import com.smouldering_durtles.wk.ui.theme.BdTheme
import com.smouldering_durtles.wk.ui.theme.BlazingDurtlesTheme
import com.smouldering_durtles.wk.ui.theme.overlineStyle

/**
 * Brand recognition, shown once per install. A returning user whose token has gone stale starts
 * at [ConnectScreen] instead — they know what the app is, they just need a new token.
 */
@Composable
fun WelcomeScreen(onGetStarted: () -> Unit) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { inner ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            BdTheme.colors.container,
                            MaterialTheme.colorScheme.background,
                        ),
                        radius = 900f,
                    )
                )
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
                            .shadow(24.dp, RoundedCornerShape(38.dp), clip = false)
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
                OnboardingButton(
                    text = OnboardingStrings.getStarted,
                    onClick = onGetStarted,
                    trailingIcon = Icons.AutoMirrored.Rounded.ArrowForward,
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
