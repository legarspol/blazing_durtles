package com.smouldering_durtles.wk.api.model

import com.smouldering_durtles.wk.db.Converters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Golden-JSON / interop coverage for the rest of the `api/model` DTOs not covered by the more
 * detailed [ApiAssignmentTest]/[ApiSubjectTest]/[ApiUserTest]: the other [WaniKaniEntity]
 * implementers, the request DTOs mutated via setters by out-of-scope Java callers, and the two
 * classes ([ApiStage]/[ApiSrsSystem]) that are still (de)serialized through the legacy Jackson
 * `Converters` object mapper as well as kotlinx.
 */
class RemainingDtosTest {
    @Test
    fun `ApiLevelProgression round trips and keeps id out of the data payload`() {
        val fixture = """
            {"created_at":"2021-01-01T00:00:00.000000Z","unlocked_at":null,"started_at":null,
             "passed_at":null,"completed_at":null,"abandoned_at":null,"level":5}
        """.trimIndent()
        val progression = apiModelJson.decodeFromString(ApiLevelProgression.serializer(), fixture)
        assertEquals(5, progression.level)
        assertEquals(0L, progression.id)
        progression.setId(99L)
        assertEquals(99L, progression.id)

        val reDecoded = apiModelJson.decodeFromString(
            ApiLevelProgression.serializer(),
            apiModelJson.encodeToString(ApiLevelProgression.serializer(), progression)
        )
        assertEquals(progression.level, reDecoded.level)
    }

    @Test
    fun `ApiReviewStatistic round trips`() {
        val fixture = """
            {"meaning_correct":10,"meaning_incorrect":2,"meaning_max_streak":5,"meaning_current_streak":3,
             "reading_correct":9,"reading_incorrect":1,"reading_max_streak":6,"reading_current_streak":4,
             "percentage_correct":90,"subject_id":123}
        """.trimIndent()
        val stat = apiModelJson.decodeFromString(ApiReviewStatistic.serializer(), fixture)
        assertEquals(123L, stat.subjectId)
        assertEquals(90, stat.percentageCorrect)

        val reDecoded = apiModelJson.decodeFromString(
            ApiReviewStatistic.serializer(),
            apiModelJson.encodeToString(ApiReviewStatistic.serializer(), stat)
        )
        assertEquals(stat, reDecoded)
    }

    @Test
    fun `ApiStudyMaterial is mutable via setters and excludes id from the wire format`() {
        val material = ApiStudyMaterial()
        material.meaningNote = "note"
        material.readingNote = "reading note"
        material.meaningSynonyms = listOf("a", "b")
        material.subjectId = 55L
        material.setId(7L)

        assertEquals(7L, material.id)
        val json = apiModelJson.encodeToString(ApiStudyMaterial.serializer(), material)
        assertTrue(!json.contains("\"id\""))

        val reDecoded = apiModelJson.decodeFromString(ApiStudyMaterial.serializer(), json)
        assertEquals("note", reDecoded.meaningNote)
        assertEquals(listOf("a", "b"), reDecoded.meaningSynonyms)
        assertEquals(55L, reDecoded.subjectId)
        // id isn't part of the wire format, so it doesn't survive the round trip - matches the
        // original @JsonIgnore behavior.
        assertEquals(0L, reDecoded.id)
    }

    @Test
    fun `ApiStartAssignment has a Java-visible no-arg constructor and is mutable`() {
        val request = ApiStartAssignment()
        request.startedAt = 1_600_000_000_000L

        val json = apiModelJson.encodeToString(ApiStartAssignment.serializer(), request)
        val reDecoded = apiModelJson.decodeFromString(ApiStartAssignment.serializer(), json)
        assertEquals(request.startedAt, reDecoded.startedAt)
    }

    @Test
    fun `ApiCreateReview nested body is mutated through chained getters like the Java call sites do`() {
        val request = ApiCreateReview()
        request.review.subjectId = 42L
        request.review.incorrectMeaningAnswers = 1
        request.review.incorrectReadingAnswers = 2

        val json = apiModelJson.encodeToString(ApiCreateReview.serializer(), request)
        val reDecoded = apiModelJson.decodeFromString(ApiCreateReview.serializer(), json)
        assertEquals(42L, reDecoded.review.subjectId)
        assertEquals(1, reDecoded.review.incorrectMeaningAnswers)
        assertEquals(2, reDecoded.review.incorrectReadingAnswers)
    }

    @Test
    fun `ApiUpdateStudyMaterial nested study material is mutated through chained getters`() {
        val request = ApiUpdateStudyMaterial()
        request.studyMaterial.meaningNote = "note"
        request.studyMaterial.subjectId = 9L

        val json = apiModelJson.encodeToString(ApiUpdateStudyMaterial.serializer(), request)
        val reDecoded = apiModelJson.decodeFromString(ApiUpdateStudyMaterial.serializer(), json)
        assertEquals("note", reDecoded.studyMaterial.meaningNote)
        assertEquals(9L, reDecoded.studyMaterial.subjectId)
    }

    @Test
    fun `ApiStage and ApiSrsSystem stay Jackson-compatible for the Room JSON-blob round trip`() {
        val stages = listOf(
            ApiStage(position = 0L, interval = 0L, intervalUnit = null),
            ApiStage(position = 1L, interval = 14400L, intervalUnit = "seconds")
        )

        // Mirrors GetSrsSystemsTask/SrsSystemDefinition storing/reloading ApiStage lists as a JSON
        // blob in a Room column via the legacy Jackson object mapper.
        val jacksonJson = Converters.getObjectMapper().writeValueAsString(stages)
        val fromJackson = Converters.getObjectMapper().readValue(
            jacksonJson,
            Converters.getObjectMapper().typeFactory.constructCollectionType(List::class.java, ApiStage::class.java)
        ) as List<*>
        assertEquals(stages, fromJackson)

        // Direct field access, like LiveSrsSystems does.
        assertEquals(1L, stages[1].position)
        assertEquals(14400L, stages[1].interval)
        assertEquals("seconds", stages[1].intervalUnit)

        // Also still works via kotlinx.
        val kotlinxJson = apiModelJson.encodeToString(
            kotlinx.serialization.builtins.ListSerializer(ApiStage.serializer()),
            stages
        )
        val fromKotlinx = apiModelJson.decodeFromString(
            kotlinx.serialization.builtins.ListSerializer(ApiStage.serializer()),
            kotlinxJson
        )
        assertEquals(stages, fromKotlinx)
    }

    @Test
    fun `ApiSrsSystem exposes direct field access like the original Java class`() {
        val system = ApiSrsSystem(name = "Classic", description = "desc", unlockingStagePosition = 0L)
        system.setId(2L)
        assertEquals(2L, system.id)
        assertEquals("Classic", system.name)
    }

    @Test
    fun `ApiSummary and ApiSummarySession round trip`() {
        val fixture = """
            {"lessons":[{"available_at":null,"subject_ids":[1,2,3]}],
             "reviews":[{"available_at":"2021-01-01T00:00:00.000000Z","subject_ids":[]}]}
        """.trimIndent()
        val summary = apiModelJson.decodeFromString(ApiSummary.serializer(), fixture)
        assertEquals(listOf(1L, 2L, 3L), summary.lessons[0].subjectIds)
        assertEquals(0L, summary.lessons[0].availableAt)
        assertTrue(summary.reviews[0].availableAt != 0L)
    }
}
