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

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.smouldering_durtles.wk.model.PitchInfo

/**
 * Custom Jackson serializer for PitchInfo instances. PitchInfo isn't part of the WaniKani API
 * wire format - this is used for the locally-bundled/cached pitch-accent reference data, which
 * is still read/written through the legacy Jackson [com.smouldering_durtles.wk.db.Converters]
 * object mapper.
 */
class PitchInfoSerializer : StdSerializer<PitchInfo>(PitchInfo::class.java) {
    override fun serialize(value: PitchInfo?, gen: JsonGenerator, provider: SerializerProvider) {
        if (value == null) {
            gen.writeNull()
            return
        }

        gen.writeStartArray()
        if (value.reading == null) {
            gen.writeNull()
        } else {
            gen.writeString(value.reading)
        }
        if (value.partOfSpeech == null) {
            gen.writeNull()
        } else {
            gen.writeString(value.partOfSpeech)
        }
        gen.writeNumber(value.pitchNumber)
        gen.writeEndArray()
    }

    companion object {
        private const val serialVersionUID = -1569741771743919908L
    }
}
