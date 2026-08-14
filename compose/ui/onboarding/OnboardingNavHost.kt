package com.blazingdurtles.ui.onboarding

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

/**
 * Onboarding v2 ("Less") navigation graph — 4 destinations.
 *   Welcome → Connect → EnterToken → Syncing → (dashboard)
 *
 * Wire real behaviour in your ViewModel: token verification, the WaniKani deep link
 * (BuildConfig-driven Uri to /settings/personal_access_tokens), and the sync pipeline that
 * drives [SyncUiState]. The sample state below is illustrative only.
 */
object OnboardingRoutes {
    const val WELCOME = "welcome"
    const val CONNECT = "connect"
    const val ENTER_TOKEN = "enter_token"
    const val SYNCING = "syncing"
}

@Composable
fun OnboardingNavHost(
    navController: NavHostController = rememberNavController(),
    onOpenWaniKaniSettings: () -> Unit,
    onVerifyToken: (String) -> Unit,
    syncState: SyncUiState,
    onFinished: () -> Unit, // navigate to dashboard when sync completes
) {
    NavHost(navController, startDestination = OnboardingRoutes.WELCOME) {
        composable(OnboardingRoutes.WELCOME) {
            WelcomeScreen(onGetStarted = { navController.navigate(OnboardingRoutes.CONNECT) })
        }
        composable(OnboardingRoutes.CONNECT) {
            ConnectScreen(
                onBack = { navController.popBackStack() },
                onOpenSettings = onOpenWaniKaniSettings,
                onHaveToken = { navController.navigate(OnboardingRoutes.ENTER_TOKEN) },
            )
        }
        composable(OnboardingRoutes.ENTER_TOKEN) {
            EnterTokenScreen(
                onBack = { navController.popBackStack() },
                onVerify = { token ->
                    onVerifyToken(token)
                    // On success the VM should emit an identified user; then:
                    navController.navigate(OnboardingRoutes.SYNCING)
                },
            )
        }
        composable(OnboardingRoutes.SYNCING) {
            SyncingScreen(state = syncState)
            // Observe syncState in the caller; call onFinished() once every task is DONE.
        }
    }
}

/** Illustrative preview/sample state. */
val sampleSyncState = SyncUiState(
    userName = "Kenji",
    level = 24,
    subtitle = "Pleasant",
    itemsSynced = 3214,
    itemsTotal = 5020,
    tasks = listOf(
        SyncTask("Profile & level", "Done", SyncStatus.DONE),
        SyncTask("Assignments", "2,431", SyncStatus.DONE),
        SyncTask("Subjects & mnemonics", "783 / 2,589", SyncStatus.RUNNING),
        SyncTask("Review forecast", "Waiting", SyncStatus.WAITING),
    ),
)
