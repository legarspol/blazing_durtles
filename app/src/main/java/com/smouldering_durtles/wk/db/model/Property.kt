package com.smouldering_durtles.wk.db.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for the properties table, which is a simple key/value store with Strings for both keys and values.
 * This entity class is not actually used directly, it's only used by Room to generate the schema.
 */
@Entity(tableName = "properties")
class Property {
    /**
     * The name and primary key.
     */
    @JvmField
    @PrimaryKey
    var name: String = ""

    /**
     * The value.
     */
    @JvmField
    var value: String = ""
}
