package com.example.strive.activities

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.strive.R
import com.example.strive.models.UserProfile
import com.example.strive.repo.StriveRepository
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.navigationrail.NavigationRailView
import com.example.strive.sensors.StriveStepService

class ProfileActivity : AppCompatActivity() {

    private var bottomNav: BottomNavigationView? = null
    private var navRail: NavigationRailView? = null
    private lateinit var repo: StriveRepository

    // Views
    private lateinit var ivAvatarEmoji: TextView
    private lateinit var tvName: TextView
    private lateinit var tvAgeGender: TextView
    
    // Widget and step counter settings
    private lateinit var spinnerWidgetHabit: MaterialAutoCompleteTextView
    private lateinit var btnSaveWidget: com.google.android.material.button.MaterialButton
    private lateinit var switchStepSensor: MaterialSwitch

    companion object {
        private const val REQUEST_ACTIVITY_RECOGNITION = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        repo = StriveRepository.getInstance(this)

        bindViews()
        bindButtons()
        renderProfile()
        setupWidgetHabitDropdown()
        setupStepSensor()
        setupNavigation()
    }

    private fun setupNavigation() {
        bottomNav = findViewById(R.id.bottomNav)
        navRail = findViewById(R.id.navRail)
        
        val navigationListener = { item: MenuItem ->
            when (item.itemId) {
                R.id.menu_home -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                    true
                }
                R.id.menu_habits -> {
                    startActivity(Intent(this, HabitsActivity::class.java))
                    finish()
                    true
                }
                R.id.menu_mood -> {
                    startActivity(Intent(this, MoodActivity::class.java))
                    finish()
                    true
                }
                R.id.menu_profile -> {
                    // Already on profile, do nothing
                    true
                }
                else -> false
            }
        }
        
        bottomNav?.setOnItemSelectedListener(navigationListener)
        navRail?.setOnItemSelectedListener(navigationListener)
        
