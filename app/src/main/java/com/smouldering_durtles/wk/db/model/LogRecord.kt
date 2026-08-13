package com.smouldering_durtles.wk.db.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for the log_record table. This is where debug log records are stored.
 * Each row also contains the length of its message, to make it easier to trim excess
 * entries.
 *
 * [timestamp] is a primitive `Long`, and the column is `NOT NULL` to match. It used to be
 * nullable because the app moved from `Date` to long timestamps and the column was left as it
 * was; a separate `LogRecordEntityDefinition` existed solely to keep declaring it nullable.
 */
@Entity(tableName = "log_record")
class LogRecord {
    /**
     * Primary key.
     */
    @JvmField @PrimaryKey(autoGenerate = true) var id: Long = 0L

    /**
     * Timestamp when the event was generated.
     */
    @JvmField var timestamp: Long = 0L

    /**
     * The tag (class name) for this record.
     */
    @JvmField var tag: String? = null

    /**
     * The length of the message string.
     */
    @JvmField var length: Int = 0

    /**
     * The log message.
     */
    @JvmField var message: String? = null
}
