package com.example.strive.util

import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

object SerializationUtils {
    val json = Json {
        prettyPrint = false
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    inline fun <reified T> toJson(obj: T): String = json.encodeToString(obj)
    inline fun <reified T> fromJson(jsonStr: String): T = json.decodeFromString(jsonStr)
}