        // Set profile as selected
        bottomNav?.selectedItemId = R.id.menu_profile
        navRail?.selectedItemId = R.id.menu_profile
    }

    private fun bindViews() {
        ivAvatarEmoji = findViewById(R.id.ivAvatarEmoji)
        tvName = findViewById(R.id.tvName)
        tvAgeGender = findViewById(R.id.tvAgeGender)
        spinnerWidgetHabit = findViewById(R.id.spinnerWidgetHabit)
        btnSaveWidget = findViewById(R.id.btnSaveWidget)
        switchStepSensor = findViewById(R.id.switchStepSensor)
    }

    private fun bindButtons() {
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnEditProfile).setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        
        // Widget save button
        btnSaveWidget.setOnClickListener {
            val selectedText = spinnerWidgetHabit.text.toString()
            @Suppress("UNCHECKED_CAST")
            val habitIds = spinnerWidgetHabit.tag as? List<String> ?: emptyList()
            val habitOptions = mutableListOf<String>()
            
            // Recreate options list to find selected index
            habitOptions.add("None")
            val habits = repo.getAllHabits()
            habits.forEach { habit ->
                habitOptions.add("${habit.emoji} ${habit.title}")
            }
            if (habits.isEmpty()) {
                habitOptions.add("No habits available")
            }
            
            val selectedIndex = habitOptions.indexOf(selectedText)
            val selectedHabitId = if (selectedIndex >= 0 && selectedIndex < habitIds.size) {
                habitIds[selectedIndex]
            } else {
                ""
            }
            
            val cur = repo.getSettings()
            repo.saveSettings(cur.copy(widgetSelectedHabitId = selectedHabitId))
            
            // Update all widgets
            com.example.strive.widgets.StriveWidgetProvider.WidgetUpdateHelper.updateAllWidgets(this)
            
            val message = if (selectedHabitId.isEmpty()) {
                "Widget will show overall progress only"
            } else {
                val habit = habits.find { it.id == selectedHabitId }
                "Widget will show progress for: ${habit?.title ?: selectedText}"
            }
            com.google.android.material.snackbar.Snackbar.make(findViewById(android.R.id.content), message, com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun setupWidgetHabitDropdown() {
        val settings = repo.getSettings()
        val habits = repo.getAllHabits()
        val habitOptions = mutableListOf<String>()
        val habitIds = mutableListOf<String>()
        
        // Add "None" option
        habitOptions.add("None")
        habitIds.add("")
        
        // Add all habits
        habits.forEach { habit ->
            habitOptions.add("${habit.emoji} ${habit.title}")
            habitIds.add(habit.id)
        }
        
        // If no habits available, add a message
        if (habits.isEmpty()) {
            habitOptions.add("No habits available")
            habitIds.add("")
        }
        
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, habitOptions)
        spinnerWidgetHabit.setAdapter(adapter)
        
        // Set current selection
        val currentIndex = habitIds.indexOf(settings.widgetSelectedHabitId)
        if (currentIndex >= 0) {
            spinnerWidgetHabit.setText(habitOptions[currentIndex], false)
        } else {
            spinnerWidgetHabit.setText(habitOptions[0], false)
        }
        
        // Make dropdown show on click
        spinnerWidgetHabit.setOnClickListener {
            spinnerWidgetHabit.showDropDown()
        }
        
        // Store the mapping for later use
        spinnerWidgetHabit.tag = habitIds
    }

    private fun setupStepSensor() {
        val settings = repo.getSettings()
        switchStepSensor.isChecked = settings.stepSensorEnabled
        
        // Auto-start step service if enabled
        if (settings.stepSensorEnabled) {
            autoStartStriveStepServiceIfNeeded()
        }
        
        switchStepSensor.setOnCheckedChangeListener { _, isChecked ->
            repo.saveSettings(repo.getSettings().copy(stepSensorEnabled = isChecked))
            if (isChecked) {
                startStriveStepService()
            } else {
                stopStriveStepService()
            }
        }
    }

    private fun startStriveStepService() {
        android.util.Log.d("ProfileActivity", "Starting step service")
        
        // Check available sensors
        val sensorManager = getSystemService(android.content.Context.SENSOR_SERVICE) as android.hardware.SensorManager
        val stepCounterSensor = sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_STEP_COUNTER)
        val stepDetectorSensor = sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_STEP_DETECTOR)
        val accelerometerSensor = sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER)
        
        val sensorMessage = when {
            stepCounterSensor != null -> {
                android.util.Log.d("ProfileActivity", "Using hardware step counter sensor")
                "Step counting enabled (Hardware Step Counter)"
            }
            stepDetectorSensor != null -> {
                android.util.Log.d("ProfileActivity", "Using hardware step detector sensor")
                "Step counting enabled (Hardware Step Detector)"
            }
            accelerometerSensor != null -> {
                android.util.Log.d("ProfileActivity", "Using accelerometer for step detection")
                "Step counting enabled (Accelerometer Detection)"
            }
            else -> {
                android.util.Log.e("ProfileActivity", "No sensors available for step detection")
                switchStepSensor.isChecked = false
                repo.saveSettings(repo.getSettings().copy(stepSensorEnabled = false))
                "No step detection sensors available on this device"
            }
        }
        
        if (stepCounterSensor == null && stepDetectorSensor == null && accelerometerSensor == null) {
            com.google.android.material.snackbar.Snackbar.make(
                findViewById(android.R.id.content),
                sensorMessage,
                com.google.android.material.snackbar.Snackbar.LENGTH_LONG
            ).show()
            return
        }
        
        // Check for ACTIVITY_RECOGNITION permission (Android 10+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACTIVITY_RECOGNITION) 
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestActivityRecognitionPermission()
                return
            }
        }
        
        val intent = Intent(this, StriveStepService::class.java)
        startService(intent)
        
        com.google.android.material.snackbar.Snackbar.make(
            findViewById(android.R.id.content),
            sensorMessage,
            com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
        ).show()
    }

    private fun stopStriveStepService() {
        android.util.Log.d("ProfileActivity", "Stopping step service")
        
        val intent = Intent(this, StriveStepService::class.java)
        stopService(intent)
        
        com.google.android.material.snackbar.Snackbar.make(
            findViewById(android.R.id.content),
            "Step counting disabled",
            com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
        ).show()
    }

    private fun requestActivityRecognitionPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACTIVITY_RECOGNITION),
                REQUEST_ACTIVITY_RECOGNITION
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_ACTIVITY_RECOGNITION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    startStriveStepService()
                } else {
                    switchStepSensor.isChecked = false
                    repo.saveSettings(repo.getSettings().copy(stepSensorEnabled = false))
                    com.google.android.material.snackbar.Snackbar.make(
                        findViewById(android.R.id.content),
                        "Activity recognition permission required for step counting",
                        com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun autoStartStriveStepServiceIfNeeded() {
        // Check if device has step sensor
        val sensorManager = getSystemService(android.content.Context.SENSOR_SERVICE) as android.hardware.SensorManager
        val stepSensor = sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_STEP_COUNTER)
        
        if (stepSensor == null) {
            return // No step sensor available
        }
        
        // Check for ACTIVITY_RECOGNITION permission (Android 10+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACTIVITY_RECOGNITION) 
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                return // Permission not granted
            }
        }
        
        // Start the service silently
        val intent = Intent(this, StriveStepService::class.java)
        startService(intent)
    }

    private fun renderProfile() {
        val profile: UserProfile? = repo.getUserProfile()
        if (profile == null) {
            tvName.text = getString(R.string.profile_title)
            tvAgeGender.text = ""
            ivAvatarEmoji.text = "😃"
        } else {
            ivAvatarEmoji.text = profile.avatarEmoji
            tvName.text = profile.name
            tvAgeGender.text = "${profile.age}, ${profile.gender}"
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh in case profile was edited
        renderProfile()
    }
}