package com.example.strive.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.strive.util.AlarmScheduler
import com.example.strive.repo.StriveRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class StriveBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            CoroutineScope(Dispatchers.IO).launch {
                val repo = StriveRepository.getInstance(context)
                repo.getAllHabits().forEach { habit ->
                    habit.reminderTimes.forEach { timeStr ->
                        AlarmScheduler.scheduleExactDailyAlarm(context, habit.id, timeStr)
                    }
                }
                // Reschedule mood notifications if enabled
                val s = repo.getSettings()
                if (s.notificationsAll && s.notificationsMood) {
                    val start = java.time.LocalTime.parse(s.moodStartTime)
                    val end = java.time.LocalTime.parse(s.moodEndTime)
                    val interval = s.moodIntervalMinutes.coerceAtLeast(1)
                    val times = generateAlarmTimes(start, end, interval)
                    AlarmScheduler.scheduleForHabit(context, "mood", times)
                }
                // Reschedule hydration notifications if enabled
                if (s.notificationsAll && s.notificationsHydration) {
                    val start = java.time.LocalTime.parse(s.hydrationStartTime)
                    val end = java.time.LocalTime.parse(s.hydrationEndTime)
                    val interval = s.hydrationIntervalMinutes.coerceAtLeast(1)
                    val times = generateAlarmTimes(start, end, interval)
                    AlarmScheduler.scheduleForHabit(context, "hydration", times)
                }
            }
        }
    }

    private fun generateAlarmTimes(start: java.time.LocalTime, end: java.time.LocalTime, interval: Int): List<String> {
        val times = mutableListOf<String>()
        var t = start
        
        // Generate all alarms based on user's interval - no limits
        while (!t.isAfter(end)) {
            times.add(t.toString().substring(0, 5))
            t = t.plusMinutes(interval.toLong())
        }
        
        return times
    }
}