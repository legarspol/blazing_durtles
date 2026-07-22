package com.smouldering_durtles.wk.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CoreSmokeTest {
    @Test
    fun statusReportsWiredUp() {
        assertEquals("core is wired up", CoreSmoke().status)
    }

    @Test
    fun wiredAtReturnsTheCurrentInstant() {
        assertTrue(CoreSmoke().wiredAt().epochSeconds > 0)
    }
}
