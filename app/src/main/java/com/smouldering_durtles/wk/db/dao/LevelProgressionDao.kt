package com.smouldering_durtles.wk.db.dao

import androidx.room.Dao
import androidx.room.Query
import com.smouldering_durtles.wk.api.model.ApiLevelProgression
import com.smouldering_durtles.wk.db.model.LevelProgression

/**
 * DAO for level progression records.
 */
@Dao
abstract class LevelProgressionDao {
    /**
     * Room-generated method: delete all records.
     */
    @Query("DELETE FROM level_progression")
    abstract fun deleteAll()

    /**
     * Room-generated method: get all records for a specific level.
     *
     * @param userLevel the level to fetch for
     * @return the list of records in no particular order
     */
    @Query("SELECT * FROM level_progression WHERE level = :userLevel")
    protected abstract fun getForLevel(userLevel: Int): List<LevelProgression>

    /**
     * Room-generated method: get a specific record.
     *
     * @param id the ID to fetch
     * @return the record or null if it doesn't exist
     */
    @Query("SELECT * FROM level_progression WHERE id = :id")
    abstract fun getById(id: Long): LevelProgression?

    /**
     * Get the time when the user most recently started this level.
     *
     * @param userLevel the level to look for
     * @return the timestamp or null if not started or unknown.
     */
    fun getLevelReachedDate(userLevel: Int): Long {
        var since = 0L
        for (lp in getForLevel(userLevel)) {
            val date = lp.getSince()
            if (date == 0L) {
                continue
            }
            if (since == 0L || date > since) {
                since = date
            }
        }
        return since
    }

    /**
     * Update a record in the database if it exists, or create a new record.
     *
     * @param apiLevelProgression the API entity to take the data from
     */
    fun insertOrUpdate(apiLevelProgression: ApiLevelProgression) {
        var exists = true

        var lp = getById(apiLevelProgression.id)
        if (lp == null) {
            lp = LevelProgression()
            lp.id = apiLevelProgression.id
            exists = false
        }

        lp.abandonedAt = apiLevelProgression.abandonedAt
        lp.completedAt = apiLevelProgression.completedAt
        lp.createdAt = apiLevelProgression.createdAt
        lp.passedAt = apiLevelProgression.passedAt
        lp.startedAt = apiLevelProgression.startedAt
        lp.unlockedAt = apiLevelProgression.unlockedAt
        lp.level = apiLevelProgression.level

        if (exists) {
            update(lp)
        } else {
            insert(lp)
        }
    }

    /**
     * Room-generated method: insert a row.
     *
     * @param id entity field
     * @param abandonedAt entity field
     * @param completedAt entity field
     * @param createdAt entity field
     * @param passedAt entity field
     * @param startedAt entity field
     * @param unlockedAt entity field
     * @param level entity field
     */
    @Query(
        "INSERT INTO level_progression (id, abandonedAt, completedAt, createdAt, passedAt, startedAt, unlockedAt, level) VALUES" +
            " (:id, :abandonedAt, :completedAt, :createdAt, :passedAt, :startedAt, :unlockedAt, :level)"
    )
    protected abstract fun insertHelper(
        id: Long,
        abandonedAt: Long,
        completedAt: Long,
        createdAt: Long,
        passedAt: Long,
        startedAt: Long,
        unlockedAt: Long,
        level: Int
    )

    private fun insert(levelProgression: LevelProgression) {
        insertHelper(
            levelProgression.id, levelProgression.abandonedAt, levelProgression.completedAt,
            levelProgression.createdAt, levelProgression.passedAt, levelProgression.startedAt,
            levelProgression.unlockedAt, levelProgression.level
        )
    }

    /**
     * Room-generated method: update a row.
     *
     * @param id ID of the row to update
     * @param abandonedAt entity field
     * @param completedAt entity field
     * @param createdAt entity field
     * @param passedAt entity field
     * @param startedAt entity field
     * @param unlockedAt entity field
     * @param level entity field
     */
    @Query(
        "UPDATE level_progression SET abandonedAt=:abandonedAt, completedAt=:completedAt, createdAt=:createdAt, passedAt=:passedAt, " +
            "startedAt=:startedAt, unlockedAt=:unlockedAt, level=:level WHERE id = :id"
    )
    protected abstract fun updateHelper(
        id: Long,
        abandonedAt: Long,
        completedAt: Long,
        createdAt: Long,
        passedAt: Long,
        startedAt: Long,
        unlockedAt: Long,
        level: Int
    )

    private fun update(levelProgression: LevelProgression) {
        updateHelper(
            levelProgression.id, levelProgression.abandonedAt, levelProgression.completedAt,
            levelProgression.createdAt, levelProgression.passedAt, levelProgression.startedAt,
            levelProgression.unlockedAt, levelProgression.level
        )
    }
}
