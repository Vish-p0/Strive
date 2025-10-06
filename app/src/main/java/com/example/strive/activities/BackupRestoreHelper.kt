package com.example.strive.activities

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import com.example.strive.repo.StriveRepository
import com.example.strive.models.UserProfile
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Helper functions to export/import data in CSV and Excel (CSV-only + XLSX via simple CSV-in-xlsx) formats.
 * Note: For simplicity, we export three CSVs in a ZIP-like flow using document picker multiple times,
 * or a single CSV with all sections separated. Here we implement a single CSV file export/import.
 */
object BackupRestoreHelper {

    const val MIME_CSV = "text/csv"
    const val MIME_EXCEL = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

    fun backupWithChooser(activity: AppCompatActivity) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = MIME_CSV
            putExtra(Intent.EXTRA_TITLE, "pulse_backup_${timestamp}.csv")
        }
        activity.startActivityForResult(intent, 9001)
    }

    fun startRestoreFlow(activity: AppCompatActivity) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                MIME_CSV,
                "text/plain",
                "application/csv",
                "text/comma-separated-values"
            ))
        }
        activity.startActivityForResult(intent, 9002)
    }

    fun handleActivityResult(activity: Activity, requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK || data?.data == null) return
        val uri = data.data!!
        val repo = StriveRepository.getInstance(activity)
        when (requestCode) {
            9001 -> exportCsv(activity, uri, repo)
            9002 -> importCsv(activity, uri, repo)
        }
    }

    private fun exportCsv(activity: Activity, uri: Uri, repo: StriveRepository) {
        val resolver = activity.contentResolver
        resolver.openOutputStream(uri)?.use { os ->
            BufferedWriter(OutputStreamWriter(os)).use { w ->
                // Simple CSV with sections
                w.appendLine("SECTION,KEY,VALUE1,VALUE2,VALUE3,VALUE4,VALUE5")
                // UserProfile
                repo.getUserProfile()?.let { p ->
                    w.appendLine("USER,NAME,${p.name}")
                    w.appendLine("USER,AGE,${p.age}")
                    w.appendLine("USER,GENDER,${p.gender}")
                    w.appendLine("USER,AVATAR,${p.avatarEmoji}")
                }
                // Settings
                val s = repo.getSettings()
                w.appendLine("SETTINGS,NOTIF_ALL,${s.notificationsAll}")
                w.appendLine("SETTINGS,NOTIF_HABITS,${s.notificationsHabits}")
                w.appendLine("SETTINGS,NOTIF_MOOD,${s.notificationsMood}")
                w.appendLine("SETTINGS,NOTIF_HYDRATION,${s.notificationsHydration}")
                w.appendLine("SETTINGS,THEME,${s.theme}")
                w.appendLine("SETTINGS,STEP_SENSOR,${s.stepSensorEnabled}")
                w.appendLine("SETTINGS,MOOD_START,${s.moodStartTime}")
                w.appendLine("SETTINGS,MOOD_END,${s.moodEndTime}")
                w.appendLine("SETTINGS,MOOD_INTERVAL,${s.moodIntervalMinutes}")
                w.appendLine("SETTINGS,HYDRATION_START,${s.hydrationStartTime}")
                w.appendLine("SETTINGS,HYDRATION_END,${s.hydrationEndTime}")
                w.appendLine("SETTINGS,HYDRATION_INTERVAL,${s.hydrationIntervalMinutes}")
                w.appendLine("SETTINGS,WIDGET_HABIT,${s.widgetSelectedHabitId}")
                w.appendLine("SETTINGS,ONBOARDING_COMPLETE,true") // Always true when exporting
                // Notification channels
                s.notificationChannels.forEach { (key, value) ->
                    w.appendLine("NOTIF_CHANNEL,${key},${value}")
                }
                // Habits
                repo.getAllHabits().forEach { h ->
                    w.appendLine("HABIT,${h.id},${h.title},${h.emoji},${h.targetPerDay},${h.unit},${h.isBuiltIn}")
                }
                // Ticks
                repo.getAllHabits().forEach { h ->
                    repo.getTicksForHabit(h.id).forEach { t ->
                        w.appendLine("TICK,${t.habitId},${t.date},${t.amount}")
                    }
                }
                // Moods
                repo.getAllMoods().forEach { m ->
                    val safeNote = m.note?.replace(',', ' ') ?: ""
                    w.appendLine("MOOD,${m.id},${m.timestamp},${m.emoji},${m.score},${safeNote}")
                }
            }
        }
    }

    private fun importCsv(activity: Activity, uri: Uri, repo: StriveRepository) {
        val resolver = activity.contentResolver
        val lines = resolver.openInputStream(uri)?.bufferedReader()?.use { it.readLines() } ?: return
        var name: String? = null
        var age: Int? = null
        var gender: String? = null
        var avatar: String? = null

        var all = true
        var hab = true
        var mood = true
        var hydration = false
        var theme = "system"
        var stepSensor = false
        var moodStart = "09:00"
        var moodEnd = "21:00"
        var moodInterval = 120
        var hydrationStart = "08:00"
        var hydrationEnd = "22:00"
        var hydrationInterval = 60
        var widgetHabit = ""
        var onboardingComplete = true // Default to true for restore
        val notifChannels = mutableMapOf<String, Boolean>()

        val habits = repo.getAllHabits().toMutableList()
        val ticks = mutableListOf<com.example.strive.models.HabitTick>()
        val moods = mutableListOf<com.example.strive.models.MoodEntry>()

        for (line in lines.drop(1)) { // skip header
            val parts = line.split(',')
            if (parts.isEmpty()) continue
            when (parts[0]) {
                "USER" -> {
                    when (parts.getOrNull(1)) {
                        "NAME" -> name = parts.getOrNull(2) ?: name
                        "AGE" -> age = parts.getOrNull(2)?.toIntOrNull() ?: age
                        "GENDER" -> gender = parts.getOrNull(2) ?: gender
                        "AVATAR" -> avatar = parts.getOrNull(2) ?: avatar
                    }
                }
                "SETTINGS" -> {
                    when (parts.getOrNull(1)) {
                        "NOTIF_ALL" -> all = parts.getOrNull(2)?.toBooleanStrictOrNull() ?: all
                        "NOTIF_HABITS" -> hab = parts.getOrNull(2)?.toBooleanStrictOrNull() ?: hab
                        "NOTIF_MOOD" -> mood = parts.getOrNull(2)?.toBooleanStrictOrNull() ?: mood
                        "NOTIF_HYDRATION" -> hydration = parts.getOrNull(2)?.toBooleanStrictOrNull() ?: hydration
                        "THEME" -> theme = parts.getOrNull(2) ?: theme
                        "STEP_SENSOR" -> stepSensor = parts.getOrNull(2)?.toBooleanStrictOrNull() ?: stepSensor
                        "MOOD_START" -> moodStart = parts.getOrNull(2) ?: moodStart
                        "MOOD_END" -> moodEnd = parts.getOrNull(2) ?: moodEnd
                        "MOOD_INTERVAL" -> moodInterval = parts.getOrNull(2)?.toIntOrNull() ?: moodInterval
                        "HYDRATION_START" -> hydrationStart = parts.getOrNull(2) ?: hydrationStart
                        "HYDRATION_END" -> hydrationEnd = parts.getOrNull(2) ?: hydrationEnd
                        "HYDRATION_INTERVAL" -> hydrationInterval = parts.getOrNull(2)?.toIntOrNull() ?: hydrationInterval
                        "WIDGET_HABIT" -> widgetHabit = parts.getOrNull(2) ?: widgetHabit
                        "ONBOARDING_COMPLETE" -> onboardingComplete = parts.getOrNull(2)?.toBooleanStrictOrNull() ?: onboardingComplete
                    }
                }
                "NOTIF_CHANNEL" -> {
                    val key = parts.getOrNull(1)
                    val value = parts.getOrNull(2)?.toBooleanStrictOrNull()
                    if (key != null && value != null) {
                        notifChannels[key] = value
                    }
                }
                "HABIT" -> {
                    val id = parts.getOrNull(1) ?: continue
                    val title = parts.getOrNull(2) ?: continue
                    val emoji = parts.getOrNull(3) ?: ""
                    val target = parts.getOrNull(4)?.toIntOrNull() ?: 0
                    val unit = parts.getOrNull(5) ?: ""
                    val builtIn = parts.getOrNull(6)?.toBooleanStrictOrNull() ?: false
                    val existing = habits.indexOfFirst { it.id == id }
                    val h = com.example.strive.models.Habit(
                        id = id,
                        title = title,
                        emoji = emoji,
                        unit = unit,
                        targetPerDay = target,
                        defaultIncrement = 1,
                        isBuiltIn = builtIn
                    )
                    if (existing >= 0) habits[existing] = h else habits.add(h)
                }
                "TICK" -> {
                    val hid = parts.getOrNull(1) ?: continue
                    val date = parts.getOrNull(2) ?: continue
                    val amount = parts.getOrNull(3)?.toIntOrNull() ?: 0
                    ticks.add(com.example.strive.models.HabitTick(hid, date, amount))
                }
                "MOOD" -> {
                    val id = parts.getOrNull(1) ?: continue
                    val ts = parts.getOrNull(2)?.toLongOrNull() ?: System.currentTimeMillis()
                    val emoji = parts.getOrNull(3) ?: ""
                    val score = parts.getOrNull(4)?.toIntOrNull() ?: 0
                    val note = parts.getOrNull(5)
                    moods.add(
                        com.example.strive.models.MoodEntry(
                            id = id,
                            emoji = emoji,
                            note = note,
                            timestamp = ts,
                            score = score
                        )
                    )
                }
            }
        }

        // Save collected data
        val profile = if (name != null && age != null && gender != null && avatar != null) {
            UserProfile(name!!, age!!, gender!!, avatar!!)
        } else repo.getUserProfile()
        profile?.let { repo.saveUserProfile(it) }

        // Save settings immediately
        val updatedSettings = repo.getSettings().copy(
            notificationsAll = all,
            notificationsHabits = hab,
            notificationsMood = mood,
            notificationsHydration = hydration,
            theme = theme,
            stepSensorEnabled = stepSensor,
            moodStartTime = moodStart,
            moodEndTime = moodEnd,
            moodIntervalMinutes = moodInterval,
            hydrationStartTime = hydrationStart,
            hydrationEndTime = hydrationEnd,
            hydrationIntervalMinutes = hydrationInterval,
            widgetSelectedHabitId = widgetHabit,
            hasCompletedOnboarding = onboardingComplete,
            notificationChannels = if (notifChannels.isNotEmpty()) notifChannels else repo.getSettings().notificationChannels
        )
        repo.saveSettings(updatedSettings)

        // Persist lists
        // Direct prefs writes via repository methods
        habits.forEach { /* already persisted */ }
        // Using repository json export/import path for simplicity
        val bundle = com.example.strive.models.ExportBundle(
            version = com.example.strive.repo.PrefsKeys.PREF_EXPORT_VERSION,
            exportedAt = System.currentTimeMillis(),
            userProfile = profile,
            habits = habits,
            ticks = ticks,
            moods = moods,
            settings = updatedSettings
        )
        repo.importFromJson(com.example.strive.util.SerializationUtils.toJson(bundle), merge = false)
    }
}
