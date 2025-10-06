package com.example.strive.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.strive.R
import com.example.strive.models.AppSettings
import com.example.strive.repo.StriveRepository
import com.example.strive.sensors.StriveStepService
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import android.widget.ArrayAdapter
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SettingsActivity : AppCompatActivity() {

    private lateinit var repo: StriveRepository

    // New expandable card toggles
    private lateinit var switchMoodNotifications: MaterialSwitch
    private lateinit var switchHydrationNotifications: MaterialSwitch
    private lateinit var layoutMoodSettings: android.view.View
    private lateinit var layoutHydrationSettings: android.view.View
    private lateinit var cardMoodNotifications: com.google.android.material.card.MaterialCardView
    private lateinit var cardHydrationNotifications: com.google.android.material.card.MaterialCardView

    private lateinit var etMoodStart: TextInputEditText
    private lateinit var etMoodInterval: TextInputEditText
    private lateinit var etMoodEnd: TextInputEditText
    private lateinit var btnSaveMood: com.google.android.material.button.MaterialButton

    private lateinit var etHydrationStart: TextInputEditText
    private lateinit var etHydrationInterval: TextInputEditText
    private lateinit var etHydrationEnd: TextInputEditText
    private lateinit var btnSaveHydration: com.google.android.material.button.MaterialButton

    private lateinit var btnBackup: com.google.android.material.button.MaterialButton
    private lateinit var btnRestore: com.google.android.material.button.MaterialButton
    private lateinit var btnTestAlarm: com.google.android.material.button.MaterialButton
    private lateinit var btnDeleteData: com.google.android.material.button.MaterialButton

    private var bottomNav: BottomNavigationView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        repo = StriveRepository.getInstance(this)

        bindViews()
        render()
        bindActions()
        setupToolbar()
        setupBottomNav()
    }

    private fun bindViews() {
        // Expandable card views
        switchMoodNotifications = findViewById(R.id.switchMoodNotifications)
        switchHydrationNotifications = findViewById(R.id.switchHydrationNotifications)
        layoutMoodSettings = findViewById(R.id.layoutMoodSettings)
        layoutHydrationSettings = findViewById(R.id.layoutHydrationSettings)
        cardMoodNotifications = findViewById(R.id.cardMoodNotifications)
        cardHydrationNotifications = findViewById(R.id.cardHydrationNotifications)

        etMoodStart = findViewById(R.id.etMoodStart)
        etMoodInterval = findViewById(R.id.etMoodInterval)
        etMoodEnd = findViewById(R.id.etMoodEnd)
        btnSaveMood = findViewById(R.id.btnSaveMoodSchedule)

        etHydrationStart = findViewById(R.id.etHydrationStart)
        etHydrationInterval = findViewById(R.id.etHydrationInterval)
        etHydrationEnd = findViewById(R.id.etHydrationEnd)
        btnSaveHydration = findViewById(R.id.btnSaveHydrationSchedule)

        btnBackup = findViewById(R.id.btnBackup)
        btnRestore = findViewById(R.id.btnRestore)
        btnTestAlarm = findViewById(R.id.btnTestAlarm)
        btnDeleteData = findViewById(R.id.btnDeleteData)
        bottomNav = findViewById(R.id.bottomNav)
    }

    private fun render() {
        val s = repo.getSettings()
        
        // Set toggle states
        switchMoodNotifications.isChecked = s.notificationsMood
        switchHydrationNotifications.isChecked = s.notificationsHydration

        // Set expandable layouts visibility based on toggle state
        layoutMoodSettings.visibility = if (s.notificationsMood) android.view.View.VISIBLE else android.view.View.GONE
        layoutHydrationSettings.visibility = if (s.notificationsHydration) android.view.View.VISIBLE else android.view.View.GONE

        // Set default values
        etMoodStart.setText(s.moodStartTime)
        etMoodEnd.setText(s.moodEndTime)
        etMoodInterval.setText(s.moodIntervalMinutes.toString())

        etHydrationStart.setText(s.hydrationStartTime)
        etHydrationEnd.setText(s.hydrationEndTime)
        etHydrationInterval.setText(s.hydrationIntervalMinutes.toString())
    }

    private fun bindActions() {
        // Mood notifications toggle with expand/collapse
        switchMoodNotifications.setOnCheckedChangeListener { _, isChecked ->
            // Save state
            repo.saveSettings(repo.getSettings().copy(notificationsMood = isChecked))
            
            // Animate expand/collapse
            if (isChecked) {
                layoutMoodSettings.visibility = android.view.View.VISIBLE
                rescheduleMoodAlarms()
            } else {
                layoutMoodSettings.visibility = android.view.View.GONE
                cancelMoodAlarms()
            }
        }

        // Hydration notifications toggle with expand/collapse
        switchHydrationNotifications.setOnCheckedChangeListener { _, isChecked ->
            // Save state
            repo.saveSettings(repo.getSettings().copy(notificationsHydration = isChecked))
            
            // Animate expand/collapse
            if (isChecked) {
                layoutHydrationSettings.visibility = android.view.View.VISIBLE
                rescheduleHydrationAlarms()
            } else {
                layoutHydrationSettings.visibility = android.view.View.GONE
                cancelHydrationAlarms()
            }
        }

        // Make cards clickable to toggle
        cardMoodNotifications.setOnClickListener {
            switchMoodNotifications.isChecked = !switchMoodNotifications.isChecked
        }

        cardHydrationNotifications.setOnClickListener {
            switchHydrationNotifications.isChecked = !switchHydrationNotifications.isChecked
        }

        etMoodStart.setOnClickListener { pickTime(etMoodStart) }
        etMoodEnd.setOnClickListener { pickTime(etMoodEnd) }

        btnSaveMood.setOnClickListener {
            val cur = repo.getSettings()
            val interval = etMoodInterval.text?.toString()?.toIntOrNull()?.coerceAtLeast(1) ?: cur.moodIntervalMinutes
            repo.saveSettings(cur.copy(
                moodStartTime = etMoodStart.text?.toString() ?: cur.moodStartTime,
                moodEndTime = etMoodEnd.text?.toString() ?: cur.moodEndTime,
                moodIntervalMinutes = interval
            ))
            rescheduleMoodAlarms()
            com.google.android.material.snackbar.Snackbar.make(findViewById(android.R.id.content), "Mood schedule saved", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show()
        }

        etHydrationStart.setOnClickListener { pickTime(etHydrationStart) }
        etHydrationEnd.setOnClickListener { pickTime(etHydrationEnd) }

        btnSaveHydration.setOnClickListener {
            val cur = repo.getSettings()
            val interval = etHydrationInterval.text?.toString()?.toIntOrNull()?.coerceAtLeast(1) ?: cur.hydrationIntervalMinutes
            repo.saveSettings(cur.copy(
                hydrationStartTime = etHydrationStart.text?.toString() ?: cur.hydrationStartTime,
                hydrationEndTime = etHydrationEnd.text?.toString() ?: cur.hydrationEndTime,
                hydrationIntervalMinutes = interval
            ))
            rescheduleHydrationAlarms()
            com.google.android.material.snackbar.Snackbar.make(findViewById(android.R.id.content), "Hydration schedule saved", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show()
        }

        btnBackup.setOnClickListener { BackupRestoreHelper.backupWithChooser(this) }
        btnRestore.setOnClickListener { BackupRestoreHelper.startRestoreFlow(this) }
        btnTestAlarm.setOnClickListener { 
            testAlarm()
        }
        btnDeleteData.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_data_title)
                .setMessage(R.string.delete_data_message)
                .setPositiveButton(R.string.delete) { _, _ ->
                    repo.resetAll()
                    Toast.makeText(
                        this,
                        getString(R.string.delete_data_success),
                        Toast.LENGTH_LONG
                    ).show()

                    val restartIntent = Intent(this, SignupActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(restartIntent)
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }

    private fun setupToolbar() {
        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupBottomNav() {
        bottomNav?.apply {
            // Pre-select without triggering navigation
            selectedItemId = R.id.menu_profile

            setOnItemSelectedListener { item ->
                if (item.itemId == selectedItemId) return@setOnItemSelectedListener true
                when (item.itemId) {
                    R.id.menu_home -> {
                        startActivity(Intent(this@SettingsActivity, HomeActivity::class.java))
                        finish()
                        true
                    }
                    R.id.menu_habits -> {
                        startActivity(Intent(this@SettingsActivity, HabitsActivity::class.java))
                        finish()
                        true
                    }
                    R.id.menu_mood -> {
                        startActivity(Intent(this@SettingsActivity, MoodActivity::class.java))
                        finish()
                        true
                    }
                    R.id.menu_profile -> {
                        startActivity(Intent(this@SettingsActivity, ProfileActivity::class.java))
                        finish()
                        true
                    }
                    else -> false
                }
            }
        }
    }

    private fun pickTime(target: TextInputEditText) {
        val parts = (target.text?.toString() ?: "09:00").split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 9
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val dialog = android.app.TimePickerDialog(this, { _, h, m ->
            val v = String.format("%02d:%02d", h, m)
            target.setText(v)
        }, hour, minute, true)
        dialog.show()
    }

    private fun cancelMoodAlarms() {
        // Cancel all mood notification alarms when toggle is turned off
        com.example.strive.util.AlarmScheduler.cancelAlarmsForHabit(this, "mood")
    }

    private fun cancelHydrationAlarms() {
        // Cancel all hydration notification alarms when toggle is turned off
        com.example.strive.util.AlarmScheduler.cancelAlarmsForHabit(this, "hydration")
    }

    private fun rescheduleMoodAlarms() {
        android.util.Log.d("SettingsActivity", "rescheduleMoodAlarms called")
        
        val s = repo.getSettings()
        if (!s.notificationsMood) {
            android.util.Log.d("SettingsActivity", "Mood notifications disabled - notificationsMood: ${s.notificationsMood}")
            return
        }
        val start = java.time.LocalTime.parse(s.moodStartTime)
        val end = java.time.LocalTime.parse(s.moodEndTime)
        val interval = s.moodIntervalMinutes.coerceAtLeast(1)
        val times = generateAlarmTimes(start, end, interval)
        
        android.util.Log.d("SettingsActivity", "Generated ${times.size} mood alarm times: $times")
        
        com.example.strive.util.AlarmScheduler.scheduleForHabit(this, "mood", times)
    }

    private fun rescheduleHydrationAlarms() {
        android.util.Log.d("SettingsActivity", "rescheduleHydrationAlarms called")
        
        val s = repo.getSettings()
        if (!s.notificationsHydration) {
            android.util.Log.d("SettingsActivity", "Hydration notifications disabled - notificationsHydration: ${s.notificationsHydration}")
            return
        }
        val start = java.time.LocalTime.parse(s.hydrationStartTime)
        val end = java.time.LocalTime.parse(s.hydrationEndTime)
        val interval = s.hydrationIntervalMinutes.coerceAtLeast(1)
        val times = generateAlarmTimes(start, end, interval)
        
        android.util.Log.d("SettingsActivity", "Generated ${times.size} hydration alarm times: $times")
        
        com.example.strive.util.AlarmScheduler.scheduleForHabit(this, "hydration", times)
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

    override fun onPause() {
        super.onPause()
        // Auto-persist current values so they remain when returning
        val cur = repo.getSettings()
        val moodInterval = etMoodInterval.text?.toString()?.toIntOrNull() ?: cur.moodIntervalMinutes
        val hydrationInterval = etHydrationInterval.text?.toString()?.toIntOrNull() ?: cur.hydrationIntervalMinutes
        val updated = cur.copy(
            moodStartTime = etMoodStart.text?.toString() ?: cur.moodStartTime,
            moodEndTime = etMoodEnd.text?.toString() ?: cur.moodEndTime,
            moodIntervalMinutes = moodInterval.coerceAtLeast(1),
            hydrationStartTime = etHydrationStart.text?.toString() ?: cur.hydrationStartTime,
            hydrationEndTime = etHydrationEnd.text?.toString() ?: cur.hydrationEndTime,
            hydrationIntervalMinutes = hydrationInterval.coerceAtLeast(1)
        )
        if (updated != cur) {
            repo.saveSettings(updated)
            rescheduleMoodAlarms()
            rescheduleHydrationAlarms()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        BackupRestoreHelper.handleActivityResult(this, requestCode, resultCode, data)
        
        // Show feedback for successful restore
        if (requestCode == 9002 && resultCode == RESULT_OK) { // 9002 is restore request code
            val profile = repo.getUserProfile()
            if (profile != null) {
                android.widget.Toast.makeText(this, "Backup restored successfully! Your data has been updated.", android.widget.Toast.LENGTH_LONG).show()
            }
        }
        
        render()
    }

    private fun testAlarm() {
        android.util.Log.d("SettingsActivity", "Test alarm button pressed")
        
        // Schedule an alarm for 1 minute from now
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.MINUTE, 1)
        val timeStr = String.format("%02d:%02d", calendar.get(java.util.Calendar.HOUR_OF_DAY), calendar.get(java.util.Calendar.MINUTE))
        
        android.util.Log.d("SettingsActivity", "Scheduling test alarm for: $timeStr")
        
        com.example.strive.util.AlarmScheduler.scheduleExactDailyAlarm(this, "test", timeStr)
        
        com.google.android.material.snackbar.Snackbar.make(
            findViewById(android.R.id.content), 
            "Test alarm scheduled for $timeStr", 
            com.google.android.material.snackbar.Snackbar.LENGTH_LONG
        ).show()
    }
}
