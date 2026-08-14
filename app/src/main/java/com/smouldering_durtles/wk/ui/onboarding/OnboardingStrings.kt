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

    // Welcome
    const val welcomeEyebrow = "WELCOME TO"
    const val appName = "Blazing Durtles"
    const val mascotDescription = "Blazing Durtles mascot"
    const val getStarted = "Get started"

    // Connect
    const val connectTitle = "Connect your account"
    const val connectIntro =
        "Blazing Durtles syncs with your free WaniKani account using an API token. " +
            "The button below opens the right page for you."
    const val privacyNote = "We store only your API token, used solely to talk to WaniKani. "
    const val privacyLinkLabel = "Privacy policy"
    const val stepGenerateTitle = "Generate a new token"
    const val stepGenerateBody =
        "On the page that opens, tap Generate a new token and switch on these four permissions:"
    const val stepCopyTitle = "Copy it & come back"
    const val stepCopyBody = "Paste the token on the next screen."
    const val openWaniKaniSettings = "Open WaniKani settings"
    const val haveMyToken = "I've got my token"

    /** The four scopes the app needs; WaniKani grants none of them by default. */
    val requiredScopes = listOf(
        "assignments:start",
        "reviews:create",
        "study_materials:create",
        "study_materials:update",
    )

    // Enter token
    const val enterTokenTitle = "Enter your token"
    const val enterTokenIntro = "Paste the V2 API token you just generated."
    const val tokenFieldLabel = "V2 API token"
    const val tokenFieldHelper =
        "Looks like a1b2c3d4-… — find it on your WaniKani API Tokens page."
    const val pasteFromClipboard = "Paste from clipboard"
    const val verifyAndContinue = "Verify & continue"

    // Links
    const val wanikaniTokensUrl = "https://www.wanikani.com/settings/personal_access_tokens"

    /**
     * The `blob` URL, not the `raw` one used elsewhere in the app: a raw link serves the file as
     * plain text, which a browser renders unformatted or offers as a download.
     */
    const val privacyPolicyUrl =
        "https://github.com/legarspol/blazing_durtles/blob/main/PRIVACY-POLICY.md"
}
