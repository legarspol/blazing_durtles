package com.smouldering_durtles.wk.ui.sync

/** How far along one row of the checklist is. */
enum class SyncRowStatus { Done, Running, Waiting }

/** One row of the checklist: what it covers, where it has got to, and its trailing figure. */
data class SyncRow(
    val label: String,
    val status: SyncRowStatus,
    val detail: String,
)

/**
 * The four rows, in the order the sync actually runs them.
 *
 * Tasks drain in strict priority order, so a row can only ever cover a *contiguous* run of
 * priorities — a row spanning, say, the user fetch (2) and level progressions (26) would go
 * running, then waiting, then running again as the stages in between overtook it. That constraint
 * is why "Subjects & mnemonics" sits above "Assignments" here while the design board draws them
 * the other way round.
 *
 * [lastPriority] is the inclusive upper bound of the group. The lower bound is implied by the
 * previous entry, which is what keeps the groups gapless.
 */
enum class SyncGroup(val label: String, val lastPriority: Int) {
    /** Reference data (1) and the user fetch (2). */
    Profile(SyncStrings.rowProfile, 2),

    /** SRS systems (10) and subjects (20) - the corpus, and by far the longest stage. */
    Subjects(SyncStrings.rowSubjects, 20),

    /** Assignments (21), review statistics (22) and study materials (23) - the user's own progress. */
    Assignments(SyncStrings.rowAssignments, 23),

    /** The summary (25) and level progressions (26), which is what the timeline is built from. */
    Forecast(SyncStrings.rowForecast, 26),
}

/**
 * Everything the first-sync screen renders from.
 *
 * [progress] is stage-weighted rather than item-weighted: finished rows plus the fraction of the
 * running one. A stage's true size only arrives with its first page, so an item-weighted bar would
 * have to guess at the total and then walk backwards when it guessed low. This one only ever moves
 * forward.
 */
data class SyncUiState(
    val visible: Boolean = false,
    val progress: Float = 0f,
    val itemsSynced: Int = 0,
    val itemsTotal: Int = 0,
    val rows: List<SyncRow> = emptyList(),
) {
    /** The caption is only worth showing once at least one stage has reported a real size. */
    val showItemCount: Boolean get() = itemsTotal > 0
}
