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
import kotlinx.serialization.Serializable

/**
 * Model class for a 'hidden' subject meaning that is blacklisted or whitelisted.
 *
 * Still (de)serialized through the legacy Jackson `Converters` object mapper too: subjects store
 * their auxiliary meanings as a JSON blob in a Room column (see
 * [com.smouldering_durtles.wk.db.dao.SubjectSyncDao] and
 * [com.smouldering_durtles.wk.db.model.Subject], both out of scope for this port), which is
 * re-parsed via Jackson on every read - not just over the network - so the `@JsonCreator`/
 * `@JsonProperty` annotations here are load-bearing, not vestigial.
 */
@Serializable
data class AuxiliaryMeaning @JsonCreator constructor(
    @param:JsonProperty("meaning") val meaning: String? = null,
    @param:JsonProperty("type") val type: String? = null
) {
    /**
     * Check the type to see if this is a whitelist entry.
     *
     * @return true if it is
     */
    fun isWhiteList(): Boolean = type == "whitelist"

    /**
     * Check the type to see if this is a blacklist entry.
     *
     * @return true if it is
     */
    fun isBlackList(): Boolean = type == "blacklist"
}
