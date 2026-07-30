package com.smouldering_durtles.wk.db.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for the search_preset table, which stores the user's search presets.
 */
@Entity(tableName = "search_preset")
class SearchPreset {
    /**
     * Name of this preset.
     */
    @JvmField
    @PrimaryKey
    var name: String = ""

    /**
     * Type of this preset. 0 = level browse, 1 = simple keyword search, 2 = advanced search
     */
    @JvmField
    var type: Int = 0

    /**
     * Type-specific string that encodes the parameters for the search preset.
     */
    @JvmField
    var data: String = ""
}
