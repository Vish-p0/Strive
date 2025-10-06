package com.example.strive.receivers

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.example.strive.repo.StriveRepository
import com.example.strive.util.StriveNotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class StriveReminderReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TRIGGER_REMINDER = "com.example.strive.action.TRIGGER_REMINDER"
        const val EXTRA_HABIT_ID = "extra_habit_id"
        const val EXTRA_TIME = "extra_time"
        const val EXTRA_IS_MOOD = "extra_is_mood"
        const val EXTRA_IS_HYDRATION = "extra_is_hydration"

        const val ACTION_QUICK_ADD = "com.example.strive.action.QUICK_ADD"
        const val EXTRA_QUICK_HABIT_ID = "extra_quick_habit_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        android.util.Log.d("StriveReminderReceiver", "onReceive called with action: ${intent.action}")
        
        val intentAction = intent.action
        if (intentAction == ACTION_TRIGGER_REMINDER) {
            val habitId = intent.getStringExtra(EXTRA_HABIT_ID) ?: return
            val timeStr = intent.getStringExtra(EXTRA_TIME) ?: ""
            val isMood = intent.getBooleanExtra(EXTRA_IS_MOOD, false)
            
            android.util.Log.d("StriveReminderReceiver", "Triggering reminder for habit: $habitId, time: $timeStr, isMood: $isMood")
            val isHydration = intent.getBooleanExtra(EXTRA_IS_HYDRATION, false)
            CoroutineScope(Dispatchers.IO).launch {
                val repo = StriveRepository.getInstance(context)
                val habit = if (!isMood && !isHydration) repo.getHabit(habitId) else null

                val quickIntent = Intent(context, StriveReminderReceiver::class.java).apply {
                    setAction(ACTION_QUICK_ADD)
                    putExtra(EXTRA_QUICK_HABIT_ID, habitId)
                }
                val flags = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M)
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                else PendingIntent.FLAG_UPDATE_CURRENT

                val quickPending = PendingIntent.getBroadcast(context, habitId.hashCode(), quickIntent, flags)

                val builder = if (isMood) {
                    val title = "Mood Reminder"
                    val content = "Enter how you're feeling with an emoji 😁"
                    StriveNotificationHelper.createNotificationBuilder(context, habitId.hashCode(), StriveNotificationHelper.CHANNEL_MOOD)
                        .setContentTitle(title)
                        .setContentText(content)
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                } else if (isHydration) {
                    val title = "Hydration Reminder"
                    val content = "Time to drink water! Stay hydrated 💧"
                    StriveNotificationHelper.createNotificationBuilder(context, habitId.hashCode(), StriveNotificationHelper.CHANNEL_HYDRATION)
                        .setContentTitle(title)
                        .setContentText(content)
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                } else if (habitId == "test") {
                    val title = "Test Alarm"
                    val content = "Test alarm fired successfully! 🎉"
                    StriveNotificationHelper.createNotificationBuilder(context, habitId.hashCode(), StriveNotificationHelper.CHANNEL_GENERAL)
                        .setContentTitle(title)
                        .setContentText(content)
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                } else {
                    val title = habit!!.title
                    val content = if (timeStr.isNotBlank()) "Reminder: $timeStr" else "Reminder"
                    StriveNotificationHelper.createNotificationBuilder(context, habitId.hashCode())
                        .setContentTitle(title)
                        .setContentText(content)
                        .setSmallIcon(android.R.drawable.ic_dialog_info) // replace with app icon
                        .addAction(0, "Mark +${habit.defaultIncrementLabel()}", quickPending)
                }

                val hasPostNotifications =
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        androidx.core.content.ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.POST_NOTIFICATIONS
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    } else true

                if (hasPostNotifications) {
                    with(NotificationManagerCompat.from(context)) {
                        notify(habitId.hashCode(), builder.build())
                    }
                } else {
                    // Permission not granted; skip notifying to avoid SecurityException
                }
            }
        } else if (intentAction == ACTION_QUICK_ADD) {
            val habitId = intent.getStringExtra(EXTRA_QUICK_HABIT_ID) ?: return
            CoroutineScope(Dispatchers.IO).launch {
                val repo = StriveRepository.getInstance(context)
                val habit = repo.getHabit(habitId) ?: return@launch
                repo.addTick(habitId, habit.defaultIncrement)
                StriveNotificationHelper.cancelNotification(context, habitId.hashCode())
                com.example.strive.widgets.StriveWidgetProvider.WidgetUpdateHelper.updateAllWidgets(context)
            }
        }
    }
}

// extension helper
fun com.example.strive.models.Habit.defaultIncrementLabel(): String {
    return when (this.unit) {
        "ML" -> "${this.defaultIncrement} mL"
        "LITERS" -> "${this.defaultIncrement} L"
        "MINUTES" -> "${this.defaultIncrement} min"
        "STEPS" -> "${this.defaultIncrement} steps"
        else -> "${this.defaultIncrement}"
    }
}