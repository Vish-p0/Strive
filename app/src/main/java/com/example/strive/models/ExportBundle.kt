package com.example.strive.models

import kotlinx.serialization.Serializable

@Serializable
data class ExportBundle constructor(
    val version: Int,
    val exportedAt: Long,
    val userProfile: UserProfile? = null,
    val habits: List<Habit> = emptyList(),
    val ticks: List<HabitTick> = emptyList(),
    val moods: List<MoodEntry> = emptyList(),
    val settings: AppSettings = AppSettings()
)

