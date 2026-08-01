package com.smouldering_durtles.wk.api.model

import com.smouldering_durtles.wk.db.Converters
import com.smouldering_durtles.wk.model.PitchInfo
import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Golden-JSON parity tests for [PitchInfo]'s bespoke 3-element array wire format
 * (`[reading|null, partOfSpeech|null, pitchNumber]`), proving the new kotlinx.serialization
 * [PitchInfoKotlinxSerializer] agrees with the legacy Jackson
 * [com.smouldering_durtles.wk.util.PitchInfoSerializer]/
 * [com.smouldering_durtles.wk.util.PitchInfoDeserializer] pair that
 * [com.smouldering_durtles.wk.db.Converters] still uses for the locally-cached pitch-accent data.
 */
class PitchInfoTest {
    @Test
    fun `kotlinx serializer produces the same 3-element array Jackson does`() {
        val pitchInfo = PitchInfo("たべる", "godan_verb", 2)

        val kotlinxJson = apiModelJson.encodeToString(PitchInfoKotlinxSerializer, pitchInfo)
        val jacksonJson = Converters.getObjectMapper().writeValueAsString(pitchInfo)

        assertEquals(jacksonJson, kotlinxJson)
    }

    @Test
    fun `kotlinx and Jackson both round trip a fixture with null reading and part of speech`() {
        val fixture = "[null,null,3]"

        val fromKotlinx = apiModelJson.decodeFromString(PitchInfoKotlinxSerializer, fixture)
        val fromJackson = Converters.getObjectMapper().readValue(fixture, PitchInfo::class.java)

        assertEquals(PitchInfo(null, null, 3), fromKotlinx)
        assertEquals(PitchInfo(null, null, 3), fromJackson)
    }

    @Test
    fun `kotlinx and Jackson both round trip a fixture with only part of speech null`() {
        val fixture = """["たべる",null,0]"""

        val fromKotlinx = apiModelJson.decodeFromString(PitchInfoKotlinxSerializer, fixture)
        val fromJackson = Converters.getObjectMapper().readValue(fixture, PitchInfo::class.java)

        assertEquals(fromJackson, fromKotlinx)
    }

    @Test
    fun `Jackson returns null for a malformed shape - wrong element count`() {
        val malformed = "[1,2]"
        val result = Converters.getObjectMapper().readValue(malformed, PitchInfo::class.java)
        assertNull(result)
    }

    @Test
    fun `Jackson returns null for a malformed shape - not an array`() {
        val malformed = "\"not an array\""
        val result = Converters.getObjectMapper().readValue(malformed, PitchInfo::class.java)
        assertNull(result)
    }

    @Test
    fun `kotlinx throws for a malformed shape - wrong element count`() {
        assertThrows(SerializationException::class.java) {
            apiModelJson.decodeFromString(PitchInfoKotlinxSerializer, "[1,2]")
        }
    }

    @Test
    fun `kotlinx throws for a malformed shape - not an array`() {
        assertThrows(SerializationException::class.java) {
            apiModelJson.decodeFromString(PitchInfoKotlinxSerializer, "\"not an array\"")
        }
    }

    @Test
    fun `kotlinx throws for a malformed shape - non-numeric pitch number`() {
        assertThrows(SerializationException::class.java) {
            apiModelJson.decodeFromString(PitchInfoKotlinxSerializer, """["たべる","godan_verb","two"]""")
        }
    }
}
