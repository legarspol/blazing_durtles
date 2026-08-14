package com.smouldering_durtles.wk.ui.onboarding

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

/** Two real WaniKani personal access tokens, used as the shape of record. */
private const val TOKEN = "029c9659-4af7-488a-8c3c-5f0f852ba034"
private const val OTHER_TOKEN = "1368c809-8276-45f6-8143-d6ec76131218"

class ApiTokenFormatTest {

    @Test
    fun `accepts real tokens`() {
        assertTrue(isValidApiToken(TOKEN))
        assertTrue(isValidApiToken(OTHER_TOKEN))
    }

    @Test
    fun `accepts uppercase`() {
        assertTrue(isValidApiToken(TOKEN.uppercase()))
    }

    @Test
    fun `rejects what the old screen used to accept`() {
        // NoApiKeyHelpActivity stored any of these, then bounced the user straight back.
        assertFalse(isValidApiToken(""))
        assertFalse(isValidApiToken("   "))
        assertFalse(isValidApiToken("not-a-token"))
    }

    @Test
    fun `rejects near misses`() {
        assertFalse(isValidApiToken(TOKEN.dropLast(1)))
        assertFalse(isValidApiToken(TOKEN + "0"))
        assertFalse(isValidApiToken(TOKEN.replace('-', '_')))
        assertFalse(isValidApiToken(TOKEN.replaceFirst("0", "g")))
    }

    @Test
    fun `strips whitespace anywhere, not just the ends`() {
        // The whole point: GlobalSettings.getApiKey() strips every space on read, so validating
        // a token that trim() would have left intact means storing something else entirely.
        assertEquals(TOKEN, normalizeApiToken("  $TOKEN  "))
        assertEquals(TOKEN, normalizeApiToken(TOKEN.replaceFirst("-", " - ")))
        assertEquals(TOKEN, normalizeApiToken(TOKEN.replaceFirst("-", "\n-")))
        // A non-breaking space, which \s alone would miss. The legacy pattern pairs
        // \s with \p{Z} for exactly this, and so does ours.
        assertEquals(TOKEN, normalizeApiToken(TOKEN.replaceFirst("-", "\u00A0-")))
        assertEquals(TOKEN, normalizeApiToken(TOKEN.replaceFirst("-", "- ")))
    }

    @Test
    fun `a token split by a space is still valid`() {
        val pasted = TOKEN.replaceFirst("-", "- ")
        assertTrue(isValidApiToken(pasted))
        assertEquals(TOKEN, normalizeApiToken(pasted))
    }

    @Test
    fun `welcome is shown once per install`() {
        assertEquals(OnboardingDestination.Welcome, startDestinationFor(welcomeSeen = false))
        assertEquals(OnboardingDestination.Connect, startDestinationFor(welcomeSeen = true))
    }
}
