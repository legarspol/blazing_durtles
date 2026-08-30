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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * Model class representing a user's review statistics for a subject, as defined by the API.
 */
@Serializable
data class ApiReviewStatistic(
    @Transient private var idValue: Long = 0L,
    @SerialName("meaning_correct") val meaningCorrect: Int = 0,
    @SerialName("meaning_incorrect") val meaningIncorrect: Int = 0,
    @SerialName("meaning_max_streak") val meaningMaxStreak: Int = 0,
    @SerialName("meaning_current_streak") val meaningCurrentStreak: Int = 0,
    @SerialName("reading_correct") val readingCorrect: Int = 0,
    @SerialName("reading_incorrect") val readingIncorrect: Int = 0,
    @SerialName("reading_max_streak") val readingMaxStreak: Int = 0,
    @SerialName("reading_current_streak") val readingCurrentStreak: Int = 0,
    @SerialName("percentage_correct") val percentageCorrect: Int = 0,
    @SerialName("subject_id") val subjectId: Long = 0L
) : WaniKaniEntity {
    /**
     * The unique ID for this record.
     */
    val id: Long get() = idValue

    override fun setId(id: Long) {
        idValue = id
    }

    override fun setObject(`object`: String?) {
        //
    }
}
