package com.smouldering_durtles.wk.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.smouldering_durtles.wk.db.model.SrsSystemDefinition

/**
 * DAO for SRS systems.
 */
@Dao
abstract class SrsSystemDao {
    /**
     * Room-generated method: delete all records.
     */
    @Query("DELETE FROM srs_system")
    abstract fun deleteAll()

    /**
     * Room-generated method: get a list of all records.
     *
     * @return the list
     */
    @Query("SELECT * FROM srs_system ORDER BY id")
    abstract fun getAll(): List<SrsSystemDefinition>

    /**
     * Room-generated method: insert a new record.
     * @param system the record to insert
     */
    @Insert
    abstract fun insert(system: SrsSystemDefinition)
}
