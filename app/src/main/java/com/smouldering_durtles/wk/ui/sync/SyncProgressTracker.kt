package com.smouldering_durtles.wk.ui.sync

import com.smouldering_durtles.wk.data.sync.SyncSnapshot

/**
 * Turns the stream of per-stage snapshots into one cumulative picture of the whole sync.
 *
 * The accumulation is the entire reason this class exists. `LiveApiProgress` reports the *current*
 * stage only and zeroes itself between stages, so nothing downstream can see a whole-sync total
 * without remembering what each finished stage contributed.
 *
 * It remembers per stage *name*, keeping each stage's high-water mark. Both of the alternatives
 * were tried against a real sync and both over-counted. Adding a stage's figures to a running sum
 * when the queue moved past it counted the subject corpus twice, because the queue enqueues
 * follow-up work after draining and so revisits priorities. Keying the same high-water map by
 * queue priority instead *also* double counted, because a task deletes its own row before
 * `LiveApiProgress` stops reporting its numbers - so the queue's minimum priority moves on while
 * nine thousand subjects are still streaming, and the figures land under two different keys.
 *
 * The stage name comes off the same object as the numbers, so the two cannot drift apart. The
 * queue is still the right source for *ordering* the rows, just not for counting.
 *
 * Stateful by necessity, but deliberately not a ViewModel and not Android-aware: feed it a list of
 * snapshots and assert on what comes out.
 */
class SyncProgressTracker {

    private val processedByStage = mutableMapOf<String, Int>()
    private val totalByStage = mutableMapOf<String, Int>()

    /** The bar is clamped to its own high-water mark; see [progressFor]. */
    private var highWaterMark = 0f

    fun accept(snapshot: SyncSnapshot): SyncUiState {
        record(snapshot)

        val running = runningGroup(snapshot)
        val runningIndex = running?.ordinal
        return SyncUiState(
            visible = snapshot.firstTimeSetup,
            progress = progressFor(running, snapshot.entityName),
            itemsSynced = processedByStage.values.sum(),
            itemsTotal = totalByStage.values.sum(),
            rows = SyncGroup.entries.mapIndexed { index, group ->
                rowFor(group, index, runningIndex, snapshot.entityName)
            },
        )
    }

    private fun record(snapshot: SyncSnapshot) {
        val stage = snapshot.entityName
        if (stage.isEmpty()) {
            return
        }
        // Only ever move these up. A snapshot taken just after a stage reset reports zeroes, and
        // treating that as "the stage shrank" is what would make the display stutter.
        processedByStage[stage] = maxOf(processedByStage[stage] ?: 0, snapshot.processedCount)
        // A stage that processes items without reporting a total still contributed to the sync, so
        // count what it did rather than letting the denominator fall behind the numerator.
        totalByStage[stage] =
            maxOf(totalByStage[stage] ?: 0, snapshot.totalCount, snapshot.processedCount)
    }

    /**
     * Which row is running. The stage name wins when there is one, so that the highlighted row and
     * the figures beside it always describe the same thing; the queue's next priority is the
     * fallback for the single-entity stages (the user fetch, the summary) that report no name.
     */
    private fun runningGroup(snapshot: SyncSnapshot): SyncGroup? {
        if (snapshot.nextPriority < 0) {
            return null
        }
        return SyncStages.groupOf(snapshot.entityName)
            ?: SyncGroup.entries.firstOrNull { snapshot.nextPriority <= it.lastPriority }
    }

    private fun progressFor(running: SyncGroup?, stage: String): Float {
        val raw = if (running == null) {
            1f
        } else {
            val before = SyncGroup.entries.take(running.ordinal).sumOf { it.weight.toDouble() }
            before.toFloat() + running.weight * fractionWithin(stage)
        }
        // A row can cover more than one stage - "Subjects & mnemonics" covers SRS systems and then
        // subjects - and the within-stage fraction drops back to zero at each handover. The bar is
        // clamped so that internal reset never reads as the sync losing ground.
        highWaterMark = maxOf(highWaterMark, raw)
        return highWaterMark
    }

    private fun fractionWithin(stage: String): Float {
        val total = totalByStage[stage] ?: 0
        if (total <= 0) {
            return 0f
        }
        return ((processedByStage[stage] ?: 0).toFloat() / total).coerceIn(0f, 1f)
    }

    private fun rowFor(group: SyncGroup, index: Int, runningIndex: Int?, stage: String): SyncRow {
        val status = when {
            runningIndex == null || index < runningIndex -> SyncRowStatus.Done
            index == runningIndex -> SyncRowStatus.Running
            else -> SyncRowStatus.Waiting
        }
        return SyncRow(group.label, status, detailFor(group, status, stage))
    }

    private fun detailFor(group: SyncGroup, status: SyncRowStatus, stage: String): String =
        when (status) {
            SyncRowStatus.Waiting -> SyncStrings.waiting
            // Single-entity stages (the user fetch, the summary) report no counts at all, so a
            // running row can legitimately have nothing to say; the spinner carries it.
            SyncRowStatus.Running -> {
                val total = totalByStage[stage] ?: 0
                if (total > 0) SyncStrings.ratio(processedByStage[stage] ?: 0, total) else ""
            }
            SyncRowStatus.Done -> {
                val finished = itemsIn(group)
                if (finished > 0) SyncStrings.count(finished) else SyncStrings.done
            }
        }

    private fun itemsIn(group: SyncGroup): Int = processedByStage.entries
        .filter { SyncStages.groupOf(it.key) == group }
        .sumOf { it.value }
}

/**
 * The names `LiveApiProgress` is given for each stage, and the row each belongs to.
 *
 * These are the exact strings the tasks pass to `LiveApiProgress.reset`, capitalisation included -
 * "Level progression" really does have the odd capital, and "statistics" really is the label for
 * review statistics. They are matched rather than parsed, so an unrecognised name simply falls
 * through to the queue-priority path instead of landing in the wrong row.
 */
object SyncStages {
    private val groups = mapOf(
        "reference data" to SyncGroup.Profile,
        "SRS systems" to SyncGroup.Subjects,
        "subjects" to SyncGroup.Subjects,
        "assignments" to SyncGroup.Assignments,
        "statistics" to SyncGroup.Assignments,
        "study materials" to SyncGroup.Assignments,
        "Level progression" to SyncGroup.Forecast,
    )

    fun groupOf(entityName: String): SyncGroup? = groups[entityName]
}
