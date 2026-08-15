package com.smouldering_durtles.wk.ui.sync

import com.smouldering_durtles.wk.data.sync.SyncSnapshot

/**
 * Turns the stream of per-stage snapshots into one cumulative picture of the whole sync.
 *
 * The accumulation is the entire reason this class exists. `LiveApiProgress` reports the *current*
 * stage only and zeroes itself between stages, so nothing downstream can see a whole-sync total
 * without remembering what each finished stage contributed. Since a stage's numbers are gone the
 * instant the next one starts, the tracker holds on to the last figures it saw for the stage in
 * flight and folds those in when the queue moves on.
 *
 * Stateful by necessity, but deliberately not a ViewModel and not Android-aware: feed it a list of
 * snapshots and assert on what comes out.
 */
class SyncProgressTracker {

    private companion object {
        /** No stage seen yet - distinct from -1, which means the queue has drained. */
        const val NOT_STARTED = Int.MIN_VALUE
    }

    private var currentPriority = NOT_STARTED
    private var lastTotal = 0
    private var lastProcessed = 0

    private var completedItems = 0
    private var knownTotal = 0
    private val finishedPerGroup = mutableMapOf<SyncGroup, Int>()

    /** The bar is clamped to its own high-water mark; see [progressFor]. */
    private var highWaterMark = 0f

    fun accept(snapshot: SyncSnapshot): SyncUiState {
        if (snapshot.nextPriority != currentPriority) {
            if (currentPriority != NOT_STARTED) {
                fold(currentPriority, lastTotal, lastProcessed)
            }
            currentPriority = snapshot.nextPriority
            lastTotal = 0
            lastProcessed = 0
        }
        // Only ever move these up. A snapshot taken just after a stage reset reports zeroes, and
        // treating that as "the stage shrank" is what would make the display stutter.
        if (snapshot.totalCount > lastTotal) {
            lastTotal = snapshot.totalCount
        }
        if (snapshot.processedCount > lastProcessed) {
            lastProcessed = snapshot.processedCount
        }

        val runningIndex = runningIndexFor(snapshot.nextPriority)
        return SyncUiState(
            visible = snapshot.firstTimeSetup,
            progress = progressFor(runningIndex),
            itemsSynced = completedItems + lastProcessed,
            itemsTotal = knownTotal + lastTotal,
            rows = SyncGroup.entries.mapIndexed { index, group -> rowFor(group, index, runningIndex) },
        )
    }

    private fun fold(priority: Int, total: Int, processed: Int) {
        if (priority < 0) {
            return
        }
        completedItems += processed
        // A stage that processed items without reporting a total still contributed to the sync, so
        // count what it did rather than letting the denominator fall behind the numerator.
        knownTotal += maxOf(total, processed)
        groupFor(priority)?.let { finishedPerGroup[it] = (finishedPerGroup[it] ?: 0) + processed }
    }

    private fun groupFor(priority: Int): SyncGroup? =
        SyncGroup.entries.firstOrNull { priority <= it.lastPriority }

    /** Index of the group currently running, or null once the queue has drained. */
    private fun runningIndexFor(nextPriority: Int): Int? {
        if (nextPriority < 0) {
            return null
        }
        val index = SyncGroup.entries.indexOfFirst { nextPriority <= it.lastPriority }
        return if (index < 0) null else index
    }

    private fun progressFor(runningIndex: Int?): Float {
        val raw = if (runningIndex == null) {
            1f
        } else {
            val withinStage = if (lastTotal > 0) lastProcessed.toFloat() / lastTotal else 0f
            (runningIndex + withinStage.coerceIn(0f, 1f)) / SyncGroup.entries.size
        }
        // A group can cover more than one stage - "Subjects & mnemonics" covers SRS systems and
        // then subjects - and the within-stage fraction drops back to zero at each handover. The
        // bar is clamped so that internal reset never reads as the sync losing ground.
        highWaterMark = maxOf(highWaterMark, raw)
        return highWaterMark
    }

    private fun rowFor(group: SyncGroup, index: Int, runningIndex: Int?): SyncRow {
        val status = when {
            runningIndex == null || index < runningIndex -> SyncRowStatus.Done
            index == runningIndex -> SyncRowStatus.Running
            else -> SyncRowStatus.Waiting
        }
        return SyncRow(group.label, status, detailFor(group, status))
    }

    private fun detailFor(group: SyncGroup, status: SyncRowStatus): String = when (status) {
        SyncRowStatus.Waiting -> SyncStrings.waiting
        // Single-entity stages (the user fetch, the summary) report no counts at all, so a running
        // row can legitimately have nothing to say; the spinner carries it.
        SyncRowStatus.Running -> if (lastTotal > 0) SyncStrings.ratio(lastProcessed, lastTotal) else ""
        SyncRowStatus.Done -> {
            val finished = finishedPerGroup[group] ?: 0
            if (finished > 0) SyncStrings.count(finished) else SyncStrings.done
        }
    }
}
