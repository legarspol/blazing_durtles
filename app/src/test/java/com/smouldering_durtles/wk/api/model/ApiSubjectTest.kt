package com.smouldering_durtles.wk.api.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Golden-JSON parity tests for [ApiSubject], covering a realistic full "data" payload (a
 * vocabulary subject) with all of its nested list fields - meanings, readings, auxiliary
 * meanings, context sentences, pronunciation audio and the various subject-ID lists. The ID/
 * object type live outside this object (see [WaniKaniEntity]), so they aren't part of the
 * fixture below.
 */
class ApiSubjectTest {
    private val fixture = """
        {
          "created_at": "2020-01-15T02:00:00.000000Z",
          "level": 3,
          "slug": "続ける",
          "hidden_at": null,
          "document_url": "https://www.wanikani.com/vocabulary/%E7%B6%9A%E3%81%91%E3%82%8B",
          "characters": "続ける",
          "meanings": [
            {"meaning": "To Continue", "primary": true, "accepted_answer": true},
            {"meaning": "To Keep Doing", "primary": false, "accepted_answer": true}
          ],
          "auxiliary_meanings": [
            {"meaning": "To Persist", "type": "whitelist"},
            {"meaning": "To Cease", "type": "blacklist"}
          ],
          "readings": [
            {"reading": "つづける", "primary": true, "accepted_answer": true, "type": null}
          ],
          "parts_of_speech": ["godan_verb", "transitive_verb"],
          "component_subject_ids": [467, 471],
          "amalgamation_subject_ids": [],
          "visually_similar_subject_ids": [],
          "meaning_mnemonic": "Some mnemonic text",
          "meaning_hint": null,
          "reading_mnemonic": "Some reading mnemonic",
          "reading_hint": null,
          "lesson_position": 12,
          "spaced_repetition_system_id": 2,
          "context_sentences": [
            {"en": "I will continue studying.", "ja": "勉強を続けます。"}
          ],
          "pronunciation_audios": [
            {
              "url": "https://files.wanikani.com/audio.mp3",
              "content_type": "audio/mpeg",
              "metadata": {
                "gender": "male",
                "pronunciation": "つづける",
                "source_id": 1,
                "voice_actor_id": 2,
                "voice_actor_name": "Kenichi",
                "voice_description": "Tokyo dialect"
              }
            }
          ]
        }
    """.trimIndent()

    @Test
    fun `deserializes a realistic vocabulary subject payload with nested lists`() {
        val subject = apiModelJson.decodeFromString(ApiSubject.serializer(), fixture)

        assertEquals(3, subject.level)
        assertEquals("続ける", subject.characters)
        assertEquals(2, subject.meanings.size)
        assertEquals("To Continue", subject.meanings[0].meaning)
        assertTrue(subject.meanings[0].isPrimary)
        assertEquals(2, subject.auxiliaryMeanings.size)
        assertTrue(subject.auxiliaryMeanings[0].isWhiteList())
        assertTrue(subject.auxiliaryMeanings[1].isBlackList())
        assertEquals(1, subject.readings.size)
        assertEquals("つづける", subject.readings[0].reading)
        assertEquals(listOf(467L, 471L), subject.componentSubjectIds)
        assertTrue(subject.amalgamationSubjectIds.isEmpty())
        assertEquals(listOf("godan_verb", "transitive_verb"), subject.partsOfSpeech)
        assertEquals(1, subject.contextSentences.size)
        assertEquals("I will continue studying.", subject.contextSentences[0].english)
        assertEquals(1, subject.pronunciationAudios.size)
        assertEquals("Kenichi", subject.pronunciationAudios[0].metadata.voiceActorName)
        assertTrue(subject.pronunciationAudios[0].metadata.isMale())
        assertFalse(subject.hiddenAt != 0L)
        // id/object aren't part of the "data" payload - they're set separately by the caller.
        assertEquals(0L, subject.id)
        assertEquals(null, subject.getObject())
    }

    @Test
    fun `round trips through kotlinx serialization`() {
        val subject = apiModelJson.decodeFromString(ApiSubject.serializer(), fixture)
        val reEncoded = apiModelJson.encodeToString(ApiSubject.serializer(), subject)
        val reDecoded = apiModelJson.decodeFromString(ApiSubject.serializer(), reEncoded)

        assertEquals(subject, reDecoded)
    }

    @Test
    fun `setId and setObject populate the WaniKaniEntity fields outside the data payload`() {
        val subject = apiModelJson.decodeFromString(ApiSubject.serializer(), fixture)
        subject.setId(8761L)
        subject.setObject("vocabulary")

        assertEquals(8761L, subject.id)
        assertEquals("vocabulary", subject.getObject())
    }

    @Test
    fun `ignores unknown fields in the payload`() {
        val withExtraField = """
            {
              "level": 1,
              "slug": "one",
              "characters": "一",
              "some_future_field_wanikani_might_add": "unexpected value"
            }
        """.trimIndent()

        val subject = apiModelJson.decodeFromString(ApiSubject.serializer(), withExtraField)

        assertEquals(1, subject.level)
        assertEquals("一", subject.characters)
    }

    @Test
    fun `coerces an explicit null for a non-nullable field with a default instead of throwing`() {
        val withExplicitNullMetadata = """
            {
              "level": 1,
              "slug": "one",
              "characters": "一",
              "pronunciation_audios": [
                {
                  "url": "https://files.wanikani.com/audio.mp3",
                  "content_type": "audio/mpeg",
                  "metadata": null
                }
              ]
            }
        """.trimIndent()

        val subject = apiModelJson.decodeFromString(ApiSubject.serializer(), withExplicitNullMetadata)

        assertEquals(PronunciationAudioMeta(), subject.pronunciationAudios[0].metadata)
    }
}
