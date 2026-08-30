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
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Model class representing an SRS stage in the API.
 *
 * The fields are exposed as plain JVM fields (`@JvmField`) rather than Kotlin properties because
 * [com.smouldering_durtles.wk.livedata.LiveSrsSystems] (out of scope for this port) reads them via
 * direct Java field access (`apiStage.position`, etc.), matching the original Java class's public
 * fields.
 *
 * Still (de)serialized through the legacy Jackson `Converters` object mapper too: SRS systems
 * store their stages as a JSON blob in a Room column (see
 * [com.smouldering_durtles.wk.tasks.GetSrsSystemsTask] and
 * [com.smouldering_durtles.wk.db.model.SrsSystemDefinition], both out of scope for this port),
 * which is re-parsed via Jackson on every read - not just over the network - so the
 * `@JsonCreator`/`@JsonProperty` annotations here are load-bearing, not vestigial.
 */
@Serializable
data class ApiStage @JsonCreator constructor(
    @JvmField @param:JsonProperty("position") @SerialName("position") val position: Long = 0L,
    @JvmField @param:JsonProperty("interval") @SerialName("interval") val interval: Long = 0L,
    @JvmField @param:JsonProperty("interval_unit") @SerialName("interval_unit") val intervalUnit: String? = null
)
