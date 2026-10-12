package com.smouldering_durtles.wk.ui.sync

import com.smouldering_durtles.wk.data.sync.SyncSnapshot
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

/**
 * The sync stages, by the priority the task queue stamps on them and the name the task passes to
 * LiveApiProgress. Spelled out here rather than imported so the test fails if someone regroups the
 * rows without meaning to.
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
    stage: String = "",
    firstTimeSetup: Boolean = true,
) = SyncSnapshot(firstTimeSetup, priority, stage, total, processed)

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
    fun `every stage name the tasks use maps to a row`() {
        val expected = mapOf(
            "reference data" to SyncGroup.Profile,
            "SRS systems" to SyncGroup.Subjects,
            "subjects" to SyncGroup.Subjects,
            "assignments" to SyncGroup.Assignments,
            "statistics" to SyncGroup.Assignments,
            "study materials" to SyncGroup.Assignments,
            "Level progression" to SyncGroup.Forecast,
        )
        expected.forEach { (name, group) ->
            assertEquals(group, SyncStages.groupOf(name), "stage name $name")
        }
    }

    @Test
    fun `everything is done once the queue drains`() {
        val tracker = SyncProgressTracker()
        tracker.accept(snapshot(SUBJECTS, total = 9000, processed = 9000, stage = "subjects"))
        val state = tracker.accept(snapshot(DRAINED))

        assertTrue(state.rows.all { it.status == SyncRowStatus.Done })
        assertEquals(1f, state.progress)
    }

    @Test
    fun `a running stage shows processed over its real total`() {
        val state = SyncProgressTracker()
            .accept(snapshot(SUBJECTS, total = 2589, processed = 783, stage = "subjects"))

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
        tracker.accept(snapshot(SUBJECTS, total = 2000, processed = 2000, stage = "subjects"))
        // The queue moves on and LiveApiProgress zeroes itself before the next stage reports.
        tracker.accept(snapshot(ASSIGNMENTS))
        val state = tracker.accept(snapshot(ASSIGNMENTS, total = 500, processed = 120, stage = "assignments"))

        assertEquals(2120, state.itemsSynced)
        assertEquals(2500, state.itemsTotal)
    }

    @Test
    fun `a stage is not counted twice when the queue moves on before it stops reporting`() {
        val tracker = SyncProgressTracker()
        // A task deletes its own row before LiveApiProgress stops reporting its numbers, so the
        // queue's minimum priority moves to the next stage while subjects are still streaming.
        // Observed live as the item total doubling to 18,880.
        tracker.accept(snapshot(SUBJECTS, total = 9427, processed = 5000, stage = "subjects"))
        tracker.accept(snapshot(ASSIGNMENTS, total = 9427, processed = 9427, stage = "subjects"))
        val state = tracker.accept(snapshot(ASSIGNMENTS, total = 26, processed = 26, stage = "assignments"))

        assertEquals(9453, state.itemsSynced)
        assertEquals(9453, state.itemsTotal)
    }

    @Test
    fun `a stage the queue revisits is not counted twice`() {
        val tracker = SyncProgressTracker()
        tracker.accept(snapshot(SUBJECTS, total = 9427, processed = 9427, stage = "subjects"))
        // ApiTaskService enqueues follow-up work once it has drained, so the queue comes back
        // round to a stage that already ran.
        tracker.accept(snapshot(ASSIGNMENTS, total = 15, processed = 15, stage = "assignments"))
        tracker.accept(snapshot(SUBJECTS, total = 9427, processed = 9427, stage = "subjects"))
        val state = tracker.accept(snapshot(ASSIGNMENTS, total = 15, processed = 15, stage = "assignments"))

        assertEquals(9442, state.itemsSynced)
        assertEquals(9442, state.itemsTotal)
    }

    @Test
    fun `a finished row keeps the count it contributed`() {
        val tracker = SyncProgressTracker()
        tracker.accept(snapshot(ASSIGNMENTS, total = 2431, processed = 2431, stage = "assignments"))
        val state = tracker.accept(snapshot(SUMMARY))

        assertEquals("2,431", state.rows[2].detail)
    }

    @Test
    fun `a finished row that reported no counts just says done`() {
        val tracker = SyncProgressTracker()
        tracker.accept(snapshot(USER))
        val state = tracker.accept(snapshot(SUBJECTS, stage = "subjects"))

        assertEquals(SyncStrings.done, state.rows[0].detail)
    }

    @Test
    fun `the subject corpus carries most of the bar`() {
        val tracker = SyncProgressTracker()
        val start = tracker.accept(snapshot(SUBJECTS, total = 9427, processed = 0, stage = "subjects")).progress
        val halfway = tracker.accept(snapshot(SUBJECTS, total = 9427, processed = 4713, stage = "subjects")).progress
        val finished = tracker.accept(snapshot(SUBJECTS, total = 9427, processed = 9427, stage = "subjects")).progress

        // Evenly weighted rows moved the bar a quarter across the longest stage of the sync, which
        // read as a bar that was not moving at all.
        assertTrue(
            halfway - start > 0.3f,
            "subjects moved the bar only ${halfway - start} in its first half",
        )
        assertTrue(finished >= 0.75f, "subjects finished at $finished")
    }

    @Test
    fun `the bar never goes backwards when a stage resets mid-row`() {
        val tracker = SyncProgressTracker()
        // SRS systems and subjects share the "Subjects & mnemonics" row, so the within-stage
        // fraction drops to zero at the handover between them.
        val afterSrs = tracker.accept(snapshot(SRS_SYSTEMS, total = 2, processed = 2, stage = "SRS systems")).progress
        val atHandover = tracker.accept(snapshot(SUBJECTS, stage = "subjects")).progress
        val intoSubjects = tracker.accept(snapshot(SUBJECTS, total = 9000, processed = 100, stage = "subjects")).progress

        assertTrue(atHandover >= afterSrs, "progress dipped at the handover: $afterSrs -> $atHandover")
        assertTrue(intoSubjects >= atHandover)
    }

    @Test
    fun `the bar never goes backwards across a whole sync`() {
        val tracker = SyncProgressTracker()
        val run = listOf(
            snapshot(REFERENCE_DATA, total = 300, processed = 300, stage = "reference data"),
            snapshot(USER),
            snapshot(SRS_SYSTEMS, total = 2, processed = 2, stage = "SRS systems"),
            snapshot(SUBJECTS, total = 9427, processed = 1000, stage = "subjects"),
            snapshot(SUBJECTS, total = 9427, processed = 9427, stage = "subjects"),
            // The queue runs ahead of the reporter, exactly as it does on a device.
            snapshot(ASSIGNMENTS, total = 9427, processed = 9427, stage = "subjects"),
            snapshot(ASSIGNMENTS, total = 26, processed = 26, stage = "assignments"),
            snapshot(REVIEW_STATISTICS, total = 11, processed = 11, stage = "statistics"),
            snapshot(STUDY_MATERIALS, total = 0, processed = 0, stage = "study materials"),
            snapshot(SUMMARY),
            snapshot(LEVEL_PROGRESSION, total = 1, processed = 1, stage = "Level progression"),
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
    fun `a whole sync totals what its stages actually reported`() {
        val tracker = SyncProgressTracker()
        var state = SyncUiState()
        listOf(
            snapshot(SRS_SYSTEMS, total = 2, processed = 2, stage = "SRS systems"),
            snapshot(SUBJECTS, total = 9427, processed = 9427, stage = "subjects"),
            snapshot(ASSIGNMENTS, total = 9427, processed = 9427, stage = "subjects"),
            snapshot(ASSIGNMENTS, total = 26, processed = 26, stage = "assignments"),
            snapshot(REVIEW_STATISTICS, total = 11, processed = 11, stage = "statistics"),
            snapshot(LEVEL_PROGRESSION, total = 1, processed = 1, stage = "Level progression"),
        ).forEach { state = tracker.accept(it) }

        assertEquals(2 + 9427 + 26 + 11 + 1, state.itemsSynced)
        assertEquals(2 + 9427 + 26 + 11 + 1, state.itemsTotal)
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
        assertTrue(tracker.accept(snapshot(SUBJECTS, total = 9000, processed = 10, stage = "subjects")).showItemCount)
    }
}
