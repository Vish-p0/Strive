package com.example.strive.models

import kotlinx.serialization.Serializable

@Serializable
data class UserProfile constructor(
    val name: String,
    val age: Int,
    val gender: String,        // "Male","Female","Other","PreferNotToSay"
    val avatarEmoji: String = "\uD83D\uDE03", // default 😃
    val createdAt: Long = System.currentTimeMillis()
)

