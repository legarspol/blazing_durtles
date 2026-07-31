package com.smouldering_durtles.wk.db.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for the level_progression table. See LogRecordEntityDefinition for an explanation of why this class exists.
 */
@Suppress("unused")
@Entity(tableName = "level_progression")
class LevelProgressionEntityDefinition {
    @PrimaryKey var id: Long = 0L
    var abandonedAt: Long? = null
    var completedAt: Long? = null
    var createdAt: Long? = null
    var passedAt: Long? = null
    var startedAt: Long? = null
    var unlockedAt: Long? = null
    var level: Int = 0
}
