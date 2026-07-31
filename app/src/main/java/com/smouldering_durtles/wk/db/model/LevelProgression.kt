package com.smouldering_durtles.wk.db.model

/**
 * Room entity for the level_progression table. This records the coarse-grained progression in levels.
 */
class LevelProgression {
    /**
     * The unique ID.
     */
    var id: Long = 0L

    /**
     * Timestamp when this level was abandoned (because of a reset), or null if not abandoned.
     */
    var abandonedAt: Long = 0L

    /**
     * Timestamp when this level was completed (all subjects burned), or null if not completed.
     */
    var completedAt: Long = 0L

    /**
     * Timestamp when this record was created.
     */
    var createdAt: Long = 0L

    /**
     * Timestamp when this level was passed (all subjects passed), or null if not passed.
     */
    var passedAt: Long = 0L

    /**
     * Timestamp when this level was started, or null if not started.
     */
    var startedAt: Long = 0L

    /**
     * Timestamp when this level was unlocked, or null if not unlocked.
     */
    var unlockedAt: Long = 0L

    /**
     * The level this record applies to.
     */
    var level: Int = 0

    /**
     * Get the timestamp since when the user reached this level.
     * Based on unlockedAt, with startedAt as fallback.
     *
     * @return the date
     */
    fun getSince(): Long {
        if (unlockedAt != 0L) {
            return unlockedAt
        }
        if (startedAt != 0L) {
            return startedAt
        }
        return 0
    }
}
