package com.smouldering_durtles.wk.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Phase 0 smoke check: proves kotlinx.serialization and Ktor (with the OkHttp engine) link and
 * work together as part of `:app`. Nothing here is wired into real networking or settings yet —
 * that's Phase 2. Safe to delete or replace once that work begins.
 */
@Serializable
data class NetworkSmokeMessage(val text: String)

fun networkSmokeRoundTrip(): NetworkSmokeMessage {
    val original = NetworkSmokeMessage(text = "kotlinx.serialization is wired up")
    val encoded = Json.encodeToString(NetworkSmokeMessage.serializer(), original)
    return Json.decodeFromString(NetworkSmokeMessage.serializer(), encoded)
}

fun networkSmokeClient(): HttpClient = HttpClient(OkHttp)
