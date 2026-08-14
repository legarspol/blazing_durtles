package com.smouldering_durtles.wk.ui.onboarding

/**
 * Matches `GlobalSettings.Api.getApiKey()`, which applies this same pattern on every **read**
 * (`GlobalSettings.java:786`). Normalising with anything weaker — `trim()`, say — would let a
 * token be validated in one form and read back in another: paste `"abc def"` and the store hands
 * back `"abcdef"` forever after.
 */
private val whitespace = Regex("[\\p{Z}\\s]+")

/** WaniKani personal access tokens are UUIDs. */
private val tokenShape =
    Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$", RegexOption.IGNORE_CASE)

/** Strips every space the user or their clipboard may have introduced. */
fun normalizeApiToken(raw: String): String = whitespace.replace(raw, "")

/**
 * Whether [raw] could be a WaniKani API token.
 *
 * This is a shape check, not proof the token works — only WaniKani can say that, and it does so
 * later through `GetUserTask`, which raises the dashboard's rejected-token banner on an HTTP 401.
 * Its job is to stop the old screen's worst behaviour, where any string at all (including an
 * empty one, which then bounced the user straight back) was accepted and stored.
 */
fun isValidApiToken(raw: String): Boolean = tokenShape.matches(normalizeApiToken(raw))

/** Where onboarding should open. Welcome is brand recognition and is shown once per install. */
fun startDestinationFor(welcomeSeen: Boolean): OnboardingDestination =
    if (welcomeSeen) OnboardingDestination.Connect else OnboardingDestination.Welcome
