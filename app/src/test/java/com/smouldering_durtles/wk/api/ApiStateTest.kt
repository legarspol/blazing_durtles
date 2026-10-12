package com.smouldering_durtles.wk.api

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Characterization test for issue #102: in [ApiState.ERROR], [ApiState.canGetUserData] must be
 * true so that a queued `GetUserTask` is reachable and can probe whether the server/network has
 * recovered, instead of the state latching until the user presses Retry.
 */
class ApiStateTest {
    @Test
    fun `GetUserTask is reachable from ERROR`() {
        assertTrue(ApiState.ERROR.canGetUserData())
    }

    @Test
    fun `sticky key states still keep GetUserTask unreachable`() {
        assertFalse(ApiState.API_KEY_MISSING.canGetUserData())
        assertFalse(ApiState.API_KEY_REJECTED.canGetUserData())
    }
}
