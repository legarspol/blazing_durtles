package com.smouldering_durtles.wk.db.dao

import androidx.room.Dao
import androidx.room.Query
import com.smouldering_durtles.wk.db.model.AudioDownloadStatus

/**
 * DAO for audio download status records.
 */
@Dao
abstract class AudioDownloadStatusDao {
    /**
     * Room-generated method: get all records currently available.
     *
     * @return the list of records
     */
    @Query("SELECT * FROM audio_download_status ORDER BY level")
    abstract fun getAll(): List<AudioDownloadStatus>

    /**
     * Room-generated method: delete all records.
     */
    @Query("DELETE FROM audio_download_status")
    abstract fun deleteAll()

    /**
     * Room-generated method: insert or update a record.
     *
     * @param level the level
     * @param numTotal AudioDownloadStatus field
     * @param numNoAudio AudioDownloadStatus field
     * @param numMissingAudio AudioDownloadStatus field
     * @param numPartialAudio AudioDownloadStatus field
     * @param numFullAudio AudioDownloadStatus field
     */
    @Query(
        "INSERT OR REPLACE INTO audio_download_status (level, numTotal, numNoAudio, numMissingAudio, numPartialAudio, numFullAudio) " +
            "VALUES (:level, :numTotal, :numNoAudio, :numMissingAudio, :numPartialAudio, :numFullAudio)"
    )
    abstract fun insertOrUpdate(
        level: Int,
        numTotal: Int,
        numNoAudio: Int,
        numMissingAudio: Int,
        numPartialAudio: Int,
        numFullAudio: Int
    )
}
