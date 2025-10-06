package com.example.strive.dialogs

import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.strive.R
import com.example.strive.util.EmojiInfo
import com.example.strive.util.EmojiPalette
import com.example.strive.models.MoodEntry
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.*

class MoodBottomSheetDialog(
    private val moodEntry: MoodEntry? = null,
    private val onSave: (MoodEntry) -> Unit,
    private val onDelete: ((String) -> Unit)? = null
) : DialogFragment() {

    private lateinit var emojiGridAdapter: EmojiGridAdapter
    private var selectedEmojiInfo: EmojiInfo? = null
    private var selectedTimestamp: Long = System.currentTimeMillis()

    private lateinit var tvSheetTitle: TextView
    private lateinit var rvEmojiGrid: RecyclerView
    private lateinit var layoutSelectedEmoji: View
    private lateinit var tvSelectedEmoji: TextView
    private lateinit var tvSelectedEmojiName: TextView
    private lateinit var tvSelectedEmojiScore: TextView
    private lateinit var etNote: TextInputEditText
    private lateinit var btnEditDate: MaterialButton
    private lateinit var btnEditTime: MaterialButton
    private lateinit var btnDeleteMood: MaterialButton
    private lateinit var btnClose: MaterialButton
    private lateinit var btnCancel: MaterialButton
    private lateinit var btnSave: MaterialButton

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.requestFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_mood, container, false)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            val params = window.attributes
            params.width = ViewGroup.LayoutParams.MATCH_PARENT
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT
            window.attributes = params
            window.setBackgroundDrawableResource(android.R.color.transparent)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        setupEmojiGrid()
        setupButtons()
        populateData()
    }

    private fun initViews(view: View) {
        tvSheetTitle = view.findViewById(R.id.tvSheetTitle)
        rvEmojiGrid = view.findViewById(R.id.rvEmojiGrid)
        layoutSelectedEmoji = view.findViewById(R.id.layoutSelectedEmoji)
        tvSelectedEmoji = view.findViewById(R.id.tvSelectedEmoji)
        tvSelectedEmojiName = view.findViewById(R.id.tvSelectedEmojiName)
        tvSelectedEmojiScore = view.findViewById(R.id.tvSelectedEmojiScore)
        etNote = view.findViewById(R.id.etNote)
        btnEditDate = view.findViewById(R.id.btnEditDate)
        btnEditTime = view.findViewById(R.id.btnEditTime)
        btnDeleteMood = view.findViewById(R.id.btnDeleteMood)
        btnClose = view.findViewById(R.id.btnClose)
        btnCancel = view.findViewById(R.id.btnCancel)
        btnSave = view.findViewById(R.id.btnSave)
    }

    private fun setupEmojiGrid() {
        emojiGridAdapter = EmojiGridAdapter(EmojiPalette.EMOJIS) { emojiInfo ->
            selectedEmojiInfo = emojiInfo
            updateSelectedEmojiDisplay()
        }
        
        rvEmojiGrid.apply {
            layoutManager = GridLayoutManager(context, 5)
            adapter = emojiGridAdapter
        }
    }

    private fun setupButtons() {
        btnEditDate.setOnClickListener { showDatePicker() }
        btnEditTime.setOnClickListener { showTimePicker() }
        
        btnDeleteMood.setOnClickListener {
            moodEntry?.let { mood ->
                onDelete?.invoke(mood.id)
                dismiss()
            }
        }
        
        btnClose.setOnClickListener { dismiss() }
        
        btnCancel.setOnClickListener { dismiss() }
        
        btnSave.setOnClickListener { saveMood() }
    }

    private fun populateData() {
        if (moodEntry != null) {
            // Edit mode
            tvSheetTitle.text = getString(R.string.edit_mood)
            btnDeleteMood.visibility = View.VISIBLE
            
            // Find and select the emoji
            selectedEmojiInfo = EmojiPalette.EMOJIS.find { it.emoji == moodEntry.emoji }
            selectedTimestamp = moodEntry.timestamp
            etNote.setText(moodEntry.note ?: "")
            
            updateSelectedEmojiDisplay()
        } else {
            // Add mode
            tvSheetTitle.text = getString(R.string.add_mood)
            btnDeleteMood.visibility = View.GONE
        }
        
        updateDateTimeDisplay()
    }

    private fun updateSelectedEmojiDisplay() {
        selectedEmojiInfo?.let { emojiInfo ->
            layoutSelectedEmoji.visibility = View.VISIBLE
            tvSelectedEmoji.text = emojiInfo.emoji
            tvSelectedEmojiName.text = emojiInfo.name
            tvSelectedEmojiScore.text = "Score: ${emojiInfo.score}/5"
            
            emojiGridAdapter.setSelectedEmoji(emojiInfo)
        }
    }

    private fun updateDateTimeDisplay() {
        val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        
        val date = Date(selectedTimestamp)
        btnEditDate.text = dateFormatter.format(date)
        btnEditTime.text = timeFormatter.format(date)
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = selectedTimestamp
        }
        
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val newCalendar = Calendar.getInstance().apply {
                    timeInMillis = selectedTimestamp
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }
                selectedTimestamp = newCalendar.timeInMillis
                updateDateTimeDisplay()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showTimePicker() {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = selectedTimestamp
        }
        
        TimePickerDialog(
            requireContext(),
            { _, hourOfDay, minute ->
                val newCalendar = Calendar.getInstance().apply {
                    timeInMillis = selectedTimestamp
                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                    set(Calendar.MINUTE, minute)
                }
                selectedTimestamp = newCalendar.timeInMillis
                updateDateTimeDisplay()
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            false
        ).show()
    }

    private fun saveMood() {
        val emojiInfo = selectedEmojiInfo
        if (emojiInfo == null) {
            // Show error - emoji required
            return
        }
        
        val note = etNote.text?.toString()?.trim() ?: ""
        
        val mood = if (moodEntry != null) {
            // Edit existing
            moodEntry.copy(
                emoji = emojiInfo.emoji,
                note = note,
                timestamp = selectedTimestamp,
                score = emojiInfo.score
            )
        } else {
            // Create new
            MoodEntry(
                id = generateMoodId(),
                emoji = emojiInfo.emoji,
                note = note,
                timestamp = selectedTimestamp,
                score = emojiInfo.score
            )
        }
        
        onSave(mood)
        dismiss()
    }

    private fun generateMoodId(): String {
        return "mood_${System.currentTimeMillis()}_${(0..999).random()}"
    }

    companion object {
        fun newInstance(
            moodEntry: MoodEntry? = null,
            onSave: (MoodEntry) -> Unit,
            onDelete: ((String) -> Unit)? = null
        ) = MoodBottomSheetDialog(moodEntry, onSave, onDelete)
    }
}