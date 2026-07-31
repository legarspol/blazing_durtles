package com.smouldering_durtles.wk.db.model

/**
 * Room entity for a subset of the log_record table. Only contains the id and length, for trimming.
 */
class LogRecordSummary {
    /**
     * The unique ID.
     */
    var id: Long = 0L

    /**
     * The length of the message string.
     */
    var length: Int = 0
}
