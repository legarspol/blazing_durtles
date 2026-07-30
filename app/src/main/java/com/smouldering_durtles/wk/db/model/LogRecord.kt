package com.smouldering_durtles.wk.db.model

/**
 * Room entity for the log_record table. This is where debug log records are stored.
 * Each row also contains the length of its message, to make it easier to trim excess
 * entries.
 */
class LogRecord {
    /**
     * Primary key.
     */
    @JvmField var id: Long = 0L

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
