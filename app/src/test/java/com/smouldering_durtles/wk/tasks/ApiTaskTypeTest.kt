package com.smouldering_durtles.wk.tasks

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * Characterization tests for the persisted task type registry.
 *
 * The keys in [ApiTaskType] are stored in the `task_definition` table by earlier versions of the
 * app and appear as literals in `TaskDefinitionDao`'s queries, so they are a wire format. These
 * tests pin them down: if one changes, rows queued by an installed build stop resolving and their
 * work is thrown away.
 */
class ApiTaskTypeTest {
    @Test
    fun `every key round trips through fromKey`() {
        for (type in ApiTaskType.entries) {
            assertSame(type, ApiTaskType.fromKey(type.key))
        }
    }

    @Test
    fun `keys are unique`() {
        val keys = ApiTaskType.entries.map(ApiTaskType::key)
        assertEquals(keys.size, keys.toSet().size)
    }

    @Test
    fun `fromKey rejects unknown keys`() {
        assertNull(ApiTaskType.fromKey(null))
        assertNull(ApiTaskType.fromKey(""))
        assertNull(ApiTaskType.fromKey("com.smouldering_durtles.wk.tasks.RemovedInSomeOldVersionTask"))
        // Not a task type, and previously the input to Class.forName - it must not resolve either.
        assertNull(ApiTaskType.fromKey("java.lang.String"))
    }

    /**
     * Each key currently equals the name of the class implementing it, because that is what the old
     * `Class.getCanonicalName()` converter wrote. Renaming or moving a task class must therefore not
     * silently change its key: if this test fails after a rename, keep the old string as the key and
     * replace the expectation here with that literal.
     */
    @Test
    fun `keys match the implementing class names`() {
        val expected = mapOf(
            ApiTaskType.LOAD_REFERENCE_DATA to LoadReferenceDataTask::class.java,
            ApiTaskType.SCAN_AUDIO_DOWNLOAD_STATUS to ScanAudioDownloadStatusTask::class.java,
            ApiTaskType.GET_USER to GetUserTask::class.java,
            ApiTaskType.GET_SRS_SYSTEMS to GetSrsSystemsTask::class.java,
            ApiTaskType.REPORT_SESSION_ITEM to ReportSessionItemTask::class.java,
            ApiTaskType.SUBMIT_STUDY_MATERIAL to SubmitStudyMaterialTask::class.java,
            ApiTaskType.GET_SUBJECT to GetSubjectTask::class.java,
            ApiTaskType.GET_SUBJECTS to GetSubjectsTask::class.java,
            ApiTaskType.GET_ASSIGNMENTS to GetAssignmentsTask::class.java,
            ApiTaskType.GET_PATCHED_ASSIGNMENTS to GetPatchedAssignmentsTask::class.java,
            ApiTaskType.GET_PATCHED_STUDY_MATERIALS to GetPatchedStudyMaterialsTask::class.java,
            ApiTaskType.GET_REVIEW_STATISTICS to GetReviewStatisticsTask::class.java,
            ApiTaskType.GET_PATCHED_REVIEW_STATISTICS to GetPatchedReviewStatisticsTask::class.java,
            ApiTaskType.GET_STUDY_MATERIALS to GetStudyMaterialsTask::class.java,
            ApiTaskType.GET_SUMMARY to GetSummaryTask::class.java,
            ApiTaskType.GET_LEVEL_PROGRESSION to GetLevelProgressionTask::class.java,
            ApiTaskType.DOWNLOAD_AUDIO to DownloadAudioTask::class.java,
            ApiTaskType.DOWNLOAD_PITCH_INFO to DownloadPitchInfoTask::class.java,
        )

        // A task type missing from the map above means a new task class was added without deciding
        // on its persisted key.
        assertEquals(ApiTaskType.entries.toSet(), expected.keys)
        for ((type, clas) in expected) {
            assertEquals(clas.name, type.key)
        }
    }

    /**
     * `TaskDefinitionDao` singles out the two download task types by hardcoded string literal in
     * its SQL (the API/audio/pitch-info counts driving the progress UI). Those literals have to
     * agree with the keys written to the column, and SQL can't reference the enum.
     */
    @Test
    fun `keys used as literals in dao queries are unchanged`() {
        assertEquals(
            "com.smouldering_durtles.wk.tasks.DownloadAudioTask",
            ApiTaskType.DOWNLOAD_AUDIO.key,
        )
        assertEquals(
            "com.smouldering_durtles.wk.tasks.DownloadPitchInfoTask",
            ApiTaskType.DOWNLOAD_PITCH_INFO.key,
        )
    }
}
