package com.smouldering_durtles.wk.ui.onboarding

/**
 * Every string the onboarding flow shows, as plain Kotlin.
 *
 * The migration deliberately keeps strings out of `res/strings.xml` and away from
 * `stringResource()` — the app is English-only and typed constants survive refactoring in a way
 * resource ids do not.
 */
object OnboardingStrings {
    const val back = "Back"
}
