package com.smouldering_durtles.wk.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.smouldering_durtles.wk.db.model.TaskDefinition
import com.smouldering_durtles.wk.model.TaskCounts

/**
 * DAO for task definitions.
 */
@Dao
abstract class TaskDefinitionDao {
    /**
     * Room-generated method: delete all records.
     */
    @Query("DELETE FROM task_definition")
    abstract fun deleteAll()

    /**
     * Room-generated method: delete all audio download tasks.
     */
    @Query("DELETE FROM task_definition WHERE taskClass = 'com.smouldering_durtles.wk.tasks.DownloadAudioTask'")
    abstract fun deleteAudioDownloads()

    /**
     * Room-generated method: get the total number of tasks.
     *
     * @return the number
     */
    @Query("SELECT COUNT(*) FROM task_definition")
    abstract fun getCount(): Int

    /**
     * Room-generated method: get the total number of tasks excluding audio download tasks.
     *
     * @return the number
     */
    @Query(
        "SELECT COUNT(*) FROM task_definition WHERE taskClass!='com.smouldering_durtles.wk.tasks.DownloadAudioTask' " +
            "AND taskClass!='com.smouldering_durtles.wk.tasks.DownloadPitchInfoTask'"
    )
    abstract fun getApiCount(): Int

    /**
     * Room-generated method: get a LiveData instance containing the counts of tasks (API and audio separately).
     *
     * @return the LiveData instance
     */
    @Query(
        "SELECT apiCount, audioCount, pitchInfoCount FROM " +
            "(SELECT COUNT(*) AS apiCount FROM task_definition " +
            "WHERE taskClass!='com.smouldering_durtles.wk.tasks.DownloadAudioTask' AND taskClass!='com.smouldering_durtles.wk.tasks.DownloadPitchInfoTask'), " +
            "(SELECT count(*) AS audioCount FROM task_definition " +
            "WHERE taskClass='com.smouldering_durtles.wk.tasks.DownloadAudioTask'), " +
            "(SELECT count(*) AS pitchInfoCount FROM task_definition " +
            "WHERE taskClass='com.smouldering_durtles.wk.tasks.DownloadPitchInfoTask');"
    )
    abstract fun getLiveCounts(): LiveData<TaskCounts>

    /**
     * Room-generated method: get the next task to execute.
     *
     * @return the task or null if none are pending
     */
    @Query("SELECT * FROM task_definition ORDER BY priority, id LIMIT 1")
    abstract fun getNextTaskDefinition(): TaskDefinition?

    /**
     * Room-generated method: get a LiveData instance containing the priority of the next API task
     * to run, or -1 when none are queued.
     *
     * Tasks run strictly in priority order, one at a time, so this single number says exactly how
     * far a sync has got: everything below it has finished, and the task at it is the one running.
     * -1 rather than null keeps the LiveData a plain Int for the Java callers.
     *
     * @return the LiveData instance
     */
    @Query(
        "SELECT IFNULL(MIN(priority), -1) FROM task_definition " +
            "WHERE taskClass!='com.smouldering_durtles.wk.tasks.DownloadAudioTask' " +
            "AND taskClass!='com.smouldering_durtles.wk.tasks.DownloadPitchInfoTask'"
    )
    abstract fun getLiveNextApiPriority(): LiveData<Int>

    /**
     * Room-generated method: get the number of tasks for a certain task type.
     *
     * @param taskClass the task type key to look for, see ApiTaskType
     * @return the number
     */
    @Query("SELECT COUNT(*) FROM task_definition WHERE taskClass = :taskClass")
    abstract fun getCountByType(taskClass: String): Int

    /**
     * Room-generated method: insert a new task.
     *
     * @param taskDefinition the task to insert
     */
    @Insert
    abstract fun insertTaskDefinition(taskDefinition: TaskDefinition)

    /**
     * Room-generated method: delete a task.
     *
     * @param taskDefinition the task to delete
     */
    @Delete
    abstract fun deleteTaskDefinition(taskDefinition: TaskDefinition)
}
