package com.example.strive.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.strive.receivers.StriveReminderReceiver
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object AlarmScheduler {

    // Create a unique request code for a habit/time pair
    private fun makeRequestCode(habitId: String, timeStr: String): Int {
        // timeStr format "HH:mm"
        return (habitId.hashCode() xor timeStr.hashCode())
    }

    // Schedule a single exact alarm for a given local time (HH:mm) for the next occurrence
    fun scheduleExactDailyAlarm(context: Context, habitId: String, timeStr: String) {
        android.util.Log.d("AlarmScheduler", "Scheduling alarm for habit: $habitId at time: $timeStr")
        
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        val localTime = LocalTime.parse(timeStr, formatter)
        val zone = ZoneId.systemDefault()

        var triggerDateTime = LocalDateTime.of(LocalDate.now(zone), localTime)
        if (triggerDateTime.isBefore(LocalDateTime.now(zone))) {
            triggerDateTime = triggerDateTime.plusDays(1)
        }
        val zoned = ZonedDateTime.of(triggerDateTime, zone)
        val triggerMillis = zoned.toInstant().toEpochMilli()

        android.util.Log.d("AlarmScheduler", "Trigger time: ${java.util.Date(triggerMillis)}")

        val intent = Intent(context, StriveReminderReceiver::class.java).apply {
            action = StriveReminderReceiver.ACTION_TRIGGER_REMINDER
            putExtra(StriveReminderReceiver.EXTRA_HABIT_ID, habitId)
            putExtra(StriveReminderReceiver.EXTRA_TIME, timeStr)
            // Treat habitId == "mood" as mood reminder, "hydration" as hydration reminder
            putExtra(StriveReminderReceiver.EXTRA_IS_MOOD, habitId == "mood")
            putExtra(StriveReminderReceiver.EXTRA_IS_HYDRATION, habitId == "hydration")
        }

        val reqCode = makeRequestCode(habitId, timeStr)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        else PendingIntent.FLAG_UPDATE_CURRENT

        val pending = PendingIntent.getBroadcast(context, reqCode, intent, flags)

        // From Android 12 (S), apps must hold SCHEDULE_EXACT_ALARM/USE_EXACT_ALARM to set exact alarms.
        // If missing, gracefully fallback to a non-exact alarm to avoid crashing.
        val canScheduleExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else true

        android.util.Log.d("AlarmScheduler", "Can schedule exact alarms: $canScheduleExact")

        if (canScheduleExact) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pending)
            android.util.Log.d("AlarmScheduler", "Scheduled exact alarm for $habitId at $timeStr")
        } else {
            // Best-effort fallback (inexact). Consider prompting user to grant exact alarm allowance in app settings.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pending)
            android.util.Log.w("AlarmScheduler", "Scheduled inexact alarm for $habitId at $timeStr (exact alarms not permitted)")
        }
    }

    fun cancelScheduledAlarm(context: Context, habitId: String, timeStr: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, StriveReminderReceiver::class.java).apply {
            action = StriveReminderReceiver.ACTION_TRIGGER_REMINDER
            putExtra(StriveReminderReceiver.EXTRA_HABIT_ID, habitId)
            putExtra(StriveReminderReceiver.EXTRA_TIME, timeStr)
        }
        val reqCode = makeRequestCode(habitId, timeStr)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        else PendingIntent.FLAG_UPDATE_CURRENT
        val pending = PendingIntent.getBroadcast(context, reqCode, intent, flags)
        alarmManager.cancel(pending)
    }

    // Cancel all existing alarms for a specific habit
    fun cancelAlarmsForHabit(context: Context, habitId: String) {
        // We need to cancel potential alarms - since we don't know all the time strings,
        // we'll cancel a reasonable range of possible times
        for (hour in 0..23) {
            for (minute in arrayOf(0, 15, 30, 45)) {  // Common intervals
                val timeStr = String.format("%02d:%02d", hour, minute)
                cancelScheduledAlarm(context, habitId, timeStr)
            }
        }
    }

    // Cancel all alarms for special reminder types (mood, hydration)
    fun cancelAllSpecialAlarms(context: Context) {
        cancelAlarmsForHabit(context, "mood")
        cancelAlarmsForHabit(context, "hydration")
    }

    // Helper to (re)schedule all alarms for a given habit's reminderTimes
    fun scheduleForHabit(context: Context, habitId: String, reminderTimes: List<String>) {
        // First cancel existing alarms for this habit to prevent accumulation
        cancelAlarmsForHabit(context, habitId)
        
        // Schedule all reminders without limits
        reminderTimes.forEach { timeStr ->
            scheduleExactDailyAlarm(context, habitId, timeStr)
        }
    }
}
