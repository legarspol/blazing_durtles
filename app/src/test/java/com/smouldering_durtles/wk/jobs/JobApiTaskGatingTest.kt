package com.smouldering_durtles.wk.jobs

import com.smouldering_durtles.wk.api.ApiState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Characterization test for issue #102: [Job.blocksAutomaticApiTasks] decides whether
 * `assertDueTasks` clears `canTriggerApiTasks`, which is what used to guarantee that a queued
 * `GetUserTask` could never actually run while in [ApiState.ERROR]. A rejected or missing key is
 * still sticky and needs the user to act; a transport blip or server error must not be.
 */
class JobApiTaskGatingTest {
    @Test
    fun `sticky key states still block other API tasks`() {
        assertTrue(Job.blocksAutomaticApiTasks(ApiState.API_KEY_MISSING))
        assertTrue(Job.blocksAutomaticApiTasks(ApiState.API_KEY_REJECTED))
    }

    @Test
    fun `ERROR no longer blocks the probe from running`() {
        assertFalse(Job.blocksAutomaticApiTasks(ApiState.ERROR))
    }

    @Test
    fun `the states that were never blocked stay unblocked`() {
        assertFalse(Job.blocksAutomaticApiTasks(ApiState.UNKNOWN))
        assertFalse(Job.blocksAutomaticApiTasks(ApiState.EXPIRED))
        assertFalse(Job.blocksAutomaticApiTasks(ApiState.REFRESH_USER_DATA))
        assertFalse(Job.blocksAutomaticApiTasks(ApiState.OK))
    }
}
