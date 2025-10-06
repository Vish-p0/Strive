package com.example.strive.models

import kotlinx.serialization.Serializable

@Serializable
data class AppSettings constructor(
    val stepSensorEnabled: Boolean = false,
    val theme: String = "system", // "system","light","dark"
    val notificationChannels: Map<String, Boolean> = mapOf("hydration" to false, "general" to false),
    // New explicit notification toggles
    val notificationsAll: Boolean = false,
    val notificationsHabits: Boolean = false,
    val notificationsMood: Boolean = false,
    val notificationsHydration: Boolean = false,
    // Mood entry reminders window and interval
    val moodStartTime: String = "09:00", // HH:mm
    val moodEndTime: String = "21:00",   // HH:mm
    val moodIntervalMinutes: Int = 120,
    // Hydration reminders window and interval
    val hydrationStartTime: String = "08:00", // HH:mm
    val hydrationEndTime: String = "22:00",   // HH:mm
    val hydrationIntervalMinutes: Int = 60,
    // Widget settings
    val widgetSelectedHabitId: String = "", // Empty means no habit selected for widget
    // Onboarding completion flag
    val hasCompletedOnboarding: Boolean = false,
    val exportFormatVersion: Int = 1
)

