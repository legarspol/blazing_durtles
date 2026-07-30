package com.smouldering_durtles.wk.db.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for the audio_download_status table. This caches the status
 * of audio downloads, for the audio download page.
 */
@Entity(tableName = "audio_download_status")
class AudioDownloadStatus {
    /**
     * The level for this summary, also primary key.
     */
    @PrimaryKey var level: Int = 0

    /**
     * The total number of subjects in this level.
     */
    var numTotal: Int = 0

    /**
     * The number of subjects that have no audio.
     */
    var numNoAudio: Int = 0

    /**
     * The number of subjects that have audio but none are available.
     */
    var numMissingAudio: Int = 0

    /**
     * The number of subjects that have audio and some are available, but not all.
     */
    var numPartialAudio: Int = 0

    /**
     * The number of subjects that have audio and all are available.
     */
    var numFullAudio: Int = 0
}
