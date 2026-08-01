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

import com.smouldering_durtles.wk.model.PitchInfo
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.intOrNull

/**
 * kotlinx.serialization counterpart to [com.smouldering_durtles.wk.util.PitchInfoSerializer] and
 * [com.smouldering_durtles.wk.util.PitchInfoDeserializer], added purely to prove wire-format
 * parity for PitchInfo's bespoke 3-element array encoding (`[reading|null, partOfSpeech|null,
 * pitchNumber]`) in tests.
 *
 * PitchInfo is not actually part of any WaniKani API response - it's used for locally-bundled/
 * cached pitch-accent reference data, which is still read/written through the legacy Jackson
 * `Converters` object mapper via the classes named above. This serializer has no live caller.
 *
 * Unlike the Jackson deserializer (which returns null for any malformed shape), this throws
 * [SerializationException] on malformed input, since a [KSerializer] of a non-nullable type has
 * no way to signal "no value" other than throwing.
 */
object PitchInfoKotlinxSerializer : KSerializer<PitchInfo> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("PitchInfo", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: PitchInfo) {
        val jsonEncoder = encoder as? JsonEncoder ?: error("PitchInfoKotlinxSerializer only supports JSON")
        val array = buildJsonArray {
            add(value.reading?.let { JsonPrimitive(it) } ?: JsonNull)
            add(value.partOfSpeech?.let { JsonPrimitive(it) } ?: JsonNull)
            add(JsonPrimitive(value.pitchNumber))
        }
        jsonEncoder.encodeJsonElement(array)
    }

    override fun deserialize(decoder: Decoder): PitchInfo {
        val jsonDecoder = decoder as? JsonDecoder ?: error("PitchInfoKotlinxSerializer only supports JSON")
        val element = jsonDecoder.decodeJsonElement()
        val array = element as? JsonArray ?: throw SerializationException("Expected a JSON array for PitchInfo")
        if (array.size != 3) {
            throw SerializationException("Expected a 3-element JSON array for PitchInfo")
        }
        val reading = array[0].asPitchInfoStringOrNull()
        val partOfSpeech = array[1].asPitchInfoStringOrNull()
        val pitchNumber = (array[2] as? JsonPrimitive)?.intOrNull
            ?: throw SerializationException("Expected a numeric pitch number for PitchInfo")
        return PitchInfo(reading, partOfSpeech, pitchNumber)
    }

    private fun JsonElement.asPitchInfoStringOrNull(): String? {
        if (this is JsonNull) {
            return null
        }
        val primitive = this as? JsonPrimitive
            ?: throw SerializationException("Expected a string or null in PitchInfo array")
        if (!primitive.isString) {
            throw SerializationException("Expected a string or null in PitchInfo array")
        }
        return primitive.content
    }
}
