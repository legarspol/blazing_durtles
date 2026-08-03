package com.smouldering_durtles.wk.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.smouldering_durtles.wk.db.model.Subject
import com.smouldering_durtles.wk.db.model.SubjectEntity
import com.smouldering_durtles.wk.enums.SubjectType
import java.util.Locale
import kotlin.math.min

/**
 * DAO for subjects.
 */
@Dao
abstract class SubjectCollectionsDao {
    /**
     * Room-generated method: get a list of all subjects unlocked after a cutoff date.
     *
     * @param cutoff the cutoff date
     * @return the list
     */
    @Query(
        "SELECT * FROM subject" +
            " WHERE hiddenAt = 0 AND object IS NOT NULL" +
            " AND unlockedAt != 0 AND unlockedAt >= :cutoff" +
            " ORDER BY unlockedAt DESC, level DESC, lessonPosition DESC, id DESC LIMIT 10"
    )
    abstract fun getRecentUnlocksHelper(cutoff: Long): List<SubjectEntity>

    /**
     * Get a list of all subjects unlocked after a cutoff date.
     *
     * @param cutoff the cutoff date
     * @return the list
     */
    fun getRecentUnlocks(cutoff: Long): List<Subject> = buildSubjectList(getRecentUnlocksHelper(cutoff))

    /**
     * Room-generated method: get a list of all subjects in the current session.
     *
     * @return the list
     */
    @Query(
        "SELECT * FROM subject" +
            " WHERE hiddenAt = 0 AND object IS NOT NULL" +
            " AND id IN (SELECT id FROM session_item)"
    )
    abstract fun getSessionSubjectsHelper(): List<SubjectEntity>

    /**
     * Get a list of all subjects in the current session.
     *
     * @return the list
     */
    fun getSessionSubjects(): Map<Long, Subject> {
        val result: MutableMap<Long, Subject> = HashMap()
        for (entity in getSessionSubjectsHelper()) {
            result[entity.id] = Subject(entity)
        }
        return result
    }

    /**
     * Get a list of non-passed subjects with a total score of less than 75%.
     *
     * @param filter the SRS stage filter
     * @return the list
     */
    fun getCriticalCondition(filter: String): List<Subject> {
        val sql = String.format(
            Locale.ROOT,
            "SELECT * FROM subject" +
                " WHERE hiddenAt = 0 AND object IS NOT NULL AND %s" +
                " AND percentageCorrect < 75 AND unlockedAt != 0 AND reviewStatisticId != 0" +
                " ORDER BY percentageCorrect, lessonPosition DESC, id DESC LIMIT 10",
            filter
        )

        return getSubjectsWithRawQuery(SimpleSQLiteQuery(sql))
    }

    /**
     * Get a list of all subjects burned after a cutoff date.
     *
     * @param filter the SRS stage filter
     * @param cutoff the cutoff date
     * @return the list
     */
    fun getBurnedItems(filter: String, cutoff: Long): List<Subject> {
        val sql = String.format(
            Locale.ROOT,
            "SELECT * FROM subject" +
                " WHERE hiddenAt = 0 AND %s AND object IS NOT NULL" +
                " AND burnedAt != 0 AND burnedAt >= ?" +
                " ORDER BY burnedAt DESC, level DESC, lessonPosition DESC, id DESC LIMIT 10",
            filter
        )

        return getSubjectsWithRawQuery(SimpleSQLiteQuery(sql, arrayOf<Any>(cutoff)))
    }

    /**
     * Room-generated method: get a list of all subjects available for lesson.
     *
     * @param maxLevel the maximum level available on the user's subscription
     * @return the list
     */
    @Query(
        "SELECT * FROM subject" +
            " WHERE hiddenAt = 0 AND object IS NOT NULL" +
            " AND level <= :maxLevel" +
            " AND (resurrectedAt != 0 OR burnedAt = 0)" +
            " AND unlockedAt != 0 AND startedAt = 0" +
            " ORDER BY level, lessonPosition, id"
    )
    protected abstract fun getAvailableLessonItemsHelper(maxLevel: Int): List<SubjectEntity>

    /**
     * Get a list of all subjects available for lesson.
     *
     * @param maxLevel the maximum level available on the user's subscription
     * @return the list
     */
    fun getAvailableLessonItems(maxLevel: Int): List<Subject> = buildSubjectList(getAvailableLessonItemsHelper(maxLevel))

    /**
     * Room-generated method: get a list of all subjects available for review, where the review
     * becomes/became available before the given cutoff date.
     *
     * @param maxLevel the maximum level available on the user's subscription
     * @param cutoff the cutoff date
     * @return the list
     */
    @Query(
        "SELECT * FROM subject" +
            " WHERE hiddenAt = 0 AND object IS NOT NULL" +
            " AND level <= :maxLevel" +
            " AND availableAt != 0 AND availableAt < :cutoff"
    )
    protected abstract fun getUpcomingReviewItemsHelper(maxLevel: Int, cutoff: Long): List<SubjectEntity>

    /**
     * Get a list of all subjects available for review, where the review
     * becomes/became available before the given cutoff date.
     *
     * @param maxLevel the maximum level available on the user's subscription
     * @param cutoff the cutoff date
     * @return the list
     */
    fun getUpcomingReviewItems(maxLevel: Int, cutoff: Long): List<Subject> =
        buildSubjectList(getUpcomingReviewItemsHelper(maxLevel, cutoff))

    /**
     * Room-generated method: get all kanji for a given level.
     *
     * @param level the level
     * @return the kanji subjects for this level
     */
    @Query(
        "SELECT * FROM subject" +
            " WHERE hiddenAt = 0 AND object = 'kanji'" +
            " AND level = :level" +
            " ORDER BY lessonPosition, id"
    )
    protected abstract fun getKanjiForLevelHelper(level: Int): List<SubjectEntity>

