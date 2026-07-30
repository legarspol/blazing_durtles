package com.smouldering_durtles.wk.db.model

import com.smouldering_durtles.wk.api.model.PronunciationAudio

/**
 * Interface for a subject-like object that contains pronunciation audio.
 * Apart from Subject itself, it is implemented by a trimmed down subject subset,
 * to make audio scanning more efficient.
 *
 * Declared as properties rather than `getX()` functions so implementors can satisfy them with a
 * Kotlin property; Java callers still see `getId()`/`getLevel()`/`getParsedPronunciationAudios()`.
 */
interface PronunciationAudioOwner {
    /**
     * Get the subject's ID.
     */
    val id: Long

    /**
     * Get the subject's level.
     */
    val level: Int

    /**
     * Parsed version of pronunciationAudios, inflated on demand.
     */
    val parsedPronunciationAudios: List<PronunciationAudio>
}
