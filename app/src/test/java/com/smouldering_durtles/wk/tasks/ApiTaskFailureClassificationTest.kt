package com.smouldering_durtles.wk.tasks

import com.smouldering_durtles.wk.tasks.ApiTask.ApiFailureKind
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Characterization tests for [ApiTask.classifyFailure], the seam that used to treat a DNS/timeout
 * failure identically to a real server error and latch `api_in_error` permanently (issue #102). A
 * transport failure never reaches an HTTP response, so it is reported as code 0; it must classify
 * differently from an actual HTTP error response.
 */
class ApiTaskFailureClassificationTest {
    @Test
    fun `no response at all classifies as transport, not a server error`() {
        // This is what connection getResponseCode() throwing leaves behind: UnknownHostException,
        // SocketTimeoutException and ConnectException all land here with code still at its initial 0.
        assertEquals(ApiFailureKind.TRANSPORT, ApiTask.classifyFailure(0))
    }

    @Test
    fun `5xx classifies as a server error`() {
        assertEquals(ApiFailureKind.SERVER_ERROR, ApiTask.classifyFailure(500))
        assertEquals(ApiFailureKind.SERVER_ERROR, ApiTask.classifyFailure(503))
    }

    @Test
    fun `other unexpected HTTP codes also classify as a server error`() {
        assertEquals(ApiFailureKind.SERVER_ERROR, ApiTask.classifyFailure(404))
    }

    @Test
    fun `401 classifies as unauthorized`() {
        assertEquals(ApiFailureKind.UNAUTHORIZED, ApiTask.classifyFailure(401))
    }

    @Test
    fun `429 classifies as rate limited`() {
        assertEquals(ApiFailureKind.RATE_LIMITED, ApiTask.classifyFailure(429))
    }

    @Test
    fun `422 classifies as unprocessable entity`() {
        assertEquals(ApiFailureKind.UNPROCESSABLE_ENTITY, ApiTask.classifyFailure(422))
    }
}
