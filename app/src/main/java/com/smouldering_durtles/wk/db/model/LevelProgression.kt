package com.smouldering_durtles.wk.db.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for the level_progression table. This records the coarse-grained progression in levels.
 *
 * The timestamps are primitive `Long` with 0 meaning "not set", and the columns are `NOT NULL`
 * to match. A separate `LevelProgressionEntityDefinition` used to declare them nullable so the
 * schema could stay unchanged.
 */
@Entity(tableName = "level_progression")
class LevelProgression {
    /**
     * The unique ID.
     */
    @PrimaryKey var id: Long = 0L

    /**
     * Timestamp when this level was abandoned (because of a reset), or 0L if not abandoned.
     */
    var abandonedAt: Long = 0L

    /**
     * Timestamp when this level was completed (all subjects burned), or 0L if not completed.
     */
    var completedAt: Long = 0L

    /**
     * Timestamp when this record was created.
     */
    var createdAt: Long = 0L

    /**
     * Timestamp when this level was passed (all subjects passed), or 0L if not passed.
     */
    var passedAt: Long = 0L

    /**
     * Timestamp when this level was started, or null if not started.
     */
    var startedAt: Long = 0L

    /**
     * Timestamp when this level was unlocked, or 0L if not unlocked.
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
