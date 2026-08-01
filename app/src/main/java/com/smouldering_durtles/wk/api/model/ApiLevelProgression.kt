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
 * Model class for level progression records as reported by the API.
 */
@Serializable
data class ApiLevelProgression(
    @Transient private var idValue: Long = 0L,
    @SerialName("abandoned_at") @Serializable(with = WaniKaniApiDateSerializer::class) val abandonedAt: Long = 0L,
    @SerialName("completed_at") @Serializable(with = WaniKaniApiDateSerializer::class) val completedAt: Long = 0L,
    @SerialName("created_at") @Serializable(with = WaniKaniApiDateSerializer::class) val createdAt: Long = 0L,
    @SerialName("passed_at") @Serializable(with = WaniKaniApiDateSerializer::class) val passedAt: Long = 0L,
    @SerialName("started_at") @Serializable(with = WaniKaniApiDateSerializer::class) val startedAt: Long = 0L,
    @SerialName("unlocked_at") @Serializable(with = WaniKaniApiDateSerializer::class) val unlockedAt: Long = 0L,
    @SerialName("level") val level: Int = 0
) : WaniKaniEntity {
    /**
     * Unique ID for this record.
     */
    val id: Long get() = idValue

    override fun setId(id: Long) {
        idValue = id
    }

    override fun setObject(`object`: String?) {
        //
    }
}
