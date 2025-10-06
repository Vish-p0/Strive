package com.example.strive.models

import kotlinx.serialization.Serializable

@Serializable
data class Habit constructor(
    val id: String,
    val title: String,
    val emoji: String,
    val unit: String,          // "COUNT","ML","LITERS","MINUTES","STEPS"
    val targetPerDay: Int,     // base unit (mL for water, mins for meditate, steps for steps)
    val defaultIncrement: Int,
    val color: String = "#2196F3", // Hex color code for the habit
    val isBuiltIn: Boolean = false,
    val isStarred: Boolean = false,
    val reminderTimes: List<String> = emptyList(), // "HH:mm"
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

