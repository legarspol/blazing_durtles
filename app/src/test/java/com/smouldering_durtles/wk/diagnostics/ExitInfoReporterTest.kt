package com.smouldering_durtles.wk.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the watermark dedupe logic in [ExitInfoReporter.planScan] — the part that decides which
 * exit records are new and how far the watermark advances. Extracting it from the `ActivityManager`
 * read is what makes it testable at all.
 *
 * Which reasons are considered reportable is [ExitReasonClassifierTest]'s job, not this one's.
 */
class ExitInfoReporterTest {
    // android.app.ApplicationExitInfo reason codes (API 30+).
    private val reasonAnr = 6
    private val reasonLowMemory = 3
    private val reasonUserRequested = 10

    private fun anr(timestamp: Long) = ExitRecord(timestamp, reasonAnr, "input dispatching timed out", 100)

    private fun userRequested(timestamp: Long) = ExitRecord(timestamp, reasonUserRequested, null, 100)

    @Test
    fun `records at or below the watermark are skipped`() {
        val plan = ExitInfoReporter.planScan(listOf(anr(50), anr(100)), watermark = 100)

        assertTrue(plan.reports.isEmpty())
        assertNull(plan.newWatermark)
    }

    @Test
    fun `records above the watermark are reported`() {
        val plan = ExitInfoReporter.planScan(listOf(anr(150), anr(200)), watermark = 100)

        assertEquals(listOf("anr", "anr"), plan.reports.map { it.groupKey })
        assertEquals(200L, plan.newWatermark)
    }

    @Test
    fun `only the records above the watermark are reported from a mixed batch`() {
        val records = listOf(anr(50), anr(100), anr(150), ExitRecord(200, reasonLowMemory, null, 200))

        val plan = ExitInfoReporter.planScan(records, watermark = 100)

        assertEquals(listOf("anr", "low_memory"), plan.reports.map { it.groupKey })
        assertEquals(200L, plan.newWatermark)
    }

    @Test
    fun `watermark advances to the newest record regardless of input order`() {
        val plan = ExitInfoReporter.planScan(listOf(anr(300), anr(150), anr(220)), watermark = 100)

        assertEquals(300L, plan.newWatermark)
    }

    @Test
    fun `an unreportable but recent record still advances the watermark`() {
        // Otherwise this record is re-examined on every app start, forever.
        val plan = ExitInfoReporter.planScan(listOf(userRequested(500)), watermark = 100)

        assertTrue(plan.reports.isEmpty())
        assertEquals(500L, plan.newWatermark)
    }

    @Test
    fun `a zero watermark reports everything on a first run`() {
        val plan = ExitInfoReporter.planScan(listOf(anr(1), anr(2)), watermark = 0)

        assertEquals(2, plan.reports.size)
        assertEquals(2L, plan.newWatermark)
    }

    @Test
    fun `an empty batch leaves the watermark alone`() {
        val plan = ExitInfoReporter.planScan(emptyList(), watermark = 100)

        assertTrue(plan.reports.isEmpty())
        assertNull(plan.newWatermark)
    }

    @Test
    fun `rescanning the same batch after the watermark advanced reports nothing`() {
        val records = listOf(anr(150), anr(200))

        val first = ExitInfoReporter.planScan(records, watermark = 0)
        val second = ExitInfoReporter.planScan(records, watermark = first.newWatermark!!)

        assertEquals(2, first.reports.size)
        assertTrue(second.reports.isEmpty())
        assertNull(second.newWatermark)
    }
}
