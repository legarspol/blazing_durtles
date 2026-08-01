package com.smouldering_durtles.wk.api.model

import com.smouldering_durtles.wk.components.WaniKaniApiDateSerializer
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Golden-JSON parity tests for [WaniKaniApiDateSerializer], the kotlinx.serialization
 * `KSerializer<Long>` that replaced the legacy Jackson `WaniKaniApiDateSerializer`/
 * `WaniKaniApiDateDeserializer` pair. Covers the null/zero/offset/invalid-string variants the
 * legacy pair handled (see [com.smouldering_durtles.wk.util.TextUtil.formatTimestampForApi] and
 * [com.smouldering_durtles.wk.util.TextUtil.parseTimestampFromApi], which this serializer
 * delegates to and which stay unchanged).
 */
class WaniKaniApiDateSerializerTest {
    @Test
    fun `zero serializes to JSON null`() {
        val json = apiModelJson.encodeToString(WaniKaniApiDateSerializer, 0L)
        assertEquals("null", json)
    }

    @Test
    fun `JSON null deserializes to zero`() {
        val value = apiModelJson.decodeFromString(WaniKaniApiDateSerializer, "null")
        assertEquals(0L, value)
    }

    @Test
    fun `empty string deserializes to zero`() {
        val value = apiModelJson.decodeFromString(WaniKaniApiDateSerializer, "\"\"")
        assertEquals(0L, value)
    }

    // Note: TextUtil.parseTimestampFromApi's "unparseable string -> 0" leniency (see its
    // ObjectSupport.safe() usage) routes through Logger/DbLogger, which calls android.util.Log
    // directly. That's fine on-device but throws "not mocked" in a bare JVM unit test (no
    // Robolectric configured for this module), so it isn't exercised here - this is a pre-existing
    // characteristic of TextUtil (out of scope for this port), not something introduced by it.

    @Test
    fun `a non-zero UTC timestamp round trips through the ISO-8601 offset format`() {
        val epochMillis = Instant.parse("2023-01-15T10:30:00Z").toEpochMilli()

        val json = apiModelJson.encodeToString(WaniKaniApiDateSerializer, epochMillis)
        val expected = ZonedDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneOffset.UTC)
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        assertEquals("\"$expected\"", json)

        val roundTripped = apiModelJson.decodeFromString(WaniKaniApiDateSerializer, json)
        assertEquals(epochMillis, roundTripped)
    }

    @Test
    fun `a string with a non-UTC offset parses to the correct instant`() {
        // 2021-04-05T08:00:00+09:00 is 2021-04-04T23:00:00Z.
        val value = apiModelJson.decodeFromString(WaniKaniApiDateSerializer, "\"2021-04-05T08:00:00+09:00\"")
        val expected = Instant.parse("2021-04-04T23:00:00Z").toEpochMilli()
        assertEquals(expected, value)
    }
}
