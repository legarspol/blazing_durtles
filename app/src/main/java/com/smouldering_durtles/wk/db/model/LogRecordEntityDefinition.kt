package com.smouldering_durtles.wk.db.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for the log_record table. The entity class actually used is LogRecord, this is just
 * a class used to define the schema for this table. The reason for two separate classes is a schema
 * migration: I moved from timestamps as Date instances to long Java timestamps in application code.
 * The database is already storing timestamps as long Java timestamps, but the application code was
 * using Date. An artifact of the current schema is that the timestamp columns are defined to be
 * nullable, even though a primitive long is not nullable.
 * Until I do a schema change to recreate the table with non-nullable timestamp columns,
 * this is a workaround so application code can use primitive longs but the database schema can stay
 * the same.
 *
 * TLDR: ignore this class except when modifying the database schema. Drop this class when the inevitable
 * upcoming schema overhaul is done.
 */
@Suppress("unused")
@Entity(tableName = "log_record")
class LogRecordEntityDefinition {
    @PrimaryKey(autoGenerate = true) var id: Long = 0L
    var timestamp: Long? = null
    var tag: String? = null
    var length: Int = 0
    var message: String? = null
}
