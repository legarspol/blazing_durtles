package com.smouldering_durtles.wk.db.dao

import android.util.Log
import androidx.room.Dao
import androidx.room.Query
import com.smouldering_durtles.wk.db.model.LogRecord
import com.smouldering_durtles.wk.db.model.LogRecordSummary

/**
 * DAO for debug log records.
 */
@Dao
abstract class LogRecordDao {
    /**
     * Room-generated method: get the next log record available, with ID after the supplied one.
     *
     * @param id the id
     * @return the record or null if not found
     */
    @Query("SELECT * FROM log_record WHERE id > :id ORDER BY id LIMIT 1")
    protected abstract fun getNextHelper(id: Long): LogRecord?

    /**
     * Get the next record with ID greater than the argument ID.
     * @param id the id
     * @return the next record or null if it doesn't exist
     */
    fun getNext(id: Long): LogRecord? {
        return try {
            getNextHelper(id)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get the oldest log records as summaries.
     *
     * @param count the batch size to get
     * @return the list
     */
    @Query("SELECT id, length from log_record ORDER BY id LIMIT :count")
    abstract fun getOldestSummaries(count: Int): List<LogRecordSummary>

    /**
     * Delete the oldest records, up to the one with the supplied id.
     *
     * @param id the id
     */
    @Query("DELETE FROM log_record WHERE id < :id")
    protected abstract fun deleteOldestHelper(id: Long)

    /**
     * Delete the oldest records, up to the size specified.
     *
     * @param excess the amount of excess to trim
     * @param batchSize the size of the batch to load and examine for deletion
     */
    private fun deleteOldest(excess: Int, batchSize: Int) {
        try {
            val records = getOldestSummaries(batchSize)
            if (records.isEmpty()) {
                return
            }
            var size = 0
            for (record in records) {
                size += record.length
                if (size > excess) {
                    deleteOldestHelper(record.id)
                    return
                }
            }
            deleteOldestHelper(records[records.size - 1].id + 1)
        } catch (e: Exception) {
            Log.e("LogRecordDao", "Exception deleting log records", e)
            if (batchSize > 1) {
                deleteOldest(excess, batchSize / 2)
            }
        }
    }

    /**
     * Delete the oldest records, up to the size specified.
     *
     * @param excess the amount of excess to trim
     */
    fun deleteOldest(excess: Int) {
        deleteOldest(excess, 1 shl 16)
    }

    /**
     * Room-generated method: get the total size of all records (sum of length columns).
     *
     * @return the total size
     */
    @Query("SELECT SUM(length) FROM log_record")
    abstract fun getTotalSize(): Int

    /**
     * Room-generated method: insert a new record.
     *
     * @param timestamp entity field
     * @param tag entity field
     * @param length entity field
     * @param message entity field
     */
    @Query("INSERT INTO log_record (timestamp, tag, length, message) VALUES (:timestamp, :tag, :length, :message)")
    protected abstract fun insertHelper(timestamp: Long, tag: String?, length: Int, message: String?)

    /**
     * Insert a new record.
     *
     * @param record the record to insert
     */
    fun insert(record: LogRecord) {
        insertHelper(record.timestamp, record.tag, record.length, record.message)
    }
}
