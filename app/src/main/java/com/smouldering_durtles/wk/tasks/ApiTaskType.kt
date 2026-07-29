package com.smouldering_durtles.wk.tasks

import com.smouldering_durtles.wk.db.model.TaskDefinition

/**
 * The registry of every task type that can be persisted in the `task_definition` table.
 *
 * Each entry pairs the key stored in the `taskClass` column with a direct reference to the
 * constructor of the class implementing it. This replaces the old
 * `Class.forName(...).getConstructor(...).newInstance(...)` lookup, and does so for two reasons:
 *
 *  - **Nothing can be silently swallowed.** A key that no longer corresponds to a task type is
 *    simply a lookup miss ([fromKey] returns null), handled and reported at the call site, rather
 *    than a `ClassNotFoundException` collapsed to null deep inside a Room type converter.
 *  - **It survives minification.** A constructor reference is a hard, statically visible reference,
 *    so R8 cannot strip or rename these classes out from under the database. Reflection by
 *    persisted class name was invisible to R8, which would have made every queued task
 *    unresolvable in a minified build.
 *
 * [key] values are **frozen**: they are persisted in the database by earlier versions of the app,
 * and the same strings appear as literals in `TaskDefinitionDao`'s queries. They happen to match
 * the current class names, but they are independent of them — never derive a key from a class
 * object, or the stored value would follow a class rename (or an R8 rename) and orphan every
 * existing row. If a task class is ever renamed, keep its old key and leave it here.
 */
enum class ApiTaskType(val key: String, private val factory: (TaskDefinition) -> ApiTask) {
    LOAD_REFERENCE_DATA("com.smouldering_durtles.wk.tasks.LoadReferenceDataTask", ::LoadReferenceDataTask),
    SCAN_AUDIO_DOWNLOAD_STATUS("com.smouldering_durtles.wk.tasks.ScanAudioDownloadStatusTask", ::ScanAudioDownloadStatusTask),
    GET_USER("com.smouldering_durtles.wk.tasks.GetUserTask", ::GetUserTask),
    GET_SRS_SYSTEMS("com.smouldering_durtles.wk.tasks.GetSrsSystemsTask", ::GetSrsSystemsTask),
    REPORT_SESSION_ITEM("com.smouldering_durtles.wk.tasks.ReportSessionItemTask", ::ReportSessionItemTask),
    SUBMIT_STUDY_MATERIAL("com.smouldering_durtles.wk.tasks.SubmitStudyMaterialTask", ::SubmitStudyMaterialTask),
    GET_SUBJECT("com.smouldering_durtles.wk.tasks.GetSubjectTask", ::GetSubjectTask),
    GET_SUBJECTS("com.smouldering_durtles.wk.tasks.GetSubjectsTask", ::GetSubjectsTask),
    GET_ASSIGNMENTS("com.smouldering_durtles.wk.tasks.GetAssignmentsTask", ::GetAssignmentsTask),
    GET_PATCHED_ASSIGNMENTS("com.smouldering_durtles.wk.tasks.GetPatchedAssignmentsTask", ::GetPatchedAssignmentsTask),
    GET_PATCHED_STUDY_MATERIALS("com.smouldering_durtles.wk.tasks.GetPatchedStudyMaterialsTask", ::GetPatchedStudyMaterialsTask),
    GET_REVIEW_STATISTICS("com.smouldering_durtles.wk.tasks.GetReviewStatisticsTask", ::GetReviewStatisticsTask),
    GET_PATCHED_REVIEW_STATISTICS("com.smouldering_durtles.wk.tasks.GetPatchedReviewStatisticsTask", ::GetPatchedReviewStatisticsTask),
    GET_STUDY_MATERIALS("com.smouldering_durtles.wk.tasks.GetStudyMaterialsTask", ::GetStudyMaterialsTask),
    GET_SUMMARY("com.smouldering_durtles.wk.tasks.GetSummaryTask", ::GetSummaryTask),
    GET_LEVEL_PROGRESSION("com.smouldering_durtles.wk.tasks.GetLevelProgressionTask", ::GetLevelProgressionTask),
    DOWNLOAD_AUDIO("com.smouldering_durtles.wk.tasks.DownloadAudioTask", ::DownloadAudioTask),
    DOWNLOAD_PITCH_INFO("com.smouldering_durtles.wk.tasks.DownloadPitchInfoTask", ::DownloadPitchInfoTask),
    ;

    /**
     * Create an instance of the task implementing this type.
     *
     * @param taskDefinition the database record the task will work from
     * @return the task instance
     */
    fun create(taskDefinition: TaskDefinition): ApiTask = factory(taskDefinition)

    companion object {
        private val byKey = entries.associateBy(ApiTaskType::key)

        /**
         * Look up the task type for a key as stored in the database.
         *
         * @param key the stored key, may be null since the column is nullable
         * @return the task type, or null if the key is null or no longer known
         */
        @JvmStatic
        fun fromKey(key: String?): ApiTaskType? = if (key == null) null else byKey[key]
    }
}
