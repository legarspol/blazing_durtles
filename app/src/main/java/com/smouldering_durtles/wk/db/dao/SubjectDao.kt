package com.smouldering_durtles.wk.db.dao

import androidx.room.Dao
import androidx.room.Query
import com.smouldering_durtles.wk.db.model.Subject
import com.smouldering_durtles.wk.db.model.SubjectEntity
import com.smouldering_durtles.wk.livedata.SubjectChangeWatcher

/**
 * DAO for subjects.
 */
@Dao
abstract class SubjectDao {
    /**
     * Room-generated method: delete all subjects.
     */
    @Query("DELETE FROM Subject")
    abstract fun deleteAll()

    /**
     * Room-generated method: get a single kanji subject by the characters column.
     *
     * @param characters the character to look for
     * @return the subject or null if not found
     */
    @Query("SELECT * FROM subject WHERE object = 'kanji' AND hiddenAt = 0 AND characters = :characters LIMIT 1")
    protected abstract fun getKanjiByCharactersHelper(characters: String): SubjectEntity?

    /**
     * Get a single kanji subject by the characters column.
     *
     * @param characters the character to look for
     * @return the subject or null if not found
     */
    fun getKanjiByCharacters(characters: String): Subject? {
        val entity = getKanjiByCharactersHelper(characters)
        return if (entity == null) null else Subject(entity)
    }

    /**
     * Room-generated method: get a single subject by ID.
     *
     * @param id the subject's ID
     * @return the subject or null if not found
     */
    @Query("SELECT * FROM subject WHERE id = :id")
    protected abstract fun getByIdHelper(id: Long): SubjectEntity?

    /**
     * Get a single subject by ID.
     *
     * @param id the subject's ID
     * @return the subject or null if not found
     */
    fun getById(id: Long): Subject? {
        val entity = getByIdHelper(id)
        return if (entity == null) null else Subject(entity)
    }

    /**
     * Room-generated methiod: update the reference data for a subject.
     *
     * @param id the subject ID
     * @param frequency the frequency
     * @param joyoGrade the Joyo grade
     * @param jlptLevel the JLPT level
     * @param pitchInfo the pitch info, encoded as a JSON string
     * @param strokeData the stroke data, encoded as a JSON string
     */
    @Query(
        "UPDATE subject SET frequency = :frequency, joyoGrade = :joyoGrade, jlptLevel = :jlptLevel, pitchInfo = :pitchInfo," +
            " strokeData = :strokeData WHERE id = :id"
    )
    abstract fun updateReferenceData(
        id: Long,
        frequency: Int,
        joyoGrade: Int,
        jlptLevel: Int,
        pitchInfo: String?,
        strokeData: String?
    )

    /**
     * Room-generated method: update the last incorrect answer timestamp.
     * Only update if the new value is later than the previous value.
     *
     * @param id the subject ID
     * @param lastIncorrectAnswer the new timestamp
     */
    @Query(
        "UPDATE subject SET lastIncorrectAnswer = :lastIncorrectAnswer " +
            "WHERE id = :id AND lastIncorrectAnswer < :lastIncorrectAnswer"
    )
    abstract fun updateLastIncorrectAnswer(id: Long, lastIncorrectAnswer: Long)

    /**
     * Room-generated method: update the star rating.
     *
     * @param id the subject ID
     * @param numStars the new rating
     */
    @Query("UPDATE subject SET numStars = :numStars WHERE id = :id")
    protected abstract fun updateStarsHelper(id: Long, numStars: Int)

    /**
     * Update the star rating.
     *
     * @param id the subject ID
     * @param numStars the new rating
     */
    fun updateStars(id: Long, numStars: Int) {
        updateStarsHelper(id, numStars)
        SubjectChangeWatcher.getInstance().reportChange(id)
    }

    /**
     * Room-generated method: clear the statisticPatched flag from a collection of subjects.
     *
     * @param subjectIds the subject IDs
     */
    @Query("UPDATE subject SET statisticPatched = 0 WHERE statisticPatched AND id in (:subjectIds)")
    abstract fun resolvePatchedReviewStatistics(subjectIds: Collection<Long>)

    /**
     * Room-generated method: clear the assignmentPatched flag from a collection of subjects.
     *
     * @param subjectIds the subject IDs
     */
    @Query("UPDATE subject SET assignmentPatched = 0 WHERE assignmentPatched AND id in (:subjectIds)")
    abstract fun resolvePatchedAssignments(subjectIds: Collection<Long>)

    /**
     * Room-generated method: clear the studyMaterialPatched flag from a collection of subjects.
     *
     * @param subjectIds the subject IDs
     */
    @Query("UPDATE subject SET studyMaterialPatched = 0 WHERE studyMaterialPatched AND id in (:subjectIds)")
    abstract fun resolvePatchedStudyMaterials(subjectIds: Collection<Long>)
}
