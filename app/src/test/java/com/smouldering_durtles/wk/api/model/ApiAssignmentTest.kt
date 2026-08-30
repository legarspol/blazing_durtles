package com.smouldering_durtles.wk.api.model

import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * Golden-JSON parity tests for [ApiAssignment], covering a realistic full "data" payload for this
 * DTO as WaniKani's API actually sends it (the ID/object type live outside this object - see
 * [WaniKaniEntity] - so they aren't part of the fixture below).
 */
class ApiAssignmentTest {
    private val fixture = """
        {
          "created_at": "2021-04-05T08:00:00.000000Z",
          "subject_id": 8761,
          "subject_type": "vocabulary",
          "srs_stage": 5,
          "unlocked_at": "2021-04-05T08:00:00.000000Z",
          "started_at": "2021-04-05T08:05:00.000000Z",
          "passed_at": null,
          "burned_at": null,
          "available_at": "2021-04-10T08:05:00.000000Z",
          "resurrected_at": null,
          "hidden": false
        }
    """.trimIndent()

    @Test
    fun `deserializes a realistic assignment payload`() {
        val assignment = apiModelJson.decodeFromString(ApiAssignment.serializer(), fixture)

        assertEquals(8761L, assignment.subjectId)
        assertEquals(5L, assignment.srsStageId)
        assertFalse(assignment.unlockedAt == 0L)
        assertFalse(assignment.startedAt == 0L)
        assertEquals(0L, assignment.passedAt)
        assertEquals(0L, assignment.burnedAt)
        assertEquals(0L, assignment.resurrectedAt)
        assertFalse(assignment.availableAt == 0L)
        // id/object aren't part of the "data" payload - they're set separately by the caller.
        assertEquals(0L, assignment.id)
    }

    @Test
    fun `round trips through kotlinx serialization`() {
        val assignment = apiModelJson.decodeFromString(ApiAssignment.serializer(), fixture)
        val reEncoded = apiModelJson.encodeToString(ApiAssignment.serializer(), assignment)
        val reDecoded = apiModelJson.decodeFromString(ApiAssignment.serializer(), reEncoded)

        assertEquals(assignment, reDecoded)
    }

    @Test
    fun `ignores unknown fields in the payload`() {
        val withExtraField = """
            {
              "subject_id": 42,
              "srs_stage": 1,
              "unlocked_at": null,
              "started_at": null,
              "passed_at": null,
              "burned_at": null,
              "available_at": null,
              "resurrected_at": null,
              "totally_unrecognized_future_field": {"nested": [1, 2, 3]}
            }
        """.trimIndent()

        val assignment = apiModelJson.decodeFromString(ApiAssignment.serializer(), withExtraField)

        assertEquals(42L, assignment.subjectId)
        assertEquals(1L, assignment.srsStageId)
    }

    @Test
    fun `zero timestamps serialize as JSON null, matching the legacy Jackson wire format`() {
        val assignment = ApiAssignment(subjectId = 1L)
        val json = apiModelJson.encodeToString(ApiAssignment.serializer(), assignment)
        val obj = apiModelJson.parseToJsonElement(json).jsonObject

        assertEquals("null", obj["available_at"].toString())
        assertEquals("null", obj["burned_at"].toString())
        assertEquals("null", obj["passed_at"].toString())
        assertEquals("null", obj["resurrected_at"].toString())
        assertEquals("null", obj["started_at"].toString())
        assertEquals("null", obj["unlocked_at"].toString())
    }
}
