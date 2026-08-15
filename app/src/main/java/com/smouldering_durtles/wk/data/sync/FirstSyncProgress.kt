package com.smouldering_durtles.wk.data.sync

import androidx.lifecycle.asFlow
import com.smouldering_durtles.wk.WkApplication
import com.smouldering_durtles.wk.livedata.LiveApiProgress
import com.smouldering_durtles.wk.livedata.LiveFirstTimeSetup
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * One reading of how far the sync has got.
 *
 * [nextPriority] is the whole ordering story: tasks run strictly lowest-priority-first, one at a
 * time, so every stage below it has finished and the stage at it is the one running. -1 means the
 * queue is empty.
 *
 * [totalCount] and [processedCount] describe only the *currently running* stage, and both reset to
 * zero between stages — accumulating across a whole sync is [SyncProgressTracker]'s job, not this
 * type's. Single-entity stages (user, summary) never report either, so both stay 0 while they run.
 */
data class SyncSnapshot(
    val firstTimeSetup: Boolean,
    val nextPriority: Int,
    val entityName: String,
    val totalCount: Int,
    val processedCount: Int,
)

/**
 * The seam between the new Compose sync screen and the legacy progress plumbing.
 *
 * Everything awkward about that plumbing is contained here. `LiveApiProgress` is a degenerate
 * LiveData whose value is a throwaway `Object` — all the real numbers live in statics — so each
 * emission is a bare "something changed" tick that has to be turned back into a value by reading
 * those statics. It reaches for `WkApplication.getDatabase()` for the same reason: MainActivity is
 * legacy Java with no Hilt graph to inject from.
 *
 * Keeping all of that in one file is the point. When Phase 2 and 3 replace the task queue, this is
 * the single thing that has to change, and nothing in `ui/sync` has to know it happened.
 */
class FirstSyncProgress {

    fun snapshots(): Flow<SyncSnapshot> {
        val dao = WkApplication.getDatabase().taskDefinitionDao()
        return combine(
            LiveApiProgress.getInstance().asFlow(),
            dao.getLiveNextApiPriority().asFlow(),
            LiveFirstTimeSetup.getInstance().asFlow(),
        ) { _, nextPriority, firstTimeSetup ->
            SyncSnapshot(
                firstTimeSetup = firstTimeSetup == 0,
                nextPriority = nextPriority,
                entityName = LiveApiProgress.getEntityName(),
                totalCount = LiveApiProgress.getTotalCount(),
                processedCount = LiveApiProgress.getNumProcessedEntities(),
            )
        }
    }
}
