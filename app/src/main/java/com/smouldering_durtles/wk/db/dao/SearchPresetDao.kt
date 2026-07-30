package com.smouldering_durtles.wk.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import com.smouldering_durtles.wk.db.model.SearchPreset

/**
 * DAO for search presets.
 */
@Dao
abstract class SearchPresetDao {
    /**
     * Room-generated method: set a preset.
     *
     * @param name the name of the preset
     * @param type the type of the preset
     * @param data the value of the preset
     */
    @Query("INSERT OR REPLACE INTO search_preset (name, type, data) VALUES (:name, :type, :data)")
    abstract fun setPreset(name: String, type: Int, data: String)

    /**
     * Room-generated method: delete a preset.
     *
     * @param name the name of the preset
     */
    @Query("DELETE FROM search_preset WHERE name = :name")
    abstract fun deletePreset(name: String)

    /**
     * Room-generated method: get a LiveData instance containing the presets.
     *
     * @return the LiveData instance
     */
    @Query("SELECT * FROM search_preset ORDER BY name")
    abstract fun getLivePresets(): LiveData<List<SearchPreset>>
}
