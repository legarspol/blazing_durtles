package com.smouldering_durtles.wk.ui.onboarding

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.smouldering_durtles.wk.activities.MainActivity
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

    private val viewModel: OnboardingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            BlazingDurtlesTheme(darkTheme = isSystemInDarkTheme()) {
                // collectAsState rather than collectAsStateWithLifecycle: the source is a
                // ViewModel-held StateFlow with no cold upstream, so there is nothing for
                // lifecycle awareness to release, and it would cost an extra artifact.
                val state by viewModel.state.collectAsState()

                LaunchedEffect(state.finished) {
                    if (state.finished) {
                        goToDashboard()
                    }
                }

                OnboardingNavHost(
                    start = viewModel.startDestination,
                    settingsTapped = state.settingsTapped,
                    token = state.token,
                    canContinue = state.canContinue,
                    onWelcomeShown = viewModel::onWelcomeShown,
                    onOpenWaniKaniSettings = {
                        viewModel.onOpenSettingsTapped()
                        openWaniKaniTokenSettings()
                    },
                    onTokenChange = viewModel::onTokenChange,
                    onContinue = viewModel::onContinue,
                    onExit = { finishAffinity() },
                )
            }
        }
    }

    private fun openWaniKaniTokenSettings() {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(OnboardingStrings.wanikaniTokensUrl)))
    }

    /**
     * Starts the dashboard as a fresh task. The MainActivity that bounced the user here is still
     * on the back stack and would bounce again the moment it resumed if it were reused, so the
     * task is cleared rather than returned to.
     */
    private fun goToDashboard() {
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
        )
        finish()
    }
}
