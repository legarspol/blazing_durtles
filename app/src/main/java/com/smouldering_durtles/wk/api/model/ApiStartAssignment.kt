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

/**
 * Model class used in the API to start an assignment (submit a lesson result).
 *
 * `@JvmOverloads` gives Java callers (`new ApiStartAssignment()` in
 * [com.smouldering_durtles.wk.tasks.ReportSessionItemTask], out of scope for this port) a visible
 * no-arg constructor, since a Kotlin constructor with only default-valued parameters doesn't
 * otherwise expose one to Java.
 */
@Serializable
data class ApiStartAssignment @JvmOverloads constructor(
    @SerialName("started_at") @Serializable(with = WaniKaniApiDateSerializer::class) var startedAt: Long = 0L
)
