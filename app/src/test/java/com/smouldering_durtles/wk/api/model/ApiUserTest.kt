package com.smouldering_durtles.wk.api.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Golden-JSON parity tests for [ApiUser], covering a realistic full "data" payload for the
 * `/v2/user` endpoint, including the nested [ApiSubscription] and the vacation-mode timestamp.
 */
class ApiUserTest {
    private val fixture = """
        {
          "id": "5a1e159a-93b9-4d38-8a34-40ee6dcda2bb",
          "username": "wkuser",
          "level": 12,
          "max_level_granted_by_subscription": 60,
          "current_vacation_started_at": null,
          "subscription": {
            "active": true,
            "type": "recurring",
            "max_level_granted": 60,
            "period_ends_at": "2022-06-15T02:00:00.000000Z"
          }
        }
    """.trimIndent()

    @Test
    fun `deserializes a realistic user payload`() {
        val user = apiModelJson.decodeFromString(ApiUser.serializer(), fixture)

        assertEquals("5a1e159a-93b9-4d38-8a34-40ee6dcda2bb", user.id)
        assertEquals("wkuser", user.username)
        assertEquals(12, user.level)
        assertEquals(60, user.maxLevelGrantedBySubscription)
        assertEquals(0L, user.currentVacationStartedAt)
        assertNotNull(user.subscription)
        assertEquals(60, user.subscription?.maxLevelGranted)
    }

    @Test
    fun `round trips through kotlinx serialization`() {
        val user = apiModelJson.decodeFromString(ApiUser.serializer(), fixture)
        val reEncoded = apiModelJson.encodeToString(ApiUser.serializer(), user)
        val reDecoded = apiModelJson.decodeFromString(ApiUser.serializer(), reEncoded)

        assertEquals(user, reDecoded)
    }

    @Test
    fun `vacation mode timestamp round trips when set`() {
        val fixtureOnVacation = """
            {
              "id": "abc",
              "username": "wkuser",
              "level": 1,
              "max_level_granted_by_subscription": 3,
              "current_vacation_started_at": "2021-11-01T00:00:00.000000Z",
              "subscription": null
            }
        """.trimIndent()

        val user = apiModelJson.decodeFromString(ApiUser.serializer(), fixtureOnVacation)

        assertEquals(false, user.currentVacationStartedAt == 0L)
        assertEquals(null, user.subscription)
    }

    @Test
    fun `ignores unknown fields in the payload`() {
        val withExtraField = """
            {
              "id": "abc",
              "username": "wkuser",
              "level": 1,
              "max_level_granted_by_subscription": 3,
              "current_vacation_started_at": null,
              "subscription": null,
              "profile_url": "https://www.wanikani.com/users/wkuser"
            }
        """.trimIndent()

        val user = apiModelJson.decodeFromString(ApiUser.serializer(), withExtraField)

        assertEquals("wkuser", user.username)
    }
}
