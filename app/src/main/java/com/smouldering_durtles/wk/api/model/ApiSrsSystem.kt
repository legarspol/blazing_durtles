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
 * Model class for SRS system records as reported by the API.
 *
 * The fields are exposed as plain JVM fields (`@JvmField`) rather than Kotlin properties because
 * [com.smouldering_durtles.wk.tasks.GetSrsSystemsTask] (out of scope for this port) reads them via
 * direct Java field access (`t.id`, `t.name`, etc.), matching the original Java class's public
 * fields. Since `id` needs a plain field (not a getter) for that same reason, it can't use the
 * getter-based id-trick the other [WaniKaniEntity] implementers in this package use - [setId]
 * assigns directly to the [id] field instead.
 *
 * Unlike [stages]/[ApiStage] (which are additionally round-tripped through the legacy Jackson
 * `Converters` object mapper as a JSON blob stored in a Room column - see that class's doc
 * comment), the ApiSrsSystem object itself is only ever decomposed into typed Room columns by
 * [com.smouldering_durtles.wk.tasks.GetSrsSystemsTask] and never stored/reloaded as a JSON blob
 * itself, so it doesn't need the same Jackson-compat treatment.
 */
@Serializable
data class ApiSrsSystem(
    @JvmField @Transient var id: Long = 0L,
    @JvmField val name: String? = null,
    @JvmField val description: String? = null,
    @JvmField val stages: List<ApiStage> = emptyList(),
    @JvmField @SerialName("unlocking_stage_position") val unlockingStagePosition: Long = 0L,
    @JvmField @SerialName("starting_stage_position") val startingStagePosition: Long = 0L,
    @JvmField @SerialName("passing_stage_position") val passingStagePosition: Long = 0L,
    @JvmField @SerialName("burning_stage_position") val burningStagePosition: Long = 0L
) : WaniKaniEntity {
    override fun setId(id: Long) {
        this.id = id
    }

    override fun setObject(`object`: String?) {
        //
    }
}
