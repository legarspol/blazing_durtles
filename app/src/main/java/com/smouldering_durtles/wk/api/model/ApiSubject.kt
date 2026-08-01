/*
 * Copyright 2019-2020 Ernst Jan Plugge <rmc@dds.nl>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.smouldering_durtles.wk.api.model

import com.smouldering_durtles.wk.components.WaniKaniApiDateSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * Model class representing a subject in the API.
 *
 * The ID and object type arrive outside of the entity's own "data" JSON object in WK's API
 * representation (see [WaniKaniEntity]), so [idValue]/[objectValue] are excluded from the
 * (de)serialized shape and populated separately by the caller via [setId]/[setObject]. Unlike the
 * other [WaniKaniEntity] implementers in this package, `object` is genuinely stored here (not a
 * no-op), matching the original Java class.
 */
@Serializable
data class ApiSubject(
    @Transient private var idValue: Long = 0L,
    @Transient private var objectValue: String? = null,
    @SerialName("created_at") @Serializable(with = WaniKaniApiDateSerializer::class) val createdAt: Long = 0L,
    @SerialName("hidden_at") @Serializable(with = WaniKaniApiDateSerializer::class) val hiddenAt: Long = 0L,
    @SerialName("document_url") val documentUrl: String? = null,
    @SerialName("lesson_position") val lessonPosition: Int = 0,
    @SerialName("spaced_repetition_system_id") val srsSystemId: Long = 0L,
    val level: Int = 0,
    val characters: String? = null,
    val slug: String? = null,
    val meanings: List<Meaning> = emptyList(),
    @SerialName("meaning_mnemonic") val meaningMnemonic: String? = null,
    @SerialName("meaning_hint") val meaningHint: String? = null,
    @SerialName("auxiliary_meanings") val auxiliaryMeanings: List<AuxiliaryMeaning> = emptyList(),
    val readings: List<Reading> = emptyList(),
    @SerialName("reading_mnemonic") val readingMnemonic: String? = null,
    @SerialName("reading_hint") val readingHint: String? = null,
    @SerialName("component_subject_ids") val componentSubjectIds: List<Long> = emptyList(),
    @SerialName("amalgamation_subject_ids") val amalgamationSubjectIds: List<Long> = emptyList(),
    @SerialName("visually_similar_subject_ids") val visuallySimilarSubjectIds: List<Long> = emptyList(),
    @SerialName("parts_of_speech") val partsOfSpeech: List<String> = emptyList(),
    @SerialName("context_sentences") val contextSentences: List<ContextSentence> = emptyList(),
    @SerialName("pronunciation_audios") val pronunciationAudios: List<PronunciationAudio> = emptyList()
) : WaniKaniEntity {
    /**
     * The unique ID for this subject.
     */
    val id: Long get() = idValue

    override fun setId(id: Long) {
        idValue = id
    }

    /**
     * The type of subject, one of "radical", "kanji", "vocabulary", "kana_vocabulary".
     */
    fun getObject(): String? = objectValue

    override fun setObject(`object`: String?) {
        objectValue = `object`
    }
}
