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

package com.smouldering_durtles.wk.components

import com.smouldering_durtles.wk.util.TextUtil
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

/**
 * Custom kotlinx.serialization serializer for dates, hardcoded for the format used in the
 * WaniKani API. This combines the roles of the legacy Jackson `WaniKaniApiDateSerializer` and
 * `WaniKaniApiDateDeserializer` classes, since [KSerializer] handles both directions in one type.
 *
 * Timestamps are represented on the wire as either `null` (for timestamp `0`, i.e. "not set") or
 * an ISO-8601 offset date-time string, matching the format WaniKani's API uses. Parsing delegates
 * to [TextUtil.formatTimestampForApi]/[TextUtil.parseTimestampFromApi], which already contain the
 * exact formatting/parsing rules (including the "any unparseable string becomes 0" leniency).
 */
object WaniKaniApiDateSerializer : KSerializer<Long> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("WaniKaniApiDate", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Long) {
        val jsonEncoder = encoder as? JsonEncoder ?: error("WaniKaniApiDateSerializer only supports JSON")
        val formatted = TextUtil.formatTimestampForApi(value)
        jsonEncoder.encodeJsonElement(if (formatted == null) JsonNull else JsonPrimitive(formatted))
    }

    override fun deserialize(decoder: Decoder): Long {
        val jsonDecoder = decoder as? JsonDecoder ?: error("WaniKaniApiDateSerializer only supports JSON")
        val element = jsonDecoder.decodeJsonElement()
        val text = (element as? JsonPrimitive)?.takeIf { it.isString }?.content
        return TextUtil.parseTimestampFromApi(text)
    }
}
