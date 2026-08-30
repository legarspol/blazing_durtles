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

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.smouldering_durtles.wk.model.DigraphMatch
import com.smouldering_durtles.wk.util.ObjectSupport.isEmpty
import com.smouldering_durtles.wk.util.ObjectSupport.isEqual
import com.smouldering_durtles.wk.util.PseudoIme
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Model class for a reading registered for a subject.
 *
 * This isn't a pure DTO: it also carries the answer-grading logic ([matches],
 * [matchesForDigraph], on'yomi/kun'yomi/nanori classification) used by
 * [com.smouldering_durtles.wk.enums.QuestionType] (out of scope for this port), so it's ported as
 * one class 1:1 rather than split apart, even though that means bending the usual
 * one-responsibility-per-class rule for this file specifically.
 *
 * Still (de)serialized through the legacy Jackson `Converters` object mapper too: subjects store
 * their readings as a JSON blob in a Room column (see
 * [com.smouldering_durtles.wk.db.dao.SubjectSyncDao] and
 * [com.smouldering_durtles.wk.db.model.Subject], both out of scope for this port), which is
 * re-parsed via Jackson on every read - not just over the network - so the `@JsonCreator`/
 * `@JsonProperty` annotations here are load-bearing, not vestigial.
 */
@Serializable
data class Reading @JsonCreator constructor(
    @param:JsonProperty("reading") val reading: String? = null,
    @param:JsonProperty("primary") @SerialName("primary") val isPrimary: Boolean = false,
    @param:JsonProperty("accepted_answer") @SerialName("accepted_answer") val isAcceptedAnswer: Boolean = false,
    @param:JsonProperty("type") val type: String? = null
) {
    /**
     * Is this an on'yomi reading?.
     *
     * A property (not a function) because [com.smouldering_durtles.wk.db.model.Subject] (out of
     * scope for this port, but Kotlin) reads it via property syntax (`it.isOnYomi`), which only
     * resolves against a genuine Kotlin property - unlike Java callers, Kotlin doesn't treat an
     * `isFoo()` *function* as a synthetic property.
     */
    val isOnYomi: Boolean get() = isEqual(type, "onyomi")

    /**
     * Is this an kun'yomi reading?. See [isOnYomi] for why this is a property.
     */
    val isKunYomi: Boolean get() = isEqual(type, "kunyomi")

    /**
     * Is this an nanori reading?. See [isOnYomi] for why this is a property.
     */
    val isNanori: Boolean get() = isEqual(type, "nanori")

    /**
     * Get the reading text, possibly converted to katakana.
     *
     * @param showOnInKatakana true if on'yomi should be rendered in katakana.
     * @return the text
     */
    fun getValue(showOnInKatakana: Boolean): String? {
        if (showOnInKatakana && isOnYomi) {
            return PseudoIme.toKatakana(reading)
        }
        return reading
    }

    /**
     * Check if an answer matches this reading, taking into account the setting to require on'yomi
     * in katakana.
     *
     * @param answer the answer to check
     * @param requireOnInKatakana true if the answer must be in katakana if the reading is on'yomi
     * @return true if it is a match
     */
    fun matches(answer: String, requireOnInKatakana: Boolean): Boolean {
        if (isOnYomi) {
            if (requireOnInKatakana) {
                return isEqual(PseudoIme.toKatakana(reading), answer)
            }
            return isEqual(reading, answer) || isEqual(PseudoIme.toKatakana(reading), answer)
        }
        return isEqual(reading, answer)
    }

    /**
     * Check if an answer matches this reading except for a digraph mismatch.
     *
     * @param answer the answer to check
     * @return the digraph match details if the answer is correct except for the digraph mismatch
     */
    fun matchesForDigraph(answer: CharSequence): DigraphMatch? {
        if (isOnYomi) {
            val regularMatch = matchesForDigraph(answer, reading)
            if (regularMatch != null) {
                return regularMatch
            }
            return matchesForDigraph(answer, PseudoIme.toKatakana(reading))
        }
        return matchesForDigraph(answer, reading)
    }

    /**
     * Is the meaning empty or it it's value "None".
     */
    fun isEmptyOrNone(): Boolean = isEmpty(reading) || reading == "None"

    companion object {
        private const val smallKana = "ぁぃぅぇぉっゃゅょゎゕゖァィゥェォッャュョヮヵヶ"
        private const val regularKana = "あいうえおつやゆよわかけアイウエオツヤユヨワカケ"

        private fun matchesForDigraph(answer: CharSequence, baseLine: CharSequence?): DigraphMatch? {
            if (baseLine == null || answer.length != baseLine.length) {
                return null
            }
            var regular = 0.toChar()
            var small = 0.toChar()
            for (i in answer.indices) {
                val c1 = answer[i]
                val c2 = baseLine[i]
                if (c1 == c2) {
                    continue
                }
                val p1 = regularKana.indexOf(c1)
                if (p1 >= 0 && smallKana[p1] == c2) {
                    regular = c1
                    small = c2
                    continue
                }
                val p2 = smallKana.indexOf(c1)
                if (p2 >= 0 && regularKana[p2] == c2) {
                    regular = c2
                    small = c1
                    continue
                }
                return null
            }
            if (regular.code != 0) {
                return DigraphMatch(regular, small)
            }
            return null
        }
    }
}
