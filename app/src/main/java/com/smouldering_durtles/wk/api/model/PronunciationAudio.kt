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
 * Model class representing a pronunciation audio file for a vocab subject.
 *
 * Still (de)serialized through the legacy Jackson `Converters` object mapper too: subjects store
 * their pronunciation audio as a JSON blob in a Room column (see
 * [com.smouldering_durtles.wk.db.dao.SubjectSyncDao] and
 * [com.smouldering_durtles.wk.db.model.Subject], both out of scope for this port), which is
 * re-parsed via Jackson on every read - not just over the network - so the `@JsonCreator`/
 * `@JsonProperty` annotations here are load-bearing, not vestigial.
 *
 * The original Java setter coalesced an explicit JSON `null` for `metadata` into a fresh
 * [PronunciationAudioMeta]; a missing/absent `metadata` key already resulted in the same default
 * both before and after this port (via the constructor default below). Only the (unlikely in
 * practice) explicit-`null` case differs now - see the port's final report for this known,
 * low-risk gap.
 */
@Serializable
data class PronunciationAudio @JsonCreator constructor(
    @param:JsonProperty("url") val url: String? = null,
    @param:JsonProperty("content_type") @SerialName("content_type") val contentType: String? = null,
    @param:JsonProperty("metadata") val metadata: PronunciationAudioMeta = PronunciationAudioMeta()
)
