package com.smouldering_durtles.wk.db.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for the task_definition table. These are records of background tasks to run, that involve network interaction.
 */
@Entity(tableName = "task_definition")
class TaskDefinition {
    /**
     * The unique ID.
     */
    @PrimaryKey(autoGenerate = true) var id: Int = 0

    /**
     * The key identifying this task's type, see ApiTaskType.
     */
    var taskClass: String? = null

    /**
     * The priority to execute this task with, lower priority is run earlier.
     */
    var priority: Int = 0

    /**
     * The parameters for this task, encoded as a string in a class-specific format.
     */
    var data: String? = null
}
