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
