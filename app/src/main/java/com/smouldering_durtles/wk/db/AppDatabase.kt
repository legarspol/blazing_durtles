package com.smouldering_durtles.wk.db

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.smouldering_durtles.wk.Constants.DAY
import com.smouldering_durtles.wk.Constants.HOUR
import com.smouldering_durtles.wk.GlobalSettings
import com.smouldering_durtles.wk.WkApplication
import com.smouldering_durtles.wk.db.dao.AudioDownloadStatusDao
import com.smouldering_durtles.wk.db.dao.LevelProgressionDao
import com.smouldering_durtles.wk.db.dao.LogRecordDao
import com.smouldering_durtles.wk.db.dao.PropertiesDao
import com.smouldering_durtles.wk.db.dao.SearchPresetDao
import com.smouldering_durtles.wk.db.dao.SessionItemDao
import com.smouldering_durtles.wk.db.dao.SrsSystemDao
import com.smouldering_durtles.wk.db.dao.SubjectAggregatesDao
import com.smouldering_durtles.wk.db.dao.SubjectCollectionsDao
import com.smouldering_durtles.wk.db.dao.SubjectDao
import com.smouldering_durtles.wk.db.dao.SubjectSyncDao
import com.smouldering_durtles.wk.db.dao.SubjectViewsDao
import com.smouldering_durtles.wk.db.dao.TaskDefinitionDao
import com.smouldering_durtles.wk.db.model.AudioDownloadStatus
import com.smouldering_durtles.wk.db.model.LevelProgressionEntityDefinition
import com.smouldering_durtles.wk.db.model.LogRecordEntityDefinition
import com.smouldering_durtles.wk.db.model.PronunciationAudioOwner
import com.smouldering_durtles.wk.db.model.Property
import com.smouldering_durtles.wk.db.model.SearchPreset
import com.smouldering_durtles.wk.db.model.SessionItem
import com.smouldering_durtles.wk.db.model.SrsSystemDefinition
import com.smouldering_durtles.wk.db.model.SubjectEntityDefinition
import com.smouldering_durtles.wk.db.model.TaskDefinition
import com.smouldering_durtles.wk.enums.SessionType
import com.smouldering_durtles.wk.jobs.TickJob
import com.smouldering_durtles.wk.model.Session
import com.smouldering_durtles.wk.services.JobRunnerService
import com.smouldering_durtles.wk.tasks.ApiTaskType
import com.smouldering_durtles.wk.tasks.DownloadAudioTask
import com.smouldering_durtles.wk.tasks.DownloadPitchInfoTask
import com.smouldering_durtles.wk.tasks.GetAssignmentsTask
import com.smouldering_durtles.wk.tasks.GetLevelProgressionTask
import com.smouldering_durtles.wk.tasks.GetPatchedAssignmentsTask
import com.smouldering_durtles.wk.tasks.GetPatchedReviewStatisticsTask
import com.smouldering_durtles.wk.tasks.GetPatchedStudyMaterialsTask
import com.smouldering_durtles.wk.tasks.GetReviewStatisticsTask
import com.smouldering_durtles.wk.tasks.GetSrsSystemsTask
import com.smouldering_durtles.wk.tasks.GetStudyMaterialsTask
import com.smouldering_durtles.wk.tasks.GetSubjectTask
import com.smouldering_durtles.wk.tasks.GetSubjectsTask
import com.smouldering_durtles.wk.tasks.GetSummaryTask
import com.smouldering_durtles.wk.tasks.GetUserTask
import com.smouldering_durtles.wk.tasks.LoadReferenceDataTask
import com.smouldering_durtles.wk.tasks.ReportSessionItemTask
import com.smouldering_durtles.wk.tasks.ScanAudioDownloadStatusTask
import com.smouldering_durtles.wk.tasks.SubmitStudyMaterialTask
import com.smouldering_durtles.wk.util.ObjectSupport.join
import java.util.Locale

/**
 * The Room-wrapped SQLite database.
 */
