package com.example.strive.fragments

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.*
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.strive.R
import com.example.strive.models.Habit
import com.example.strive.repo.StriveRepository
import com.example.strive.util.AlarmScheduler
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import kotlinx.coroutines.launch
import java.util.*
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class AddHabitBottomSheetFragment : DialogFragment() {

    private lateinit var repository: StriveRepository
    private var onHabitCreated: (() -> Unit)? = null
    
    // UI Components
    private lateinit var etEmoji: TextInputEditText
    private lateinit var tvEmojiPreview: TextView
    private lateinit var tilEmoji: TextInputLayout
    private lateinit var etHabitName: TextInputEditText
    private lateinit var tilHabitName: TextInputLayout
    private lateinit var etUnit: TextInputEditText
    private lateinit var tilUnit: TextInputLayout
    private lateinit var etDailyTarget: TextInputEditText
    private lateinit var tilDailyTarget: TextInputLayout
    private lateinit var etDefaultIncrement: TextInputEditText
    private lateinit var tilDefaultIncrement: TextInputLayout
    private lateinit var chipGroupIncrements: ChipGroup
    private lateinit var chipGroupColors: ChipGroup
    private lateinit var viewColorPreview: View
    private lateinit var switchReminders: SwitchMaterial
    private lateinit var llReminderContainer: LinearLayout
    private lateinit var btnAddReminder: MaterialButton
    private lateinit var switchFavorites: SwitchMaterial
    private lateinit var btnSave: MaterialButton
    private lateinit var btnClose: ImageButton
    
    // Data
    private val reminderTimes = mutableListOf<String>()
    private var selectedColor: String = "#2196F3" // Default blue
    private var editingHabit: Habit? = null
    
    // Color palette with 10 colors
    private val habitColors = listOf(
        "#FFFFFF" to "White",    // Meditate
        "#F44336" to "Red",      // Steps
        "#2196F3" to "Blue",     // Water
        "#4CAF50" to "Green",
        "#FFC107" to "Yellow",
        "#9C27B0" to "Purple",
        "#FF9800" to "Orange",
        "#00BCD4" to "Cyan",
        "#E91E63" to "Pink",
        "#795548" to "Brown"
    )
    
    // Permission launcher for notifications
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            // If permission denied, disable reminders switch
            switchReminders.isChecked = false
            Toast.makeText(requireContext(), "Notification permission is required for reminders", Toast.LENGTH_LONG).show()
        }
    }
    
    companion object {
        fun newInstance(onHabitCreated: (() -> Unit)? = null, habitToEdit: Habit? = null): AddHabitBottomSheetFragment {
            return AddHabitBottomSheetFragment().apply {
                this.onHabitCreated = onHabitCreated
                this.editingHabit = habitToEdit
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext(), R.style.FullScreenDialog)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false) // Prevent closing by touching outside or back button
        return dialog
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_add_habit, container, false)
    }

    override fun onStart() {
        super.onStart()
        
        // Set dialog window properties
        dialog?.window?.let { window ->
            val params = window.attributes
            params.width = ViewGroup.LayoutParams.MATCH_PARENT
            params.height = ViewGroup.LayoutParams.MATCH_PARENT
            window.attributes = params
            
            // Make status bar and navigation bar transparent
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            
            // Add margins by adjusting the dialog content
            val decorView = window.decorView
            val marginHorizontal = (16 * resources.displayMetrics.density).toInt() // Convert 16dp to pixels
            val marginVertical = (32 * resources.displayMetrics.density).toInt() // Convert 32dp to pixels
            decorView.setPadding(marginHorizontal, marginVertical, marginHorizontal, marginVertical) // Margins on all sides
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        repository = StriveRepository.getInstance(requireContext())
        
        initViews(view)
        setupListeners()
        setupValidation()
        setupColorPicker()
        setupKeyboardHandling(view)
        
        // Populate form if editing
        editingHabit?.let { populateFormForEdit(it) }
    }
    
    private fun setupKeyboardHandling(view: View) {
        // Find the ScrollView
        val scrollView = view.findViewById<android.widget.ScrollView>(R.id.scrollView)
        
        // Ensure the ScrollView can handle keyboard properly
        scrollView?.let { scroll ->
            scroll.isSmoothScrollingEnabled = true
            
            // Focus listeners for input fields to scroll when keyboard appears
            val inputFields = listOf(etEmoji, etHabitName, etDailyTarget, etDefaultIncrement)
            inputFields.forEach { editText ->
                editText.setOnFocusChangeListener { _, hasFocus ->
                    if (hasFocus) {
                        scroll.post {
                            scroll.smoothScrollTo(0, editText.bottom)
                        }
                    }
                }
            }
        }
    }
    
    private fun initViews(view: View) {
        etEmoji = view.findViewById(R.id.etEmoji)
        tvEmojiPreview = view.findViewById(R.id.tvEmojiPreview)
        tilEmoji = view.findViewById(R.id.tilEmoji)
        etHabitName = view.findViewById(R.id.etHabitName)
        tilHabitName = view.findViewById(R.id.tilHabitName)
        etUnit = view.findViewById(R.id.etUnit)
        tilUnit = view.findViewById(R.id.tilUnit)
        etDailyTarget = view.findViewById(R.id.etDailyTarget)
        tilDailyTarget = view.findViewById(R.id.tilDailyTarget)
        etDefaultIncrement = view.findViewById(R.id.etDefaultIncrement)
        tilDefaultIncrement = view.findViewById(R.id.tilDefaultIncrement)
        chipGroupIncrements = view.findViewById(R.id.chipGroupIncrements)
        chipGroupColors = view.findViewById(R.id.chipGroupColors)
    viewColorPreview = view.findViewById(R.id.viewSelectedColorPreview)
        switchReminders = view.findViewById(R.id.switchReminders)
        llReminderContainer = view.findViewById(R.id.llReminderContainer)
        btnAddReminder = view.findViewById(R.id.btnAddReminder)
        switchFavorites = view.findViewById(R.id.switchFavorites)
        btnSave = view.findViewById(R.id.btnSave)
        btnClose = view.findViewById(R.id.btnClose)
        
        // Set up emoji-only input filter
        setupEmojiFilter()
    }
    
    private fun setupEmojiFilter() {
        etEmoji.filters = arrayOf(android.text.InputFilter { source, start, end, dest, dstart, dend ->
            if (source.isNullOrEmpty()) return@InputFilter null
            
            val input = source.subSequence(start, end).toString()
            
            // If input is empty, allow it (for deletions)
            if (input.isEmpty()) return@InputFilter null
            
            // Check if the input is a valid emoji
            if (isValidEmoji(input)) {
                // If destination already has content, replace it with the new emoji
                if (dest.isNotEmpty()) {
                    return@InputFilter input
                }
                return@InputFilter null // Allow the input
            }
            
            // Reject non-emoji input
            return@InputFilter ""
        })
        
        // Limit to 2 characters max (to accommodate some emojis that are 2 characters)
        etEmoji.filters = etEmoji.filters + android.text.InputFilter.LengthFilter(2)
    }

    private fun setupListeners() {
        btnClose.setOnClickListener { 
            if (hasUnsavedChanges()) {
                showDiscardChangesDialog()
            } else {
                dismiss()
            }
        }
        
        btnSave.setOnClickListener { saveHabit() }
        
        switchReminders.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Check notification permission for Android 13+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) 
                        != PackageManager.PERMISSION_GRANTED) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        return@setOnCheckedChangeListener
                    }
                }
                llReminderContainer.visibility = View.VISIBLE
            } else {
                llReminderContainer.visibility = View.GONE
            }
        }
        
        btnAddReminder.setOnClickListener { showTimePicker() }
        
        etEmoji.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val text = s?.toString() ?: ""
                if (text.isNotEmpty() && isValidEmoji(text)) {
                    tvEmojiPreview.text = text
                    tvEmojiPreview.visibility = View.VISIBLE
                } else {
                    tvEmojiPreview.visibility = View.GONE
                }
                validateForm()
            }
        })
    }
    
    private fun setupValidation() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { 
                // Clear errors when user starts typing
                clearFieldErrors()
            }
        }
        
        etHabitName.addTextChangedListener(textWatcher)
        etDailyTarget.addTextChangedListener(textWatcher)
        etDefaultIncrement.addTextChangedListener(textWatcher)
        etEmoji.addTextChangedListener(textWatcher)
    }
    
    private fun clearFieldErrors() {
        tilEmoji.error = null
        tilHabitName.error = null
        tilUnit.error = null
        tilDailyTarget.error = null
        tilDefaultIncrement.error = null
    }
    
    private fun setupColorPicker() {
        // Add color chips
    habitColors.forEachIndexed { _, (colorHex, colorName) ->
            val chip = Chip(requireContext())
            chip.text = colorName
            chip.isCheckable = true
            chip.chipBackgroundColor = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor(colorHex))
            
            // Set text color based on background lightness
            val textColor = if (colorHex == "#FFFFFF" || colorHex == "#FFC107") {
                android.graphics.Color.BLACK
            } else {
                android.graphics.Color.WHITE
            }
            chip.setTextColor(textColor)
            
            chip.setOnClickListener {
                selectedColor = colorHex
                updateColorPreview(colorHex)
            }
            
            // Select default color (Blue)
            if (colorHex == selectedColor) {
                chip.isChecked = true
                updateColorPreview(colorHex)
            }
            
            chipGroupColors.addView(chip)
        }

        updateColorPreview(selectedColor)
        
        // Handle unit text changes to update increment suggestions
        etUnit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateIncrementChips(s?.toString()?.uppercase() ?: "")
            }
        })
    }

    private fun updateColorPreview(colorHex: String) {
        val parsedColor = try {
            android.graphics.Color.parseColor(colorHex)
        } catch (e: IllegalArgumentException) {
            android.graphics.Color.parseColor("#2196F3")
        }
        val backgroundDrawable = viewColorPreview.background
        if (backgroundDrawable is android.graphics.drawable.GradientDrawable) {
            backgroundDrawable.mutate()
            backgroundDrawable.setColor(parsedColor)
        } else {
            viewColorPreview.setBackgroundColor(parsedColor)
        }
    }
    
    private fun updateIncrementChips(unit: String) {
        chipGroupIncrements.removeAllViews()
        
        val suggestions = when (unit) {
            "ML" -> listOf(100, 250, 500)
            "LITERS", "L" -> listOf(1, 2, 3)
            "MINUTES", "MINS", "MIN" -> listOf(5, 10, 15)
            "STEPS" -> listOf(500, 1000, 2000)
            "COUNT" -> listOf(1, 5, 10)
            else -> listOf(1, 5, 10) // Default suggestions
        }
        
        suggestions.forEach { value ->
            val chip = Chip(requireContext())
            chip.text = value.toString()
            chip.isClickable = true
            chip.setOnClickListener {
                etDefaultIncrement.setText(value.toString())
            }
            chipGroupIncrements.addView(chip)
        }
    }
    
    private fun isValidEmoji(text: String): Boolean {
        if (text.isEmpty() || text.length > 2) return false
        
        // Check if the text contains emoji characters
        val codePoints = text.codePoints().toArray()
        
        for (codePoint in codePoints) {
            when {
                // Common emoji ranges
                codePoint in 0x1F600..0x1F64F || // Emoticons
                codePoint in 0x1F300..0x1F5FF || // Misc Symbols and Pictographs
                codePoint in 0x1F680..0x1F6FF || // Transport and Map
                codePoint in 0x1F700..0x1F77F || // Alchemical Symbols
                codePoint in 0x2600..0x26FF ||   // Misc symbols
                codePoint in 0x2700..0x27BF ||   // Dingbats
                codePoint in 0xFE00..0xFE0F ||   // Variation Selectors
                codePoint in 0x1F900..0x1F9FF || // Supplemental Symbols and Pictographs
                codePoint in 0x1F1E6..0x1F1FF || // Regional Indicator Symbols (flags)
                codePoint in 0x2000..0x206F ||   // General Punctuation (for zero-width joiners)
                codePoint == 0x200D              // Zero Width Joiner (for compound emojis)
                -> continue
                else -> return false
            }
        }
        return true
    }
    
    private fun validateForm(): Boolean {
        var isValid = true
        
        // Validate emoji
        val emoji = etEmoji.text?.toString() ?: ""
        if (emoji.isEmpty()) {
            tilEmoji.error = "Please enter an emoji for the habit icon"
            isValid = false
        } else if (!isValidEmoji(emoji)) {
            tilEmoji.error = "Please enter a single emoji for the habit icon"
            isValid = false
        } else {
            tilEmoji.error = null
        }
        
        // Validate habit name
        val name = etHabitName.text?.toString()?.trim() ?: ""
        when {
            name.isEmpty() -> {
                tilHabitName.error = "Please enter a habit name"
                isValid = false
            }
            name.length > 40 -> {
                tilHabitName.error = "Maximum 40 characters"
                isValid = false
            }
            else -> tilHabitName.error = null
        }
        
        // Validate unit
        val unitText = etUnit.text?.toString()?.trim() ?: ""
        if (unitText.isEmpty()) {
            tilUnit.error = "Please enter a unit"
            isValid = false
        } else if (unitText.length > 20) {
            tilUnit.error = "Maximum 20 characters"
            isValid = false
        } else {
            tilUnit.error = null
        }
        
        // Validate daily target
        val targetText = etDailyTarget.text?.toString() ?: ""
        if (targetText.isEmpty() || targetText.toIntOrNull() == null || targetText.toInt() <= 0) {
            tilDailyTarget.error = "Please enter a valid daily target"
            isValid = false
        } else {
            tilDailyTarget.error = null
        }
        
        // Validate default increment
        val incrementText = etDefaultIncrement.text?.toString() ?: ""
        if (incrementText.isEmpty() || incrementText.toIntOrNull() == null || incrementText.toInt() <= 0) {
            tilDefaultIncrement.error = "Please enter a valid increment"
            isValid = false
        } else {
            tilDefaultIncrement.error = null
        }
        
        return isValid
    }
    
    private fun showTimePicker() {
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(9)
            .setMinute(0)
            .setTitleText("Select reminder time")
            .build()
            
        picker.addOnPositiveButtonClickListener {
            val timeString = String.format("%02d:%02d", picker.hour, picker.minute)
            if (!reminderTimes.contains(timeString)) {
                reminderTimes.add(timeString)
                addReminderChip(timeString)
            } else {
                Toast.makeText(requireContext(), "This reminder time already exists", Toast.LENGTH_SHORT).show()
            }
        }
        
        picker.show(parentFragmentManager, "time_picker")
    }
    
    private fun addReminderChip(time: String) {
        val chip = Chip(requireContext())
        chip.text = time
        chip.isCloseIconVisible = true
        chip.setOnCloseIconClickListener {
            reminderTimes.remove(time)
            llReminderContainer.removeView(chip)
        }
        llReminderContainer.addView(chip, llReminderContainer.childCount - 1) // Add before the "Add" button
    }
    
    private fun clearReminderChips() {
        // Remove all chip views except the "Add" button (which should be the last child)
        val childCount = llReminderContainer.childCount
        for (i in childCount - 2 downTo 0) { // Skip the last child (Add button)
            val child = llReminderContainer.getChildAt(i)
            if (child is Chip) {
                llReminderContainer.removeView(child)
            }
        }
    }
    
    private fun saveHabit() {
        // Validate form first
        if (!validateForm()) {
            return
        }
        
        val emoji = etEmoji.text?.toString()?.trim() ?: ""
        val name = etHabitName.text?.toString()?.trim() ?: ""
        val unit = etUnit.text?.toString()?.trim()?.uppercase() ?: "COUNT"
        val target = etDailyTarget.text?.toString()?.toIntOrNull() ?: 0
        val increment = etDefaultIncrement.text?.toString()?.toIntOrNull() ?: 0
        
        // Convert LITERS to ML if needed
        val finalTarget = if (unit == "LITERS" || unit == "L") target * 1000 else target
        val finalIncrement = if (unit == "LITERS" || unit == "L") increment * 1000 else increment
        val finalUnit = if (unit == "LITERS" || unit == "L") "ML" else unit
        
        // Check if increment > target
        if (finalIncrement > finalTarget) {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Confirm")
                .setMessage("Default increment is greater than daily target. Continue?")
                .setPositiveButton("Accept") { _, _ -> proceedWithSave(emoji, name, finalUnit, finalTarget, finalIncrement) }
                .setNegativeButton("Adjust", null)
                .show()
        } else {
            proceedWithSave(emoji, name, finalUnit, finalTarget, finalIncrement)
        }
    }
    
    private fun proceedWithSave(emoji: String, name: String, unit: String, target: Int, increment: Int) {
        val habit = if (editingHabit != null) {
            // Update existing habit
            editingHabit!!.copy(
                title = name,
                emoji = emoji,
                unit = unit,
                targetPerDay = target,
                defaultIncrement = increment,
                color = selectedColor,
                isStarred = switchFavorites.isChecked,
                reminderTimes = reminderTimes.toList()
            )
        } else {
            // Create new habit
            Habit(
                id = "habit_${System.currentTimeMillis()}",
                title = name,
                emoji = emoji,
                unit = unit,
                targetPerDay = target,
                defaultIncrement = increment,
                color = selectedColor,
                isBuiltIn = false,
                isStarred = switchFavorites.isChecked,
                reminderTimes = reminderTimes.toList(),
                enabled = true,
                createdAt = System.currentTimeMillis()
            )
        }
        
        lifecycleScope.launch {
            try {
                if (editingHabit != null) {
                    repository.updateHabit(habit)
                } else {
                    repository.addHabit(habit)
                }
                
                // Schedule reminders if enabled and permission granted
                if (switchReminders.isChecked && reminderTimes.isNotEmpty()) {
                    // Check notification permission again before scheduling
                    val hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                    } else {
                        true // No permission needed on older versions
                    }
                    
                    if (hasNotificationPermission) {
                        AlarmScheduler.scheduleForHabit(requireContext(), habit.id, reminderTimes)
                    } else {
                        Toast.makeText(requireContext(), "Notification permission not granted. Reminders will not work.", Toast.LENGTH_LONG).show()
                    }
                }
                
                val message = if (editingHabit != null) "Habit updated" else "Habit created"
                activity?.let { activity ->
                    Snackbar.make(activity.findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
                        .setAction("Undo") {
                            lifecycleScope.launch {
                                if (editingHabit != null) {
                                    repository.updateHabit(editingHabit!!)
                                    // Reschedule alarms for the original habit
                                    AlarmScheduler.scheduleForHabit(requireContext(), editingHabit!!.id, editingHabit!!.reminderTimes)
                                } else {
                                    repository.deleteHabit(habit.id)
                                    // Cancel all scheduled alarms for this habit
                                    habit.reminderTimes.forEach { timeStr ->
                                        AlarmScheduler.cancelScheduledAlarm(requireContext(), habit.id, timeStr)
                                    }
                                }
                            }
                        }
                        .show()
                }
                
                onHabitCreated?.invoke()
                dismiss()
                
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error creating habit: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun hasUnsavedChanges(): Boolean {
        return etEmoji.text?.isNotEmpty() == true ||
               etHabitName.text?.isNotEmpty() == true ||
               etDailyTarget.text?.isNotEmpty() == true ||
               etDefaultIncrement.text?.isNotEmpty() == true ||
               reminderTimes.isNotEmpty()
    }
    
    private fun populateFormForEdit(habit: Habit) {
        // Set title
        view?.findViewById<TextView>(R.id.tvTitle)?.text = "Edit Habit"
        btnSave.text = "Update Habit"
        
        // Populate emoji
        etEmoji.setText(habit.emoji)
        tvEmojiPreview.text = habit.emoji
        tvEmojiPreview.visibility = View.VISIBLE
        
        // Populate habit name
        etHabitName.setText(habit.title)
        
        // Populate unit
        etUnit.setText(habit.unit)
        
        // Populate daily target
        etDailyTarget.setText(habit.targetPerDay.toString())
        
        // Populate default increment
        etDefaultIncrement.setText(habit.defaultIncrement.toString())
        
        // Populate color selection
        selectedColor = habit.color
        // Find and check the matching chip
        for (i in 0 until chipGroupColors.childCount) {
            val chip = chipGroupColors.getChildAt(i) as? Chip
            chip?.let {
                val chipColor = habitColors.getOrNull(i)?.first
                it.isChecked = chipColor == habit.color
            }
        }
        updateColorPreview(habit.color)
        
        // Populate reminders
        reminderTimes.clear()
        clearReminderChips() // Clear existing chips from UI
        reminderTimes.addAll(habit.reminderTimes)
        switchReminders.isChecked = habit.reminderTimes.isNotEmpty()
        llReminderContainer.visibility = if (habit.reminderTimes.isNotEmpty()) View.VISIBLE else View.GONE
        
        // Add reminder chips for existing reminder times
        habit.reminderTimes.forEach { timeString ->
            addReminderChip(timeString)
        }
        
        // Populate favorites
        switchFavorites.isChecked = habit.isStarred
        
        // Update increment chips for the selected unit
        updateIncrementChips(habit.unit)
    }
    
    private fun showDiscardChangesDialog() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Discard changes?")
            .setMessage("You have unsaved changes. Are you sure you want to discard them?")
            .setPositiveButton("Discard") { _, _ -> dismiss() }
            .setNegativeButton("Cancel", null)
            .show()
    }
}