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

package com.smouldering_durtles.wk.model

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.smouldering_durtles.wk.api.model.PitchInfoKotlinxSerializer
import com.smouldering_durtles.wk.util.PitchInfoDeserializer
import com.smouldering_durtles.wk.util.PitchInfoSerializer
import com.smouldering_durtles.wk.util.PseudoIme
import kotlinx.serialization.Serializable

/**
 * Model class to represent one item of pitch information.
 *
 * The Jackson annotations are preserved because this class is still (de)serialized through the
 * legacy Jackson `Converters` object mapper by [com.smouldering_durtles.wk.util.ReferenceDataUtil],
 * [com.smouldering_durtles.wk.util.PitchInfoUtil] and
 * [com.smouldering_durtles.wk.tasks.DownloadPitchInfoTask] - none of which are in scope for this
 * port. The kotlinx.serialization annotation is there purely so tests can prove wire-format parity
 * for the bespoke array encoding; PitchInfo isn't otherwise part of any kotlinx-serialized graph.
 *
 * @param reading The reading this item applies to.
 * @param partOfSpeech The part of speech this entry applies to, null if not differentiated.
 * @param pitchNumber The number for the applicable pitch pattern.
 */
@JsonSerialize(using = PitchInfoSerializer::class)
@JsonDeserialize(using = PitchInfoDeserializer::class)
@Serializable(with = PitchInfoKotlinxSerializer::class)
class PitchInfo(reading: CharSequence?, partOfSpeech: String?, val pitchNumber: Int) : Comparable<PitchInfo> {
    val reading: String? = reading?.let { requireNotNull(PseudoIme.toKatakana(it)).intern() }
    val partOfSpeech: String? = partOfSpeech?.intern()

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || other !is PitchInfo) {
            return false
        }
        return pitchNumber == other.pitchNumber && reading == other.reading && partOfSpeech == other.partOfSpeech
    }

    override fun hashCode(): Int {
        return arrayOf<Any?>(reading, partOfSpeech, pitchNumber).contentHashCode()
    }

    override fun compareTo(other: PitchInfo): Int {
        val readingComparison = compareValues(reading, other.reading)
        if (readingComparison != 0) {
            return readingComparison
        }
        val partOfSpeechComparison = compareValues(partOfSpeech, other.partOfSpeech)
        if (partOfSpeechComparison != 0) {
            return partOfSpeechComparison
        }
        return pitchNumber.compareTo(other.pitchNumber)
    }
}
