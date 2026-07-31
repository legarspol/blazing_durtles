package com.smouldering_durtles.wk.db.dao

import androidx.room.Dao
import androidx.room.Query
import com.smouldering_durtles.wk.db.model.SubjectPronunciationAudio
import com.smouldering_durtles.wk.model.LevelProgressItem
import com.smouldering_durtles.wk.model.SrsBreakDownItem
import com.smouldering_durtles.wk.model.SubjectReferenceData

/**
 * DAO for subjects.
 */
@Dao
abstract class SubjectViewsDao {
    /**
     * Room-generated method: get a list of subject IDs for which the assignment has been locally patched
     * but not yet updated from remote. This is normally not necessary but is a last-resort option to
     * recover from sync problems.
     *
     * @return the list of subject IDs, capped at 100
     */
    @Query("SELECT id FROM subject WHERE assignmentPatched LIMIT 100")
    abstract fun getPatchedAssignments(): List<Long>

    /**
     * Room-generated method: get a list of subject IDs for which the statistics have been locally patched
     * but not yet updated from remote. This is normally not necessary but is a last-resort option to
     * recover from sync problems.
     *
     * @return the list of subject IDs, capped at 100
     */
    @Query("SELECT id FROM subject WHERE statisticPatched LIMIT 100")
    abstract fun getPatchedReviewStatistics(): List<Long>

    /**
     * Room-generated method: get a list of subject IDs for which the study materials have been locally patched
     * but not yet updated from remote. This is normally not necessary but is a last-resort option to
     * recover from sync problems.
     *
     * @return the list of subject IDs, capped at 100
     */
    @Query("SELECT id FROM subject WHERE studyMaterialPatched LIMIT 100")
    abstract fun getPatchedStudyMaterials(): List<Long>

    /**
     * Room-generated method: get summary records describing the SRS stages and the number of subjects in each stage.
     *
     * @return the list of overview items
     */
    @Query(
        "SELECT srsSystemId AS systemId, srsStage AS stageId, COUNT(id) AS count FROM subject WHERE " +
            "hiddenAt = 0 AND object IS NOT NULL " +
            "GROUP BY srsSystemId, srsStage"
    )
    abstract fun getSrsBreakDownItems(): List<SrsBreakDownItem>

    /**
     * Room-generated method: get summary records describing the number of subjects per level/type pair.
     *
     * @return the list of overview items
     */
    @Query(
        "SELECT level, object AS type, COUNT(id) AS count FROM subject" +
            " WHERE subject.hiddenAt = 0 AND object IS NOT NULL" +
            " GROUP BY level, object"
    )
    abstract fun getLevelProgressTotalItems(): List<LevelProgressItem>

    /**
     * Room-generated method: get summary records describing the number of passed subjects per level/type pair.
     *
     * @return the list of overview items
     */
    @Query(
        "SELECT level, object AS type, COUNT(id) AS count FROM subject" +
            " WHERE subject.hiddenAt = 0 AND object IS NOT NULL" +
            " AND passedAt != 0" +
            " GROUP BY level, object"
    )
    abstract fun getLevelProgressPassedItems(): List<LevelProgressItem>

    /**
     * Room-generated method: get summary records describing the number of locked subjects per level/type pair.
     *
     * @return the list of overview items
     */
    @Query(
        "SELECT level, object AS type, COUNT(id) AS count FROM subject" +
            " WHERE subject.hiddenAt = 0 AND object IS NOT NULL" +
            " AND (unlockedAt = 0 OR unlockedAt IS NULL)" +
            " GROUP BY level, object"
    )
    abstract fun getLevelProgressLockedItems(): List<LevelProgressItem>

    /**
     * Room-generated method: get a list of all subject IDs in the database.
     *
     * @return the list
     */
    @Query("SELECT id FROM subject")
    protected abstract fun getAllSubjectIdsAsList(): List<Long>

    /**
     * Get a set of subject IDs for all subjects in the database.
     *
     * @return the set
     */
    fun getAllSubjectIds(): Set<Long> = HashSet(getAllSubjectIdsAsList())

    /**
     * Room-generated method: get the reference data for all subjects.
     *
     * @return the list of reference data
     */
    @Query("SELECT id, object AS type, characters, frequency, joyoGrade, jlptLevel, pitchInfo, strokeData FROM subject")
    abstract fun getReferenceData(): List<SubjectReferenceData>

    /**
     * Room-generated method: get the pronunciation audio for all subjects in a level.
     *
     * @param level the level
     * @return the list of audio records
     */
    @Query("SELECT id, level, pronunciationAudios FROM subject WHERE hiddenAt = 0 AND level = :level")
    abstract fun getAudioByLevel(level: Int): List<SubjectPronunciationAudio>
}