@Database(
    entities = [
        TaskDefinition::class,
        Property::class,
        SubjectEntityDefinition::class,
        SrsSystemDefinition::class,
        LevelProgressionEntityDefinition::class,
        SessionItem::class,
        LogRecordEntityDefinition::class,
        AudioDownloadStatus::class,
        SearchPreset::class
    ],
    version = 68
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    /**
     * Are there any API tasks pending?.
     *
     * @return true if there are
     */
    fun hasPendingApiTasks(): Boolean = taskDefinitionDao().getCount() > 0

    /**
     * Add a task for fetching the user endpoint if it doesn't exist already.
     */
    fun assertGetUserTask() {
        val count = taskDefinitionDao().getCountByType(ApiTaskType.GET_USER.key)
        if (count == 0) {
            val taskDefinition = TaskDefinition()
            taskDefinition.taskClass = ApiTaskType.GET_USER.key
            taskDefinition.priority = GetUserTask.PRIORITY
            taskDefinition.data = ""
            taskDefinitionDao().insertTaskDefinition(taskDefinition)
        }
    }

    /**
     * Add a task for fetching the subjects endpoint if it doesn't exist already.
     */
    fun assertGetSubjectsTask() {
        val count = taskDefinitionDao().getCountByType(ApiTaskType.GET_SUBJECTS.key)
        if (count == 0) {
            val taskDefinition = TaskDefinition()
            taskDefinition.taskClass = ApiTaskType.GET_SUBJECTS.key
            taskDefinition.priority = GetSubjectsTask.PRIORITY
            taskDefinition.data = ""
            taskDefinitionDao().insertTaskDefinition(taskDefinition)
        }
    }

    /**
     * Adds a task to fetch the latest information for a specific subject.
     * @param subjectId The id for the subject to get.
     */
    fun assertGetSubjectTask(subjectId: String) {
        val count = taskDefinitionDao().getCountByType(ApiTaskType.GET_SUBJECT.key)
        if (count == 0) {
            val taskDefinition = TaskDefinition()
            taskDefinition.taskClass = ApiTaskType.GET_SUBJECT.key
            taskDefinition.priority = GetSubjectTask.PRIORITY
            taskDefinition.data = subjectId
            taskDefinitionDao().insertTaskDefinition(taskDefinition)
        }
    }

    /**
     * Add a task for fetching the assignments endpoint if it doesn't exist already.
     */
    fun assertGetAssignmentsTask() {
        val count = taskDefinitionDao().getCountByType(ApiTaskType.GET_ASSIGNMENTS.key)
        if (count == 0) {
            val taskDefinition = TaskDefinition()
            taskDefinition.taskClass = ApiTaskType.GET_ASSIGNMENTS.key
            taskDefinition.priority = GetAssignmentsTask.PRIORITY
            taskDefinition.data = ""
            taskDefinitionDao().insertTaskDefinition(taskDefinition)
        }
    }

    /**
     * Add a task for fetching the assignments for a set of subjects if it doesn't exist already.
     *
     * @param subjectIds the subject IDs to fetch for
     */
    fun assertGetPatchedAssignmentsTask(subjectIds: Iterable<Long>) {
        val count = taskDefinitionDao().getCountByType(ApiTaskType.GET_PATCHED_ASSIGNMENTS.key)
        if (count == 0) {
            val taskDefinition = TaskDefinition()
            taskDefinition.taskClass = ApiTaskType.GET_PATCHED_ASSIGNMENTS.key
            taskDefinition.priority = GetPatchedAssignmentsTask.PRIORITY
            taskDefinition.data = join(",", "", "", subjectIds)
            taskDefinitionDao().insertTaskDefinition(taskDefinition)
        }
    }

    /**
     * Add a task for fetching the review statistics endpoint if it doesn't exist already.
     */
    fun assertGetReviewStatisticsTask() {
        val count = taskDefinitionDao().getCountByType(ApiTaskType.GET_REVIEW_STATISTICS.key)
        if (count == 0) {
            val taskDefinition = TaskDefinition()
            taskDefinition.taskClass = ApiTaskType.GET_REVIEW_STATISTICS.key
            taskDefinition.priority = GetReviewStatisticsTask.PRIORITY
            taskDefinition.data = ""
            taskDefinitionDao().insertTaskDefinition(taskDefinition)
        }
    }

    /**
     * Add a task for fetching the review statistics for a set of subjects if it doesn't exist already.
     *
     * @param subjectIds the subject IDs to fetch for
     */
    fun assertGetPatchedReviewStatisticsTask(subjectIds: Iterable<Long>) {
        val count = taskDefinitionDao().getCountByType(ApiTaskType.GET_PATCHED_REVIEW_STATISTICS.key)
        if (count == 0) {
            val taskDefinition = TaskDefinition()
            taskDefinition.taskClass = ApiTaskType.GET_PATCHED_REVIEW_STATISTICS.key
            taskDefinition.priority = GetPatchedReviewStatisticsTask.PRIORITY
            taskDefinition.data = join(",", "", "", subjectIds)
            taskDefinitionDao().insertTaskDefinition(taskDefinition)
        }
    }

    /**
     * Add a task for fetching the study materials endpoint if it doesn't exist already.
     */
    fun assertGetStudyMaterialsTask() {
        val count = taskDefinitionDao().getCountByType(ApiTaskType.GET_STUDY_MATERIALS.key)
        if (count == 0) {
            val taskDefinition = TaskDefinition()
            taskDefinition.taskClass = ApiTaskType.GET_STUDY_MATERIALS.key
            taskDefinition.priority = GetStudyMaterialsTask.PRIORITY
            taskDefinition.data = ""
            taskDefinitionDao().insertTaskDefinition(taskDefinition)
        }
    }

    /**
     * Add a task for fetching the study materials for a set of subjects if it doesn't exist already.
     *
     * @param subjectIds the subject IDs to fetch for
     */
    fun assertGetPatchedStudyMaterialsTask(subjectIds: Iterable<Long>) {
        val count = taskDefinitionDao().getCountByType(ApiTaskType.GET_PATCHED_STUDY_MATERIALS.key)
        if (count == 0) {
            val taskDefinition = TaskDefinition()
            taskDefinition.taskClass = ApiTaskType.GET_PATCHED_STUDY_MATERIALS.key
            taskDefinition.priority = GetPatchedStudyMaterialsTask.PRIORITY
            taskDefinition.data = join(",", "", "", subjectIds)
            taskDefinitionDao().insertTaskDefinition(taskDefinition)
        }
    }

    /**
     * Add a task for fetching the SRS systems endpoint if it doesn't exist already.
     */
    fun assertGetSrsSystemsTask() {
        val count = taskDefinitionDao().getCountByType(ApiTaskType.GET_SRS_SYSTEMS.key)
        if (count == 0) {
            val taskDefinition = TaskDefinition()
            taskDefinition.taskClass = ApiTaskType.GET_SRS_SYSTEMS.key
            taskDefinition.priority = GetSrsSystemsTask.PRIORITY
            taskDefinition.data = ""
            taskDefinitionDao().insertTaskDefinition(taskDefinition)
        }
    }

    /**
     * Add a task for fetching the summary endpoint if it doesn't exist already.
     */
    fun assertGetSummaryTask() {
        val count = taskDefinitionDao().getCountByType(ApiTaskType.GET_SUMMARY.key)
        if (count == 0) {
            val taskDefinition = TaskDefinition()
            taskDefinition.taskClass = ApiTaskType.GET_SUMMARY.key
            taskDefinition.priority = GetSummaryTask.PRIORITY
            taskDefinition.data = ""
            taskDefinitionDao().insertTaskDefinition(taskDefinition)
        }
    }

    /**
     * Add a task for fetching the level progression endpoint if it doesn't exist already.
     */
    fun assertGetLevelProgressionTask() {
        val count = taskDefinitionDao().getCountByType(ApiTaskType.GET_LEVEL_PROGRESSION.key)
        if (count == 0) {
            val taskDefinition = TaskDefinition()
            taskDefinition.taskClass = ApiTaskType.GET_LEVEL_PROGRESSION.key
            taskDefinition.priority = GetLevelProgressionTask.PRIORITY
            taskDefinition.data = ""
            taskDefinitionDao().insertTaskDefinition(taskDefinition)
        }
    }

    /**
     * Add a task for reporting the status of a finished session item.
     *
     * @param timeStamp the timetamp the item was finished
     * @param subjectId the subject ID this task applies to
     * @param assignmentId the assignment ID for this item, or 0 if not known
     * @param type the type of the session this item is from
     * @param meaningIncorrect number of incorrect meaning answers
     * @param readingIncorrect number of incorrect reading answers
     * @param justPassed true if the subject just passed with this update (went to Guru I for the first time)
     */
    fun assertReportSessionItemTask(
        timeStamp: Long,
        subjectId: Long,
        assignmentId: Long,
        type: SessionType,
        meaningIncorrect: Int,
        readingIncorrect: Int,
        justPassed: Boolean
    ) {
        val taskDefinition = TaskDefinition()
        taskDefinition.taskClass = ApiTaskType.REPORT_SESSION_ITEM.key
        taskDefinition.priority = ReportSessionItemTask.PRIORITY
        taskDefinition.data = String.format(
            Locale.ROOT, "%d %d %d %s %d %d %s", timeStamp,
            subjectId, assignmentId, type, meaningIncorrect, readingIncorrect, justPassed
        )
        taskDefinitionDao().insertTaskDefinition(taskDefinition)
    }

    /**
     * Add a task for downloading audio for a subject.
     *
     * @param subject the subject to download for
     */
    fun assertDownloadAudioTask(subject: PronunciationAudioOwner) {
        val taskDefinition = TaskDefinition()
        taskDefinition.taskClass = ApiTaskType.DOWNLOAD_AUDIO.key
        taskDefinition.priority = DownloadAudioTask.PRIORITY
        taskDefinition.data = subject.id.toString()
        taskDefinitionDao().insertTaskDefinition(taskDefinition)
    }

    /**
     * Add a task for downloading pitch info for a subject.
     *
     * @param subjectId the subject to download for
     */
    fun assertDownloadPitchInfoTask(subjectId: Long) {
        val taskDefinition = TaskDefinition()
        taskDefinition.taskClass = ApiTaskType.DOWNLOAD_PITCH_INFO.key
        taskDefinition.priority = DownloadPitchInfoTask.PRIORITY
        taskDefinition.data = subjectId.toString()
        taskDefinitionDao().insertTaskDefinition(taskDefinition)
    }

    /**
     * Add a task for saving/updating study materials.
     *
     * @param data the prepared data string for the task
     */
    fun assertSubmitStudyMaterialTask(data: String) {
        val taskDefinition = TaskDefinition()
        taskDefinition.taskClass = ApiTaskType.SUBMIT_STUDY_MATERIAL.key
        taskDefinition.priority = SubmitStudyMaterialTask.PRIORITY
        taskDefinition.data = data
        taskDefinitionDao().insertTaskDefinition(taskDefinition)
    }

    /**
     * Add a task for loading reference data for all subjects in one go.
     */
    fun loadReferenceData() {
        val count = taskDefinitionDao().getCountByType(ApiTaskType.LOAD_REFERENCE_DATA.key)
        if (count == 0) {
            val taskDefinition = TaskDefinition()
            taskDefinition.taskClass = ApiTaskType.LOAD_REFERENCE_DATA.key
            taskDefinition.priority = LoadReferenceDataTask.PRIORITY
            taskDefinition.data = ""
            taskDefinitionDao().insertTaskDefinition(taskDefinition)
        }
    }

    /**
     * Add a task for scanning audio download status for all subjects in one go.
     */
    fun assertScanAudioDownloadStatusTask() {
        val count = taskDefinitionDao().getCountByType(ApiTaskType.SCAN_AUDIO_DOWNLOAD_STATUS.key)
        if (count == 0) {
            val taskDefinition = TaskDefinition()
            taskDefinition.taskClass = ApiTaskType.SCAN_AUDIO_DOWNLOAD_STATUS.key
            taskDefinition.priority = ScanAudioDownloadStatusTask.PRIORITY
            taskDefinition.data = ""
            taskDefinitionDao().insertTaskDefinition(taskDefinition)
        }
    }

    /**
     * Make sure there are tasks to update all of the core API models.
     */
    fun assertRefreshForAllModels() {
        assertGetUserTask()
        assertGetAssignmentsTask()
        assertGetReviewStatisticsTask()
        assertGetStudyMaterialsTask()
        assertGetSummaryTask()
        val lastSubjectSyncSuccessDate = propertiesDao().getLastSubjectSyncSuccessDate(0)
        if (lastSubjectSyncSuccessDate == 0L ||
            System.currentTimeMillis() - lastSubjectSyncSuccessDate > HOUR
        ) {
            assertGetSubjectsTask()
        }
        val lastGetSrsSystemsSuccess = propertiesDao().getLastSrsSystemSyncSuccessDate()
        if (lastGetSrsSystemsSuccess == 0L ||
            System.currentTimeMillis() - lastGetSrsSystemsSuccess > DAY
        ) {
            assertGetSrsSystemsTask()
        }
        JobRunnerService.schedule(TickJob::class.java, "")
    }

    /**
     * Clear all API data out of the database.
     */
    fun resetDatabase() {
        propertiesDao().setApiInError(false)
        propertiesDao().setApiKeyRejected(false)
        propertiesDao().setLastApiSuccessDate(0)
        propertiesDao().setLastUserSyncSuccessDate(0)
        propertiesDao().setLastSubjectSyncSuccessDate(0)
        propertiesDao().setLastAssignmentSyncSuccessDate(0)
        propertiesDao().setLastReviewStatisticSyncSuccessDate(0)
        propertiesDao().setLastStudyMaterialSyncSuccessDate(0)
        propertiesDao().setLastSrsSystemSyncSuccessDate(0)
        propertiesDao().setLastLevelProgressionSyncSuccessDate(0)
        propertiesDao().setLastSummarySyncSuccessDate(0)
        propertiesDao().setSessionType(SessionType.NONE)
        propertiesDao().setSessionOnkun(false)
        Session.getInstance().reset()
        taskDefinitionDao().deleteAll()
        subjectDao().deleteAll()
        srsSystemDao().deleteAll()
        sessionItemDao().deleteAll()
        levelProgressionDao().deleteAll()
        assertGetSubjectsTask()
        assertRefreshForAllModels()
        GlobalSettings.setFirstTimeSetup(0)
    }

    /**
     * Get the DAO instance for properties.
     *
     * @return the DAO
     */
    abstract fun propertiesDao(): PropertiesDao

    /**
     * Get the DAO instance for task definitions.
     *
     * @return the DAO
     */
    abstract fun taskDefinitionDao(): TaskDefinitionDao

    /**
     * Get the DAO instance for subjects.
     *
     * @return the DAO
     */
    abstract fun subjectDao(): SubjectDao

    /**
     * Get the DAO instance for fetching various collections of subjects.
     *
     * @return the DAO
     */
    abstract fun subjectCollectionsDao(): SubjectCollectionsDao

    /**
     * Get the DAO instance for fetching various aggregates of subjects.
     *
     * @return the DAO
     */
    abstract fun subjectAggregatesDao(): SubjectAggregatesDao

    /**
     * Get the DAO instance for fetching various subset views of subjects.
     *
     * @return the DAO
     */
    abstract fun subjectViewsDao(): SubjectViewsDao

    /**
     * Get the DAO instance for sync actions on subjects.
     *
     * @return the DAO
     */
    abstract fun subjectSyncDao(): SubjectSyncDao

    /**
     * Get the DAO instance for SRS systems.
     *
     * @return the DAO
     */
    abstract fun srsSystemDao(): SrsSystemDao

    /**
     * Get the DAO instance for level progression records.
     *
     * @return the DAO
     */
    abstract fun levelProgressionDao(): LevelProgressionDao

    /**
     * Get the DAO instance for session items.
     *
     * @return the DAO
     */
    abstract fun sessionItemDao(): SessionItemDao

    /**
     * Get the DAO instance for log records.
     *
     * @return the DAO
     */
    abstract fun logRecordDao(): LogRecordDao

    /**
     * Get the DAO instance for audio download status.
     *
     * @return the DAO
     */
    abstract fun audioDownloadStatusDao(): AudioDownloadStatusDao

    /**
     * Get the DAO instance for search presets.
     *
     * @return the DAO
     */
    abstract fun searchPresetDao(): SearchPresetDao

    companion object {
        /**
         * The internal name of the database.
         */
        private const val DATABASE_NAME = "wanikani"

        /**
         * The singleton instance.
         */
        private var instance: AppDatabase? = null

        /**
         * Get the singleton instance.
         *
         * @return the instance
         */
        @JvmStatic
        fun getInstance(): AppDatabase {
            var db = instance
            if (db == null) {
                db = Room.databaseBuilder(WkApplication.getInstance(), AppDatabase::class.java, DATABASE_NAME)
                    .fallbackToDestructiveMigration()
                    .build()
                instance = db
            }
            return db
        }
    }
}
