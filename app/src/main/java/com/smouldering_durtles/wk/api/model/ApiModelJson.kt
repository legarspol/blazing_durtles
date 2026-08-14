package com.smouldering_durtles.wk.api.model

import kotlinx.serialization.json.Json

/**
 * Shared [Json] instance for (de)serializing the WaniKani API model classes in this package.
 *
 * Mirrors the leniency of the legacy Jackson `ObjectMapper` in `Converters` (which disabled
 * `FAIL_ON_UNKNOWN_PROPERTIES`): the WK API is free to add new response fields over time without
 * breaking parsing here. `coerceInputValues = true` mirrors the same leniency for an explicit JSON
 * `null` sent for a non-nullable field that has a default: Jackson silently fell back to the
 * default (e.g. the old setter for `PronunciationAudio.metadata`); without this flag,
 * kotlinx.serialization would instead throw a `SerializationException` at parse time.
 *
 * There is no Ktor content-negotiation wiring yet (that's a separate ticket), so this instance is
 * currently only consumed directly by callers that (de)serialize these DTOs by hand, and by tests.
 */
val apiModelJson: Json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}
