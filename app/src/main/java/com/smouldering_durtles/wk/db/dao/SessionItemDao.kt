package com.smouldering_durtles.wk.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.smouldering_durtles.wk.db.model.SessionItem

/**
 * DAO for session items. These are the persisten record of a session in progress.
 */
@Dao
abstract class SessionItemDao {
    /**
     * Room-generated method: delete all items.
     */
    @Query("DELETE FROM session_item")
    abstract fun deleteAll()

    /**
     * Room-generated method: get all items.
     *
     * @return the list of items in order
     */
    @Query("SELECT * FROM session_item ORDER BY `order`")
    abstract fun getAll(): List<SessionItem>

    /**
     * Room-generated method: get a specific item by subject ID.
     *
     * @param id the subject ID
     * @return the item or null if not found
     */
    @Query("SELECT * FROM session_item WHERE id = :id LIMIT 1")
    abstract fun getById(id: Long): SessionItem?

    /**
     * Room-generated method: insert a new item.
     *
     * @param sessionItem the item to insert
     */
    @Insert
    abstract fun insert(sessionItem: SessionItem)

    /**
     * Room-generated method: update an item.
     *
     * @param sessionItem the item to update
     */
    @Update
    abstract fun update(sessionItem: SessionItem)
}
