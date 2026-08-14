package com.smouldering_durtles.wk.ui.onboarding

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.smouldering_durtles.wk.ui.theme.BlazingDurtlesTheme

/**
 * Hosts the Compose onboarding flow, and in time every other screen the migration moves to
 * Compose.
 *
 * It deliberately does not extend `AbstractActivity`. That base class hands subclasses an options
 * menu, a background-task subtitle, session LiveData observers, a per-minute tick timer,
 * auto-sync-on-open, a shared-preferences listener, fragment plumbing and the AppCompat theme
 * machinery — every one of which is either meaningless before the user has a token, or replaced
 * by something Compose does better. Its window-inset handling, ~25 lines of manual padding, is
 * one `enableEdgeToEdge()` call here.
 */
class OnboardingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val darkTheme = isSystemInDarkTheme()
            BlazingDurtlesTheme(darkTheme = darkTheme) {
                // Placeholder state until the ViewModel lands; keeps the graph drivable so the
                // screens can be walked on a device.
                var settingsTapped by remember { mutableStateOf(false) }
                var token by remember { mutableStateOf("") }

                OnboardingNavHost(
                    start = OnboardingDestination.Welcome,
                    settingsTapped = settingsTapped,
                    token = token,
                    canContinue = token.isNotBlank(),
                    onOpenWaniKaniSettings = {
                        settingsTapped = true
                        openWaniKaniTokenSettings()
                    },
                    onTokenChange = { token = it },
                    onContinue = { },
                    onExit = { finishAffinity() },
                )
            }
        }
    }

    private fun openWaniKaniTokenSettings() {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(OnboardingStrings.wanikaniTokensUrl)))
    }
}
