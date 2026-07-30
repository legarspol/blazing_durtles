package com.smouldering_durtles.wk.db.dao

import androidx.room.Dao
import androidx.room.Query
import com.smouldering_durtles.wk.model.AlertContext
import com.smouldering_durtles.wk.model.JlptProgressItem
import com.smouldering_durtles.wk.model.JoyoProgressItem

/**
 * DAO for subjects.
 */
@Dao
abstract class SubjectAggregatesDao {
    /**
     * Room-generated method: get the next timestamp when a review becomes available, after the given cutoff date.
     *
     * @param maxLevel the maximum level available on the user's subscription
     * @param cutoff the cutoff date
     * @return the available timestamp or null if there are no long-term upcoming reviews
     */
    @Query(
        "SELECT availableAt FROM subject" +
            " WHERE hiddenAt = 0 AND object IS NOT NULL" +
            " AND level <= :maxLevel" +
            " AND availableAt != 0 AND availableAt >= :cutoff" +
            " ORDER BY availableAt LIMIT 1"
    )
    abstract fun getNextLongTermReviewDate(maxLevel: Int, cutoff: Long): Long

    /**
     * Room-generated method: get the number of reviews that will become available at the specified time.
     *
     * @param maxLevel the maximum level available on the user's subscription
     * @param targetDate the date for reviews to become available at
     * @return the count
     */
    @Query(
        "SELECT COUNT(id) FROM subject" +
            " WHERE hiddenAt = 0 AND object IS NOT NULL" +
            " AND level <= :maxLevel" +
            " AND availableAt = :targetDate"
    )
    abstract fun getNextLongTermReviewCount(maxLevel: Int, targetDate: Long): Int

    /**
     * Room-generated method: get statistics for widgets and notifications.
     *
     *  * Number of available lessons
     *  * Number of available reviews
     *  * Timestamp of the newest available review
     *  * Timestamp of next upcoming review
     *
     * @param maxLevel the maximum level available on the user's subscription
     * @param cutoff the current date
     * @return a POJO containing the results
     */
    @Query(
        "SELECT numLessons, numReviews, newestAvailableAt, upcomingAvailableAt FROM " +
            "(SELECT COUNT(*) AS numLessons FROM subject WHERE hiddenAt=0 AND object IS NOT NULL " +
            "AND level <= :maxLevel AND unlockedAt!=0 AND startedAt=0 AND (resurrectedAt!=0 OR burnedAt=0)), " +
            "(SELECT COUNT(*) AS numReviews FROM subject WHERE hiddenAt=0 AND object IS NOT NULL " +
            "AND level <= :maxLevel AND availableAt!=0 AND availableAt < :cutoff), " +
            "(SELECT MAX(availableAt) AS newestAvailableAt FROM subject WHERE hiddenAt=0 AND object IS NOT NULL " +
            "AND level <= :maxLevel AND availableAt!=0 AND availableAt < :cutoff), " +
            "(SELECT MIN(availableAt) AS upcomingAvailableAt FROM subject WHERE hiddenAt=0 AND object IS NOT NULL " +
            "AND level <= :maxLevel AND availableAt!=0 AND availableAt > :cutoff)" +
            ";"
    )
    abstract fun getAlertContext(maxLevel: Int, cutoff: Long): AlertContext

    /**
     * Room-generated method: get the date the user reached a level by looking at the earliest unlockedAt date
     * for that level. This is only used as fallback if a level progression record is not available.
     *
     * @param level the level
     * @return the date or null if not reached yet
     */
    @Query("SELECT MIN(unlockedAt) FROM subject WHERE hiddenAt = 0 AND object IS NOT NULL AND unlockedAt != 0 AND level = :level")
    abstract fun getLevelReachedDate(level: Int): Long

    /**
     * Room-generated method: get the highest level of any subject in the database.
     *
     * @return the highest level
     */
    @Query("SELECT MAX(level) FROM subject WHERE hiddenAt = 0 AND object IS NOT NULL")
    abstract fun getMaxLevel(): Int

    /**
     * Room-generated method: get the JLPT progress detail.
     *
     * @return the list of items
     */
    @Query(
        "SELECT srsSystemId, srsStage, jlptLevel, COUNT(id) AS count FROM subject WHERE (object = 'kanji') " +
            "AND jlptLevel > 0 GROUP BY srsSystemId, srsStage, jlptLevel"
    )
    abstract fun getJlptProgress(): List<JlptProgressItem>

    /**
     * Room-generated method: get the Joyo progress detail.
     *
     * @return the list of items
     */
    @Query(
        "SELECT srsSystemId, srsStage, joyoGrade, COUNT(id) AS count FROM subject WHERE (object = 'kanji') " +
            "AND joyoGrade > 0 GROUP BY srsSystemId, srsStage, joyoGrade"
    )
    abstract fun getJoyoProgress(): List<JoyoProgressItem>
}
