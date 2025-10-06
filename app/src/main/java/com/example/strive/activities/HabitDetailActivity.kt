package com.example.strive.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.strive.R
import com.example.strive.models.Habit
import com.example.strive.repo.StriveRepository
import com.example.strive.util.DateUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.example.strive.views.HabitProgressChartView
import com.example.strive.fragments.AddHabitBottomSheetFragment
import java.util.*

class HabitDetailActivity : AppCompatActivity() {

    private lateinit var repository: StriveRepository
    private lateinit var habit: Habit
    
    // UI Components
    private lateinit var btnBack: ImageButton
    private lateinit var btnFavorite: ImageButton
    private lateinit var tvEmoji: TextView
    private lateinit var tvHabitTitle: TextView
    private lateinit var tvHabitType: TextView
    private lateinit var viewHabitColor: View
    private lateinit var tvStreakNumber: TextView
    private lateinit var tvProgressLarge: TextView
    private lateinit var tvProgressPercent: TextView
    private lateinit var progressBar: ProgressBar
    
    // Quick action buttons
    private lateinit var btnQuick1: MaterialButton
    private lateinit var btnQuick2: MaterialButton
    private lateinit var btnQuick3: MaterialButton
    private lateinit var btnManualEntry: MaterialButton
    
    // Meditation timer components
    private lateinit var cardMeditationTimer: MaterialCardView
    private lateinit var chipGroupDuration: ChipGroup
    private lateinit var tilCustomDuration: TextInputLayout
    private lateinit var etCustomDuration: TextInputEditText
    private lateinit var tvTimerDisplay: TextView
    private lateinit var btnStartPause: MaterialButton
    private lateinit var btnStop: MaterialButton
    
    // Chart
    private lateinit var chipGroupTimeframe: ChipGroup
    private lateinit var chartView: HabitProgressChartView
    
    // Action buttons
    private lateinit var btnEditHabit: MaterialButton
    private lateinit var btnDeleteHabit: MaterialButton
    
    // Timer variables
    private var meditationTimer: CountDownTimer? = null
    private var isTimerRunning = false
    private var timerDurationMs = 5 * 60 * 1000L // Default 5 minutes
    private var remainingTimeMs = timerDurationMs

    companion object {
        private const val EXTRA_HABIT_ID = "habit_id"
        
        fun newIntent(context: Context, habitId: String): Intent {
            return Intent(context, HabitDetailActivity::class.java).apply {
                putExtra(EXTRA_HABIT_ID, habitId)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_habit_detail)
        
        repository = StriveRepository.getInstance(this)
        
        val habitId = intent.getStringExtra(EXTRA_HABIT_ID)
        if (habitId == null) {
            finish()
            return
        }
        
        habit = repository.getAllHabits().find { it.id == habitId } ?: run {
            finish()
            return
        }
        
        initViews()
        setupClickListeners()
        updateUI()
        updateMeditationTimerVisibility()
        
        // Initialize chart once view is ready
        chartView.post { updateChartForSelectedTimeframe() }
    }
    
    private fun initViews() {
        // Header
        btnBack = findViewById(R.id.btnBack)
        btnFavorite = findViewById(R.id.btnFavorite)
        
        // Top section
        tvEmoji = findViewById(R.id.tvEmoji)
        tvHabitTitle = findViewById(R.id.tvHabitTitle)
        tvHabitType = findViewById(R.id.tvHabitType)
        viewHabitColor = findViewById(R.id.viewHabitColor)
        tvStreakNumber = findViewById(R.id.tvStreakNumber)
        tvProgressLarge = findViewById(R.id.tvProgressLarge)
        tvProgressPercent = findViewById(R.id.tvProgressPercent)
        progressBar = findViewById(R.id.progressBar)
        
        // Quick actions
        btnQuick1 = findViewById(R.id.btnQuick1)
        btnQuick2 = findViewById(R.id.btnQuick2)
        btnQuick3 = findViewById(R.id.btnQuick3)
        btnManualEntry = findViewById(R.id.btnManualEntry)
        
        // Meditation timer
        cardMeditationTimer = findViewById(R.id.cardMeditationTimer)
        chipGroupDuration = findViewById(R.id.chipGroupDuration)
        tilCustomDuration = findViewById(R.id.tilCustomDuration)
        etCustomDuration = findViewById(R.id.etCustomDuration)
        tvTimerDisplay = findViewById(R.id.tvTimerDisplay)
        btnStartPause = findViewById(R.id.btnStartPause)
        btnStop = findViewById(R.id.btnStop)
        
    // Chart
        chipGroupTimeframe = findViewById(R.id.chipGroupTimeframe)
        chartView = findViewById(R.id.chartView)
        
        // Action buttons
        btnEditHabit = findViewById(R.id.btnEditHabit)
        btnDeleteHabit = findViewById(R.id.btnDeleteHabit)
    }
    
    private fun setupClickListeners() {
        btnBack.setOnClickListener { finish() }
        
        btnFavorite.setOnClickListener {
            habit = habit.copy(isStarred = !habit.isStarred)
            repository.updateHabit(habit)
            updateFavoriteIcon()
        }
        
        // Quick action buttons
        btnQuick1.setOnClickListener { addProgress(getQuickValue(0).toFloat()) }
        btnQuick2.setOnClickListener { addProgress(getQuickValue(1).toFloat()) }
        btnQuick3.setOnClickListener { addProgress(getQuickValue(2).toFloat()) }
        btnManualEntry.setOnClickListener { showManualEntryDialog() }
        
        // Meditation timer
        setupMeditationTimer()
        
        // Chart timeframe selection
        chipGroupTimeframe.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) updateChartForTimeframe(checkedIds[0])
        }
        
