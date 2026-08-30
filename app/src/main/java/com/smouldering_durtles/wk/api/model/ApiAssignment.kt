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
 * Model class representing a user's assignment for a subject, as defined by the API.
 *
 * The ID and object type arrive outside of the entity's own "data" JSON object in WK's API
 * representation (see [WaniKaniEntity]), so [idValue] is excluded from the (de)serialized shape
 * and populated separately by the caller via [setId].
 */
@Serializable
data class ApiAssignment(
    @Transient private var idValue: Long = 0L,
    @SerialName("available_at") @Serializable(with = WaniKaniApiDateSerializer::class) val availableAt: Long = 0L,
    @SerialName("burned_at") @Serializable(with = WaniKaniApiDateSerializer::class) val burnedAt: Long = 0L,
    @SerialName("passed_at") @Serializable(with = WaniKaniApiDateSerializer::class) val passedAt: Long = 0L,
    @SerialName("resurrected_at") @Serializable(with = WaniKaniApiDateSerializer::class) val resurrectedAt: Long = 0L,
    @SerialName("started_at") @Serializable(with = WaniKaniApiDateSerializer::class) val startedAt: Long = 0L,
    @SerialName("unlocked_at") @Serializable(with = WaniKaniApiDateSerializer::class) val unlockedAt: Long = 0L,
    @SerialName("srs_stage") val srsStageId: Long = 0L,
    @SerialName("subject_id") val subjectId: Long = 0L
) : WaniKaniEntity {
    /**
     * The WK ID of the entity.
     */
    val id: Long get() = idValue

    override fun setId(id: Long) {
        idValue = id
    }

    override fun setObject(`object`: String?) {
        //
    }
}
