package com.blazingdurtles.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.blazingdurtles.R
import com.blazingdurtles.ui.components.PrimaryButton
import com.blazingdurtles.ui.theme.BdTheme
import com.blazingdurtles.ui.theme.overlineStyle

/**
 * Screen 01 — Welcome (v2 "Less").
 * Removed vs. v1: version chip, "I already have a token" branch, and the tagline.
 * Just mascot → eyebrow → wordmark → single CTA.
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
                        colors = listOf(BdTheme.colors.container, MaterialTheme.colorScheme.background),
                        radius = 900f
                    )
                )
                .padding(horizontal = 30.dp, vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier.weight(1f).fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(R.drawable.mascot),
                        contentDescription = "Blazing Durtles mascot",
                        modifier = Modifier
                            .size(152.dp)
                            .shadow(24.dp, RoundedCornerShape(38.dp), clip = false)
                            .clip(RoundedCornerShape(38.dp))
                    )
                    Text(
                        "WELCOME TO",
                        style = overlineStyle,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 26.dp)
                    )
                    Text(
                        "Blazing Durtles",
                        style = MaterialTheme.typography.displaySmall,
                        color = BdTheme.colors.textPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
                PrimaryButton(text = "Get started", onClick = onGetStarted)
            }
        }
    }
}
