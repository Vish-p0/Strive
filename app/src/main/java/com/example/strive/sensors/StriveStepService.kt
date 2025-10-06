package com.example.strive.sensors

import android.Manifest
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.strive.repo.StriveRepository
import kotlin.math.sqrt

class StriveStepService : Service(), SensorEventListener {

    private var sensorManager: SensorManager? = null
    private var stepCounterSensor: Sensor? = null
    private var stepDetectorSensor: Sensor? = null
    private var accelerometerSensor: Sensor? = null
    
    // Hardware step sensor tracking
    private var lastStepCount: Float = 0f
    private var isUsingHardwareStepSensor = false
    
    // Accelerometer-based step detection
    private var isUsingAccelerometer = false
    private var lastAcceleration = 0f
    private var currentAcceleration = 0f
    private var lastMovement = 0f
    private var stepThreshold = 12f // Threshold for step detection
    private var stepCooldown = 300L // Minimum time between steps (ms)
    private var lastStepTime = 0L
    private var stepBuffer = mutableListOf<Long>()
    private val maxStepsPerSecond = 5 // Maximum realistic steps per second

    companion object {
        private const val TAG = "StriveStepService"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "StriveStepService created")
        
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        
        // Try to use hardware step sensors first (most accurate and power efficient)
        initializeHardwareStepSensors()
        
        // If no hardware step sensors, fall back to accelerometer
        if (!isUsingHardwareStepSensor) {
            initializeAccelerometerStepDetection()
        }
    }
    
    private fun initializeHardwareStepSensors() {
        // Try TYPE_STEP_COUNTER first (cumulative steps since boot)
        stepCounterSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        if (stepCounterSensor != null) {
            Log.d(TAG, "Hardware step counter sensor found")
            val registered = sensorManager?.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_NORMAL)
            if (registered == true) {
                isUsingHardwareStepSensor = true
                Log.d(TAG, "Successfully registered for step counter sensor")
                return
            }
        }
        
        // Try TYPE_STEP_DETECTOR as alternative (individual step events)
        stepDetectorSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        if (stepDetectorSensor != null) {
            Log.d(TAG, "Hardware step detector sensor found")
            val registered = sensorManager?.registerListener(this, stepDetectorSensor, SensorManager.SENSOR_DELAY_NORMAL)
            if (registered == true) {
                isUsingHardwareStepSensor = true
                Log.d(TAG, "Successfully registered for step detector sensor")
                return
            }
        }
        
        Log.d(TAG, "No hardware step sensors available")
    }
    
    private fun initializeAccelerometerStepDetection() {
        accelerometerSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (accelerometerSensor != null) {
            Log.d(TAG, "Accelerometer sensor found, using for step detection")
            val canUseFastest = canUseFastestSampling()
            val requestedDelay = if (canUseFastest) {
                SensorManager.SENSOR_DELAY_FASTEST
            } else {
                SensorManager.SENSOR_DELAY_GAME
            }

            val registered = sensorManager?.registerListener(this, accelerometerSensor, requestedDelay)
            if (registered == true) {
                isUsingAccelerometer = true
                Log.d(TAG, "Successfully registered for accelerometer sensor with delay=$requestedDelay")

                if (!canUseFastest) {
                    Log.d(TAG, "Falling back to SENSOR_DELAY_GAME due to missing high sampling permission")
                }
                
                // Initialize accelerometer values
                lastAcceleration = SensorManager.GRAVITY_EARTH
                currentAcceleration = SensorManager.GRAVITY_EARTH
            } else {
                Log.e(TAG, "Failed to register accelerometer sensor")
            }
        } else {
            Log.e(TAG, "No accelerometer sensor available - cannot detect steps")
        }
    }

    private fun canUseFastestSampling(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.HIGH_SAMPLING_RATE_SENSORS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "StriveStepService destroyed")
        sensorManager?.unregisterListener(this)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.d(TAG, "Sensor accuracy changed: ${sensor?.name} - accuracy: $accuracy")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let { sensorEvent ->
            when (sensorEvent.sensor?.type) {
                Sensor.TYPE_STEP_COUNTER -> handleStepCounterEvent(sensorEvent)
                Sensor.TYPE_STEP_DETECTOR -> handleStepDetectorEvent(sensorEvent)
                Sensor.TYPE_ACCELEROMETER -> handleAccelerometerEvent(sensorEvent)
            }
        }
    }
    
    private fun handleStepCounterEvent(event: SensorEvent) {
        val count = event.values[0]
        Log.d(TAG, "Step counter event: $count total steps")
        
        val increment = if (lastStepCount == 0f) {
            // First reading, don't add any steps
            0
        } else {
            (count - lastStepCount).toInt()
        }
        
        lastStepCount = count
        
        if (increment > 0) {
            Log.d(TAG, "Adding $increment steps to habit")
            addStepsToHabit(increment)
        }
    }
    
    private fun handleStepDetectorEvent(event: SensorEvent) {
        // TYPE_STEP_DETECTOR triggers once per step
        Log.d(TAG, "Step detector event: step detected")
        addStepsToHabit(1)
    }
    
    private fun handleAccelerometerEvent(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        
        // Calculate the magnitude of acceleration
        val acceleration = sqrt(x * x + y * y + z * z)
        
        // Apply smoothing filter
        lastAcceleration = currentAcceleration
        currentAcceleration = acceleration * 0.1f + lastAcceleration * 0.9f
        
        // Calculate the difference from the smoothed acceleration
        val movement = Math.abs(acceleration - currentAcceleration)
        
        // Detect step pattern
        if (movement > stepThreshold) {
            val currentTime = System.currentTimeMillis()
            
            // Check cooldown period to avoid double-counting
            if (currentTime - lastStepTime > stepCooldown) {
                // Additional validation: check step frequency
                if (isValidStepTiming(currentTime)) {
                    Log.d(TAG, "Accelerometer step detected - movement: $movement, threshold: $stepThreshold")
                    addStepsToHabit(1)
                    lastStepTime = currentTime
                } else {
                    Log.d(TAG, "Step rejected - too frequent (movement: $movement)")
                }
            }
        }
    }
    
    private fun isValidStepTiming(currentTime: Long): Boolean {
        // Clean old entries (older than 1 second)
        stepBuffer.removeAll { it < currentTime - 1000 }
        
        // Check if we're under the maximum steps per second
        if (stepBuffer.size >= maxStepsPerSecond) {
            return false
        }
        
        stepBuffer.add(currentTime)
        return true
    }
    
    private fun addStepsToHabit(stepCount: Int) {
        try {
            val repo = StriveRepository.getInstance(this)
            repo.addTick("habit_steps", stepCount)
            Log.d(TAG, "Successfully added $stepCount steps to habit_steps")
            
            // Update widget
            com.example.strive.widgets.StriveWidgetProvider.WidgetUpdateHelper.updateAllWidgets(this)
            Log.d(TAG, "Widget updated")
        } catch (e: Exception) {
            Log.e(TAG, "Error adding steps to habit: ${e.message}", e)
        }
    }
}