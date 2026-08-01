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

package com.smouldering_durtles.wk.util

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.smouldering_durtles.wk.model.PitchInfo

/**
 * Custom Jackson deserializer for PitchInfo instances. PitchInfo isn't part of the WaniKani API
 * wire format - this is used for the locally-bundled/cached pitch-accent reference data, which
 * is still read/written through the legacy Jackson [com.smouldering_durtles.wk.db.Converters]
 * object mapper.
 */
class PitchInfoDeserializer : StdDeserializer<PitchInfo>(PitchInfo::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): PitchInfo? {
        if (!p.hasToken(JsonToken.START_ARRAY)) {
            return null
        }
        p.nextToken()
        if (!p.hasToken(JsonToken.VALUE_STRING) && !p.hasToken(JsonToken.VALUE_NULL)) {
            return null
        }
        val reading = p.valueAsString
        p.nextToken()
        if (!p.hasToken(JsonToken.VALUE_STRING) && !p.hasToken(JsonToken.VALUE_NULL)) {
            return null
        }
        val partOfSpeech = p.valueAsString
        p.nextToken()
        if (!p.hasToken(JsonToken.VALUE_NUMBER_INT) && !p.hasToken(JsonToken.VALUE_NUMBER_FLOAT)) {
            return null
        }
        val pitchNumber = p.getValueAsInt(-1)
        p.nextToken()
        if (!p.hasToken(JsonToken.END_ARRAY)) {
            return null
        }
        return PitchInfo(reading, partOfSpeech, pitchNumber)
    }

    companion object {
        private const val serialVersionUID = 5515932274345600748L
    }
}
