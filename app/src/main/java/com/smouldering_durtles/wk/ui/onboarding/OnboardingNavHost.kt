package com.smouldering_durtles.wk.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

/** The three onboarding destinations, in the order a first-run user meets them. */
enum class OnboardingDestination(val route: String) {
    Welcome("welcome"),
    Connect("connect"),
    EnterToken("enter_token"),
}

/**
 * Onboarding's navigation graph.
 *
 * [start] is a parameter rather than a constant because a returning user whose token expired
 * skips [OnboardingDestination.Welcome] — and `NavHost` captures its start destination on first
 * composition, so this has to be decided before the graph composes rather than corrected
 * afterwards with a redirect.
 *
 * [onExit] fires when back is pressed at the start destination. The caller must finish the task
 * rather than let the press pop the graph: onboarding is launched over a MainActivity that is
 * still alive, and whose onResume bounces straight back here whenever the token is missing, so
 * popping would trap the user in a flicker loop with no way out of the app.
 */
@Composable
fun OnboardingNavHost(
    start: OnboardingDestination,
    settingsTapped: Boolean,
    token: String,
    canContinue: Boolean,
    onOpenWaniKaniSettings: () -> Unit,
    onTokenChange: (String) -> Unit,
    onContinue: () -> Unit,
    onExit: () -> Unit,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(navController, startDestination = start.route) {
        composable(OnboardingDestination.Welcome.route) {
            BackHandler(onBack = onExit)
            WelcomeScreen(
                onGetStarted = { navController.navigate(OnboardingDestination.Connect.route) },
            )
        }
        composable(OnboardingDestination.Connect.route) {
            // Only the start destination owns the exit; when Welcome preceded it, back should
            // behave normally and return there.
            if (start == OnboardingDestination.Connect) {
                BackHandler(onBack = onExit)
            }
            ConnectScreen(
                settingsTapped = settingsTapped,
                onOpenSettings = onOpenWaniKaniSettings,
                onHaveToken = {
                    navController.navigate(OnboardingDestination.EnterToken.route)
                },
            )
        }
        composable(OnboardingDestination.EnterToken.route) {
            EnterTokenScreen(
                token = token,
                canContinue = canContinue,
                onTokenChange = onTokenChange,
                onBack = { navController.popBackStack() },
                onContinue = onContinue,
            )
        }
    }
}
