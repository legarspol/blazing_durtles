package com.smouldering_durtles.wk.db.model

import com.fasterxml.jackson.core.type.TypeReference
import com.smouldering_durtles.wk.api.model.PronunciationAudio
import com.smouldering_durtles.wk.db.Converters
import com.smouldering_durtles.wk.util.ObjectSupport.isEmpty
import java.io.IOException

/**
 * Subset of Subject used for audio file scanning.
 */
class SubjectPronunciationAudio : PronunciationAudioOwner {
    /**
     * The subject ID.
     */
    override var id: Long = 0L

    /**
     * The level this subject belongs to.
     */
    override var level: Int = 0

    /**
     * The audio for this vocab, empty for radicals and kanji. Encoded as a JSON string.
     */
    @Suppress("unused")
    var pronunciationAudios: String? = null

    override val parsedPronunciationAudios: List<PronunciationAudio>
        get() {
            val json = pronunciationAudios
            if (isEmpty(json)) {
                return emptyList()
            }
            return try {
                Converters.getObjectMapper().readValue(json, object : TypeReference<List<PronunciationAudio>>() {})
            } catch (e: IOException) {
                emptyList()
            }
        }
}
