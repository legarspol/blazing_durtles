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
 * Model class representing a user's study materials for a subject, as defined by the API.
 *
 * Used both as a response DTO (parsed from the API) and, mutated through its setters, as part of
 * a request body (see [com.smouldering_durtles.wk.jobs.SaveStudyMaterialJob] and
 * [com.smouldering_durtles.wk.tasks.SubmitStudyMaterialTask], both out of scope for this port), so
 * its fields are `var`. `@JvmOverloads` gives those Java callers (`new ApiStudyMaterial()`) a
 * visible no-arg constructor.
 *
 * [idValue] is excluded from the (de)serialized shape (it was `@JsonIgnore` in the original Java
 * class too - it's not part of the wire format, and is instead populated by the caller via
 * [setId], like the ID and object type for every other [WaniKaniEntity] implementer).
 */
@Serializable
data class ApiStudyMaterial @JvmOverloads constructor(
    @Transient private var idValue: Long = 0L,
    @SerialName("meaning_note") var meaningNote: String? = null,
    @SerialName("meaning_synonyms") var meaningSynonyms: List<String> = emptyList(),
    @SerialName("reading_note") var readingNote: String? = null,
    @SerialName("subject_id") var subjectId: Long = 0L
) : WaniKaniEntity {
    /**
     * The unique ID for this instance.
     */
    val id: Long get() = idValue

    override fun setId(id: Long) {
        idValue = id
    }

    override fun setObject(`object`: String?) {
        //
    }
}