    /**
     * Get all kanji for a given level.
     *
     * @param level the level
     * @return the kanji subjects for this level
     */
    private fun getKanjiForLevel(level: Int): List<Subject> = buildSubjectList(getKanjiForLevelHelper(level))

    /**
     * Room-generated method: get the candidates for downloading pitch info data.
     *
     * @return the candidates
     */
    @Query(
        "SELECT * FROM subject" +
            " WHERE hiddenAt = 0 AND (object = 'vocabulary' OR object = 'kana_vocabulary')" +
            " AND (pitchInfo IS NULL OR pitchInfo = '' OR pitchInfo LIKE '@%')"
    )
    protected abstract fun getPitchInfoDownloadCandidatesHelper(): List<SubjectEntity>

    /**
     * Get the candidates for downloading pitch info data.
     *
     * @return the candidates
     */
    fun getPitchInfoDownloadCandidates(): List<Subject> = buildSubjectList(getPitchInfoDownloadCandidatesHelper())

    /**
     * Room-generated method: get the subjects for the given collection of IDs.
     *
     * @param ids the subject IDs to fetch
     * @return the list of subjects
     */
    @Query(
        "SELECT * FROM subject" +
            " WHERE id in (:ids) AND hiddenAt = 0 AND object IS NOT NULL"
    )
    protected abstract fun getByIdsHelper(ids: Collection<Long>): List<SubjectEntity>

    /**
     * Get the subjects for the given collection of IDs. Fetch in batches of 100 to avoid SQL queries that are too long.
     *
     * @param ids the subject IDs to fetch
     * @return the list of subjects
     */
    fun getByIds(ids: Collection<Long>): List<Subject> {
        val worklist: MutableList<Long> = ArrayList(ids)
        val result: MutableList<Subject> = ArrayList()
        while (worklist.isNotEmpty()) {
            val num = min(worklist.size, 100)
            result.addAll(buildSubjectList(getByIdsHelper(worklist.subList(0, num))))
            worklist.subList(0, num).clear()
        }
        return result
    }

    /**
     * Room-generated method: get a list of all subjects in a given range of levels.
     *
     * @param firstLevel the lowest level for requested subjects
     * @param lastLevel the lowest level for requested subjects
     * @return the list of subjects
     */
    @Query(
        "SELECT * FROM subject" +
            " WHERE level >= :firstLevel AND level <= :lastLevel AND hiddenAt = 0 ORDER BY level, lessonPosition, id"
    )
    protected abstract fun getByLevelRangeHelper(firstLevel: Int, lastLevel: Int): List<SubjectEntity>

    /**
     * Get a list of all subjects in a given range of levels.
     *
     * @param firstLevel the lowest level for requested subjects
     * @param lastLevel the lowest level for requested subjects
     * @return the list of subjects
     */
    fun getByLevelRange(firstLevel: Int, lastLevel: Int): List<Subject> =
        buildSubjectList(getByLevelRangeHelper(firstLevel, lastLevel))

    /**
     * Room-generated method: get a list of all subjects for a given level/type pair.
     *
     * @param level the level for the subjects
     * @param type the type for the subjects
     * @return the list of subjects
     */
    @Query(
        "SELECT * FROM subject" +
            " WHERE level = :level AND object = :type AND hiddenAt = 0"
    )
    protected abstract fun getLevelProgressSubjectsHelper(level: Int, type: SubjectType): List<SubjectEntity>

    /**
     * Get a list of all subjects for a given level/type pair.
     *
     * @param level the level for the subjects
     * @param type the type for the subjects
     * @return the list of subjects
     */
    fun getLevelProgressSubjects(level: Int, type: SubjectType): List<Subject> =
        buildSubjectList(getLevelProgressSubjectsHelper(level, type))

    /**
     * Room-generated method: get a list of subjects from a dynamically generated SQL query string.
     *
     * @param query the query to run
     * @return the list of subjects
     */
    @RawQuery
    protected abstract fun getSubjectsWithRawQueryHelper(query: SupportSQLiteQuery): List<SubjectEntity>

    /**
     * Room-generated method: get a list of subjects from a dynamically generated SQL query string.
     *
     * @param query the query to run
     * @return the list of subjects
     */
    fun getSubjectsWithRawQuery(query: SupportSQLiteQuery): List<Subject> =
        buildSubjectList(getSubjectsWithRawQueryHelper(query))

    /**
     * Get a collection of subject IDs that are on the level-up track: current-level kanji
     * and radicals that are locking away current-level kanji. Empty list if the user is
     * at max level.
     *
     * @param userLevel the user's level
     * @param maxLevel the max level allowed by the user's subscription
     * @return the collection of IDs
     */
    fun getLevelUpIds(userLevel: Int, maxLevel: Int): Collection<Long> {
        val result: MutableCollection<Long> = HashSet()
        if (userLevel < maxLevel) {
            for (subject in getKanjiForLevel(userLevel)) {
                result.add(subject.id)
                result.addAll(subject.getComponentSubjectIds())
            }
        }
        return result
    }

    /**
     * Get the subject IDs for the subjects that have a specific number of stars.
     *
     * @param numStars the number of stars
     * @return the list of IDs
     */
    @Query("SELECT id FROM subject WHERE numStars = :numStars ORDER BY id")
    abstract fun getStarredSubjectIds(numStars: Int): List<Long>

    companion object {
        private fun buildSubjectList(list: Collection<SubjectEntity>): List<Subject> = list.map { Subject(it) }
    }
}
