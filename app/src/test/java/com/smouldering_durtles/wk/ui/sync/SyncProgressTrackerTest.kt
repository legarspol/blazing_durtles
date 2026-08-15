package com.smouldering_durtles.wk.ui.sync

import com.smouldering_durtles.wk.data.sync.SyncSnapshot
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

/**
 * The sync stages, by the priority the task queue stamps on them. Spelled out here rather than
 * imported so the test fails if someone regroups the rows without meaning to.
 */
private const val REFERENCE_DATA = 1
private const val USER = 2
private const val SRS_SYSTEMS = 10
private const val SUBJECTS = 20
private const val ASSIGNMENTS = 21
private const val REVIEW_STATISTICS = 22
private const val STUDY_MATERIALS = 23
private const val SUMMARY = 25
private const val LEVEL_PROGRESSION = 26
private const val DRAINED = -1

private fun snapshot(
    priority: Int,
    total: Int = 0,
    processed: Int = 0,
    entityName: String = "",
    firstTimeSetup: Boolean = true,
) = SyncSnapshot(firstTimeSetup, priority, entityName, total, processed)

class SyncProgressTrackerTest {

    @Test
    fun `first row runs while the earliest stages are queued`() {
        val state = SyncProgressTracker().accept(snapshot(REFERENCE_DATA))

        assertEquals(
            listOf(SyncRowStatus.Running, SyncRowStatus.Waiting, SyncRowStatus.Waiting, SyncRowStatus.Waiting),
            state.rows.map { it.status },
        )
        assertEquals(SyncStrings.rowProfile, state.rows[0].label)
    }

    @Test
    fun `rows are ordered the way the queue actually runs them`() {
        assertEquals(
            listOf(
                SyncStrings.rowProfile,
                SyncStrings.rowSubjects,
                SyncStrings.rowAssignments,
                SyncStrings.rowForecast,
            ),
            SyncProgressTracker().accept(snapshot(USER)).rows.map { it.label },
        )
    }

    @Test
    fun `each stage maps to the row that covers it`() {
        val expected = mapOf(
            REFERENCE_DATA to 0,
            USER to 0,
            SRS_SYSTEMS to 1,
            SUBJECTS to 1,
            ASSIGNMENTS to 2,
            REVIEW_STATISTICS to 2,
            STUDY_MATERIALS to 2,
            SUMMARY to 3,
            LEVEL_PROGRESSION to 3,
        )
        expected.forEach { (priority, rowIndex) ->
            val rows = SyncProgressTracker().accept(snapshot(priority)).rows
            assertEquals(
                SyncRowStatus.Running,
                rows[rowIndex].status,
                "priority $priority should light up row $rowIndex",
            )
        }
    }

    @Test
    fun `everything is done once the queue drains`() {
        val tracker = SyncProgressTracker()
        tracker.accept(snapshot(SUBJECTS, total = 9000, processed = 9000))
        val state = tracker.accept(snapshot(DRAINED))

        assertTrue(state.rows.all { it.status == SyncRowStatus.Done })
        assertEquals(1f, state.progress)
    }

    @Test
    fun `a running stage shows processed over its real total`() {
        val state = SyncProgressTracker()
            .accept(snapshot(SUBJECTS, total = 2589, processed = 783, entityName = "subjects"))

        assertEquals("783 / 2,589", state.rows[1].detail)
    }

    @Test
    fun `a single-entity stage runs without any numbers to show`() {
        val state = SyncProgressTracker().accept(snapshot(USER))

        assertEquals("", state.rows[0].detail)
        assertEquals(SyncStrings.waiting, state.rows[1].detail)
    }

    @Test
    fun `counts accumulate across stages even though each stage resets`() {
        val tracker = SyncProgressTracker()
        tracker.accept(snapshot(SUBJECTS, total = 2000, processed = 2000))
        // The queue moves on and LiveApiProgress zeroes itself before the next stage reports.
        tracker.accept(snapshot(ASSIGNMENTS))
        val state = tracker.accept(snapshot(ASSIGNMENTS, total = 500, processed = 120))

        assertEquals(2120, state.itemsSynced)
        assertEquals(2500, state.itemsTotal)
    }

    @Test
    fun `a finished row keeps the count it contributed`() {
        val tracker = SyncProgressTracker()
        tracker.accept(snapshot(ASSIGNMENTS, total = 2431, processed = 2431))
        val state = tracker.accept(snapshot(SUMMARY))

        assertEquals("2,431", state.rows[2].detail)
    }

    @Test
    fun `a finished row that reported no counts just says done`() {
        val tracker = SyncProgressTracker()
        tracker.accept(snapshot(USER))
        val state = tracker.accept(snapshot(SUBJECTS))

        assertEquals(SyncStrings.done, state.rows[0].detail)
    }

    @Test
    fun `the bar never goes backwards when a stage resets mid-row`() {
        val tracker = SyncProgressTracker()
        // SRS systems and subjects share the "Subjects & mnemonics" row, so the within-stage
        // fraction drops to zero at the handover between them.
        val afterSrs = tracker.accept(snapshot(SRS_SYSTEMS, total = 2, processed = 2)).progress
        val atHandover = tracker.accept(snapshot(SUBJECTS)).progress
        val intoSubjects = tracker.accept(snapshot(SUBJECTS, total = 9000, processed = 100)).progress

        assertTrue(atHandover >= afterSrs, "progress dipped at the handover: $afterSrs -> $atHandover")
        assertTrue(intoSubjects >= atHandover)
    }

    @Test
    fun `the bar never goes backwards across a whole sync`() {
        val tracker = SyncProgressTracker()
        val run = listOf(
            snapshot(REFERENCE_DATA, total = 300, processed = 300),
            snapshot(USER),
            snapshot(SRS_SYSTEMS, total = 2, processed = 2),
            snapshot(SUBJECTS, total = 9000, processed = 1000),
            snapshot(SUBJECTS, total = 9000, processed = 9000),
            snapshot(ASSIGNMENTS, total = 2431, processed = 2431),
            snapshot(REVIEW_STATISTICS, total = 2431, processed = 2431),
            snapshot(STUDY_MATERIALS, total = 40, processed = 40),
            snapshot(SUMMARY),
            snapshot(LEVEL_PROGRESSION, total = 24, processed = 24),
            snapshot(DRAINED),
        )

        var previous = 0f
        run.forEach { step ->
            val progress = tracker.accept(step).progress
            assertTrue(progress >= previous, "progress went backwards at $step: $previous -> $progress")
            previous = progress
        }
        assertEquals(1f, previous)
    }

    @Test
    fun `visibility follows first time setup`() {
        val tracker = SyncProgressTracker()

        assertTrue(tracker.accept(snapshot(SUBJECTS)).visible)
        assertFalse(tracker.accept(snapshot(SUBJECTS, firstTimeSetup = false)).visible)
    }

    @Test
    fun `the item caption stays hidden until a stage reports a real size`() {
        val tracker = SyncProgressTracker()

        assertFalse(tracker.accept(snapshot(USER)).showItemCount)
        assertTrue(tracker.accept(snapshot(SUBJECTS, total = 9000, processed = 10)).showItemCount)
    }
}
