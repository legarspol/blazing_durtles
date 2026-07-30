package com.smouldering_durtles.wk.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExitReasonClassifierTest {
    // android.app.ApplicationExitInfo reason codes (API 30+).
    private val reasonExitSelf = 1
    private val reasonLowMemory = 3
    private val reasonCrash = 4
    private val reasonCrashNative = 5
    private val reasonAnr = 6
    private val reasonPermissionChange = 8
    private val reasonExcessiveResourceUsage = 9
    private val reasonUserRequested = 10
    private val reasonUserStopped = 11
    private val reasonDependencyDied = 12
    private val reasonOther = 13

    @Test
    fun anrIsReported() {
        val report = ExitReasonClassifier.classify(reasonAnr, "input dispatching timed out", 100)
        assertEquals("anr", report?.groupKey)
    }

    @Test
    fun lowMemoryIsReported() {
        val report = ExitReasonClassifier.classify(reasonLowMemory, null, 200)
        assertEquals("low_memory", report?.groupKey)
    }

    @Test
    fun excessiveResourceUsageIsReported() {
        val report = ExitReasonClassifier.classify(reasonExcessiveResourceUsage, "excessive cpu", 100)
        assertEquals("excessive_resource_usage", report?.groupKey)
    }

    @Test
    fun crashNativeIsReported() {
        val report = ExitReasonClassifier.classify(reasonCrashNative, "SIGSEGV", 100)
        assertEquals("crash_native", report?.groupKey)
    }

    @Test
    fun otherWithMemoryLimiterDescriptionGetsItsOwnGroupKey() {
        val report = ExitReasonClassifier.classify(reasonOther, "MemoryLimiter:AnonSwap", 100)
        assertEquals("memory_limiter", report?.groupKey)
    }

    @Test
    fun otherWithUnrecognisedDescriptionIsReportedGenerically() {
        val report = ExitReasonClassifier.classify(reasonOther, "some future reason we don't know about", 100)
        assertEquals("other", report?.groupKey)
    }

    @Test
    fun otherWithNullDescriptionIsReportedGenerically() {
        val report = ExitReasonClassifier.classify(reasonOther, null, 100)
        assertEquals("other", report?.groupKey)
    }

    @Test
    fun exitSelfIsNotReported() {
        assertNull(ExitReasonClassifier.classify(reasonExitSelf, null, 100))
    }

    @Test
    fun userRequestedIsNotReported() {
        assertNull(ExitReasonClassifier.classify(reasonUserRequested, null, 100))
    }

    @Test
    fun userStoppedIsNotReported() {
        assertNull(ExitReasonClassifier.classify(reasonUserStopped, null, 100))
    }

    @Test
    fun dependencyDiedIsNotReported() {
        assertNull(ExitReasonClassifier.classify(reasonDependencyDied, null, 100))
    }

    @Test
    fun permissionChangeIsNotReported() {
        assertNull(ExitReasonClassifier.classify(reasonPermissionChange, null, 100))
    }

    @Test
    fun crashIsNotReported() {
        assertNull(ExitReasonClassifier.classify(reasonCrash, "already covered by Crashlytics", 100))
    }

    @Test
    fun reportBodyIncludesFullDescription() {
        val description = "some undocumented shape we've never seen: xyz123"
        val report = ExitReasonClassifier.classify(reasonAnr, description, 100)
        assertEquals(true, report?.detail?.contains(description))
    }
}