        // Action buttons
        btnEditHabit.setOnClickListener { openEditHabit() }
        btnDeleteHabit.setOnClickListener { showDeleteConfirmation() }
    }
    
    private fun setupMeditationTimer() {
        // Duration chip selection
        chipGroupDuration.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                when (checkedIds[0]) {
                    R.id.chip5min -> setTimerDuration(5)
                    R.id.chip10min -> setTimerDuration(10)
                    R.id.chip15min -> setTimerDuration(15)
                    R.id.chip20min -> setTimerDuration(20)
                    R.id.chipCustom -> {
                        tilCustomDuration.visibility = View.VISIBLE
                        etCustomDuration.requestFocus()
                    }
                    else -> {
                        tilCustomDuration.visibility = View.GONE
                    }
                }
            }
        }
        
        etCustomDuration.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val customMinutes = etCustomDuration.text.toString().toIntOrNull()
                if (customMinutes != null && customMinutes > 0) {
                    setTimerDuration(customMinutes)
                }
            }
        }
        
        btnStartPause.setOnClickListener {
            if (isTimerRunning) {
                pauseTimer()
            } else {
                startTimer()
            }
        }
        
        btnStop.setOnClickListener {
            stopTimer()
        }
    }
    
    private fun setTimerDuration(minutes: Int) {
        timerDurationMs = minutes * 60 * 1000L
        remainingTimeMs = timerDurationMs
        updateTimerDisplay()
    }
    
    private fun startTimer() {
        isTimerRunning = true
        btnStartPause.text = getString(R.string.pause)
        btnStartPause.setBackgroundTintList(
            resources.getColorStateList(R.color.warning, theme)
        )
        
        meditationTimer = object : CountDownTimer(remainingTimeMs, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                remainingTimeMs = millisUntilFinished
                updateTimerDisplay()
            }
            
            override fun onFinish() {
                onTimerComplete()
            }
        }.start()
    }
    
    private fun pauseTimer() {
        isTimerRunning = false
        meditationTimer?.cancel()
        btnStartPause.text = getString(R.string.resume)
        btnStartPause.setBackgroundTintList(
            resources.getColorStateList(R.color.success, theme)
        )
    }
    
    private fun stopTimer() {
        isTimerRunning = false
        meditationTimer?.cancel()
        
        // Show partial session dialog if timer was running and some time passed
        val timeCompleted = timerDurationMs - remainingTimeMs
        if (timeCompleted > 0) {
            showPartialSessionDialog(timeCompleted)
        }
        
        resetTimer()
    }
    
    private fun resetTimer() {
        remainingTimeMs = timerDurationMs
        updateTimerDisplay()
        btnStartPause.text = getString(R.string.start_text)
        btnStartPause.setBackgroundTintList(
            resources.getColorStateList(R.color.success, theme)
        )
    }
    
    private fun onTimerComplete() {
        isTimerRunning = false
        
        // Log full meditation session
        val sessionMinutes = (timerDurationMs / (60 * 1000)).toInt()
        addProgress(sessionMinutes.toFloat())
        
        // Show completion dialog
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.meditation_complete_title))
            .setMessage(getString(R.string.meditation_complete_msg, sessionMinutes))
            .setPositiveButton(getString(R.string.ok)) { _, _ ->
                resetTimer()
            }
            .show()
    }
    
    private fun showPartialSessionDialog(timeCompleted: Long) {
        val minutesCompleted = (timeCompleted / (60 * 1000)).toInt()
        if (minutesCompleted > 0) {
            MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.log_partial_title))
                .setMessage(getString(R.string.log_partial_msg, minutesCompleted))
                .setPositiveButton(getString(R.string.log_it)) { _, _ ->
                    addProgress(minutesCompleted.toFloat())
                }
                .setNegativeButton(getString(R.string.discard), null)
                .show()
        }
    }
    
    private fun updateTimerDisplay() {
        val minutes = (remainingTimeMs / (60 * 1000)).toInt()
        val seconds = ((remainingTimeMs % (60 * 1000)) / 1000).toInt()
        tvTimerDisplay.text = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
    
    private fun updateMeditationTimerVisibility() {
        cardMeditationTimer.visibility = if (habit.title.contains("Meditate", ignoreCase = true)) View.VISIBLE else View.GONE
    }
    
    private fun getQuickValue(index: Int): Int {
        return when {
            habit.title.contains("Water", ignoreCase = true) -> when (index) {
                0 -> 250
                1 -> 500
                2 -> 1000
                else -> habit.defaultIncrement
            }
            habit.title.contains("Steps", ignoreCase = true) -> when (index) {
                0 -> 1000
                1 -> 2500
                2 -> 5000
                else -> habit.defaultIncrement
            }
            habit.title.contains("Meditate", ignoreCase = true) -> when (index) {
                0 -> 5
                1 -> 10
                2 -> 15
                else -> habit.defaultIncrement
            }
            else -> when (index) {
                0 -> (habit.defaultIncrement * 0.5).toInt()
                1 -> habit.defaultIncrement
                2 -> habit.defaultIncrement * 2
                else -> habit.defaultIncrement
            }
        }
    }
    
    private fun addProgress(value: Float) {
        repository.addTick(habit.id, value.toInt())
        updateProgressDisplay()
        updateChartForSelectedTimeframe()
    }
    
    private fun showManualEntryDialog() {
        val editText = EditText(this)
        editText.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        editText.hint = getString(R.string.manual_entry_hint)
        
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.manual_entry_title))
            .setMessage(getString(R.string.manual_entry_msg, habit.unit.lowercase(Locale.getDefault())))
            .setView(editText)
            .setPositiveButton(getString(R.string.add)) { _, _ ->
                val value = editText.text.toString().toFloatOrNull()
                if (value != null && value > 0) {
                    addProgress(value)
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
    
    private fun updateUI() {
        tvEmoji.text = habit.emoji
        tvHabitTitle.text = habit.title
        tvHabitType.text = if (habit.isBuiltIn) getString(R.string.built_in_habit) else getString(R.string.custom_habit)
        
        // Set habit color
        try {
            val colorDrawable = viewHabitColor.background as? android.graphics.drawable.GradientDrawable
            colorDrawable?.setColor(android.graphics.Color.parseColor(habit.color))
        } catch (e: Exception) {
            // Fallback if color parsing fails
            viewHabitColor.setBackgroundColor(android.graphics.Color.parseColor("#2196F3"))
        }
        
        updateFavoriteIcon()
        updateProgressDisplay()
        updateQuickButtons()
    }
    
    private fun updateFavoriteIcon() {
        btnFavorite.setImageResource(
            if (habit.isStarred) R.drawable.ic_star_filled else R.drawable.ic_star_outline
        )
    }
    
    private fun updateProgressDisplay() {
        val today = DateUtils.nowDateString()
        val todayTicks = repository.getTicksForDate(today)
        val currentProgress = todayTicks.find { it.habitId == habit.id }?.amount ?: 0
        val progressPercent = if (habit.targetPerDay > 0) {
            ((currentProgress * 100) / habit.targetPerDay).coerceAtMost(100)
        } else 0
        
        tvStreakNumber.text = calculateCurrentStreak().toString()
        tvProgressLarge.text = getString(R.string.habits_progress, currentProgress, habit.targetPerDay)
        tvProgressPercent.text = getString(R.string.percent_format, progressPercent)
        progressBar.progress = progressPercent
    }
    
    private fun updateQuickButtons() {
        val values = listOf(getQuickValue(0), getQuickValue(1), getQuickValue(2))
        btnQuick1.text = "+${values[0]}"
        btnQuick2.text = "+${values[1]}"
        btnQuick3.text = "+${values[2]}"
    }
    
    private fun calculateCurrentStreak(): Int {
        val calendar = Calendar.getInstance()
        var streak = 0
        
        while (true) {
            val dateStr = DateUtils.calendarToDateString(calendar)
            val dayTicks = repository.getTicksForDate(dateStr)
            val dayProgress = dayTicks.find { it.habitId == habit.id }?.amount ?: 0
            
            if (dayProgress >= habit.targetPerDay) {
                streak++
                calendar.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }
        
        return streak
    }
    
    private fun updateChartForSelectedTimeframe() {
        try {
            val selectedChipId = chipGroupTimeframe.checkedChipId
            if (selectedChipId != View.NO_ID) {
                updateChartForTimeframe(selectedChipId)
            } else {
                chipGroupTimeframe.check(R.id.chipDaily)
                updateChartForTimeframe(R.id.chipDaily)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            chartView.setChartData(emptyList(), "Daily")
        }
    }
    
    private fun updateChartForTimeframe(chipId: Int) {
        try {
            val timeframe = when (chipId) {
                R.id.chipDaily -> "Daily"
                R.id.chipWeekly -> "Weekly"
                R.id.chipMonthly -> "Monthly"
                R.id.chipYearly -> "Yearly"
                else -> "Daily"
            }
            
            val chartData = generateChartData(timeframe)
            chartView.setChartData(chartData, timeframe)
        } catch (e: Exception) {
            // Log error and set empty chart data
            e.printStackTrace()
            chartView.setChartData(emptyList(), "Daily")
        }
    }
    
    private fun generateChartData(timeframe: String): List<HabitProgressChartView.ChartDataPoint> {
        return try {
            val calendar = Calendar.getInstance()
            val data = mutableListOf<HabitProgressChartView.ChartDataPoint>()
            
            when (timeframe) {
            "Daily" -> {
                // Show last 7 days
                calendar.add(Calendar.DAY_OF_YEAR, -6)
                repeat(7) {
                    val dateStr = DateUtils.calendarToDateString(calendar)
                    val dayTicks = repository.getTicksForDate(dateStr)
                    val progress = dayTicks.find { it.habitId == habit.id }?.amount?.toFloat() ?: 0f
                    
                    val dayLabel = when (calendar.get(Calendar.DAY_OF_WEEK)) {
                        Calendar.SUNDAY -> "Sun"
                        Calendar.MONDAY -> "Mon"
                        Calendar.TUESDAY -> "Tue"
                        Calendar.WEDNESDAY -> "Wed"
                        Calendar.THURSDAY -> "Thu"
                        Calendar.FRIDAY -> "Fri"
                        Calendar.SATURDAY -> "Sat"
                        else -> ""
                    }
                    
                    data.add(HabitProgressChartView.ChartDataPoint(
                        label = dayLabel,
                        value = progress,
                        target = habit.targetPerDay.toFloat(),
                        date = dateStr
                    ))
                    
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            
            "Weekly" -> {
                // Show last 4 weeks
                calendar.add(Calendar.WEEK_OF_YEAR, -3)
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                
                repeat(4) {
                    val weekStart = calendar.clone() as Calendar
                    var weekTotal = 0f
                    
                    repeat(7) {
                        val dateStr = DateUtils.calendarToDateString(weekStart)
                        val dayTicks = repository.getTicksForDate(dateStr)
                        weekTotal += dayTicks.find { it.habitId == habit.id }?.amount?.toFloat() ?: 0f
                        weekStart.add(Calendar.DAY_OF_YEAR, 1)
                    }
                    
                    val weekLabel = "W${calendar.get(Calendar.WEEK_OF_YEAR)}"
                    
                    data.add(HabitProgressChartView.ChartDataPoint(
                        label = weekLabel,
                        value = weekTotal,
                        target = habit.targetPerDay.toFloat() * 7,
                        date = DateUtils.calendarToDateString(calendar)
                    ))
                    
                    calendar.add(Calendar.WEEK_OF_YEAR, 1)
                }
            }
            
            "Monthly" -> {
                // Show last 6 months
                calendar.add(Calendar.MONTH, -5)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                
                repeat(6) {
                    val monthStart = calendar.clone() as Calendar
                    var monthTotal = 0f
                    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                    
                    repeat(daysInMonth) {
                        val dateStr = DateUtils.calendarToDateString(monthStart)
                        val dayTicks = repository.getTicksForDate(dateStr)
                        monthTotal += dayTicks.find { it.habitId == habit.id }?.amount?.toFloat() ?: 0f
                        monthStart.add(Calendar.DAY_OF_YEAR, 1)
                    }
                    
                    val monthLabel = when (calendar.get(Calendar.MONTH)) {
                        Calendar.JANUARY -> "Jan"
                        Calendar.FEBRUARY -> "Feb"
                        Calendar.MARCH -> "Mar"
                        Calendar.APRIL -> "Apr"
                        Calendar.MAY -> "May"
                        Calendar.JUNE -> "Jun"
                        Calendar.JULY -> "Jul"
                        Calendar.AUGUST -> "Aug"
                        Calendar.SEPTEMBER -> "Sep"
                        Calendar.OCTOBER -> "Oct"
                        Calendar.NOVEMBER -> "Nov"
                        Calendar.DECEMBER -> "Dec"
                        else -> ""
                    }
                    
                    data.add(HabitProgressChartView.ChartDataPoint(
                        label = monthLabel,
                        value = monthTotal,
                        target = habit.targetPerDay.toFloat() * daysInMonth,
                        date = DateUtils.calendarToDateString(calendar)
                    ))
                    
                    calendar.add(Calendar.MONTH, 1)
                }
            }
            
            "Yearly" -> {
                // Show last 3 years
                calendar.add(Calendar.YEAR, -2)
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                
                repeat(3) {
                    val yearStart = calendar.clone() as Calendar
                    var yearTotal = 0f
                    val daysInYear = calendar.getActualMaximum(Calendar.DAY_OF_YEAR)
                    
                    repeat(daysInYear) {
                        val dateStr = DateUtils.calendarToDateString(yearStart)
                        val dayTicks = repository.getTicksForDate(dateStr)
                        yearTotal += dayTicks.find { it.habitId == habit.id }?.amount?.toFloat() ?: 0f
                        yearStart.add(Calendar.DAY_OF_YEAR, 1)
                    }
                    
                    val yearLabel = calendar.get(Calendar.YEAR).toString()
                    
                    data.add(HabitProgressChartView.ChartDataPoint(
                        label = yearLabel,
                        value = yearTotal,
                        target = habit.targetPerDay.toFloat() * daysInYear,
                        date = DateUtils.calendarToDateString(calendar)
                    ))
                    
                    calendar.add(Calendar.YEAR, 1)
                }
            }
        }
        
        data
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    // Stats-only update methods removed along with UI
    
    // Additional calculation methods for different timeframes removed
    
    private fun openEditHabit() {
        val editFragment = AddHabitBottomSheetFragment.newInstance(
            onHabitCreated = {
                // Refresh the habit data and UI
                habit = repository.getAllHabits().find { it.id == habit.id } ?: habit
                updateUI()
                updateMeditationTimerVisibility()
                chartView.post { updateChartForSelectedTimeframe() }
            },
            habitToEdit = habit
        )
        
        editFragment.show(supportFragmentManager, getString(R.string.edit_habit_tag))
    }
    
    private fun showDeleteConfirmation() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.delete_habit))
            .setMessage(getString(R.string.confirm_delete_habit))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                deleteHabit()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
    
    private fun deleteHabit() {
        repository.deleteHabit(habit.id)
        Toast.makeText(this, getString(R.string.habit_deleted), Toast.LENGTH_SHORT).show()
        finish()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        meditationTimer?.cancel()
    }
}