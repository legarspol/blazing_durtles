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
 * Model class to represent a user record in the API.
 *
 * Unlike the other entity DTOs in this package, this one doesn't implement [WaniKaniEntity]: its
 * `id` is a plain string field that's genuinely part of the "data" JSON object (WK's API doesn't
 * pull the user's ID out into the enclosing envelope the way it does for other entity types), so
 * no special handling is needed here - it's just a normal property.
 */
@Serializable
data class ApiUser(
    val id: String? = null,
    val level: Int = 0,
    @SerialName("max_level_granted_by_subscription") val maxLevelGrantedBySubscription: Int = 0,
    @SerialName("current_vacation_started_at") @Serializable(with = WaniKaniApiDateSerializer::class) val currentVacationStartedAt: Long = 0L,
    val subscription: ApiSubscription? = null,
    val username: String? = null
)
