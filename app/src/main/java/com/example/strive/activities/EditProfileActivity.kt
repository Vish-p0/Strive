package com.example.strive.activities

import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.text.TextWatcher
import android.text.Editable
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.example.strive.R
import com.example.strive.models.UserProfile
import com.example.strive.repo.StriveRepository
import com.google.android.material.button.MaterialButton
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class EditProfileActivity : AppCompatActivity() {

    private lateinit var repo: StriveRepository

    private lateinit var tvAvatarEmoji: android.widget.TextView
    private lateinit var etEmojiInput: TextInputEditText
    private lateinit var tilName: TextInputLayout
    private lateinit var etName: TextInputEditText
    private lateinit var tilAge: TextInputLayout
    private lateinit var etAge: TextInputEditText
    private lateinit var tilGender: TextInputLayout
    private lateinit var spGender: MaterialAutoCompleteTextView
    private lateinit var btnSave: MaterialButton
    private var bottomNav: BottomNavigationView? = null

    private var avatarEmoji: String = "😃"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        repo = StriveRepository.getInstance(this)
        bindViews()
        setupGenderSpinner()
        populateFromProfile()
        bindActions()
        setupToolbar()
        setupBottomNav()
    }

    private fun bindViews() {
        tvAvatarEmoji = findViewById(R.id.tvAvatarEmoji)
        etEmojiInput = findViewById(R.id.etEmojiInput)
        tilName = findViewById(R.id.tilName)
        etName = findViewById(R.id.etName)
        tilAge = findViewById(R.id.tilAge)
        etAge = findViewById(R.id.etAge)
        tilGender = findViewById(R.id.tilGender)
        spGender = findViewById(R.id.spGender)
        btnSave = findViewById(R.id.btnSave)
        bottomNav = findViewById(R.id.bottomNav)
    }

    private fun setupGenderSpinner() {
        val genderOptions = resources.getStringArray(R.array.gender_options)
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, genderOptions)
        spGender.setAdapter(adapter)
    }

    private fun populateFromProfile() {
        val p: UserProfile? = repo.getUserProfile()
        if (p != null) {
            avatarEmoji = p.avatarEmoji
            tvAvatarEmoji.text = p.avatarEmoji
            etEmojiInput.setText(p.avatarEmoji)
            etName.setText(p.name)
            etAge.setText(p.age.toString())
            spGender.setText(p.gender, false)
        } else {
            tvAvatarEmoji.text = avatarEmoji
            etEmojiInput.setText(avatarEmoji)
        }

        // Setup emoji input validation and watcher
        setupEmojiInput()
    }

    private fun setupEmojiInput() {
        // Emoji: Allow single emoji (which can be 1-2 code units)
        val emojiFilter = InputFilter { source, start, end, dest, dstart, dend ->
            val input = dest.toString().substring(0, dstart) +
                       source.subSequence(start, end) +
                       dest.toString().substring(dend)

            // Allow empty input
            if (input.isEmpty()) return@InputFilter null

            // Count grapheme clusters (visible emoji characters)
            val graphemeCount = input.codePointCount(0, input.length)
            
            // Allow up to 1 emoji character (which may be 1-2 code units)
            if (graphemeCount <= 2 && input.length <= 4) {
                null // Accept
            } else {
                "" // Reject
            }
        }
        etEmojiInput.filters = arrayOf(emojiFilter)

        etEmojiInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val emoji = s?.toString()?.trim()
                if (!emoji.isNullOrEmpty()) {
                    avatarEmoji = emoji
                    tvAvatarEmoji.text = emoji
                } else {
                    avatarEmoji = "😃" // Default to 😃 if empty
                    tvAvatarEmoji.text = avatarEmoji
                }
            }
        })
    }

    private fun bindActions() {
        tvAvatarEmoji.setOnClickListener {
            etEmojiInput.requestFocus()
            etEmojiInput.selectAll()
        }

        btnSave.setOnClickListener {
            if (validate()) {
                val emoji = etEmojiInput.text?.toString()?.trim() ?: "😃"
                val updated = UserProfile(
                    name = etName.text?.toString()?.trim() ?: "",
                    age = etAge.text?.toString()?.toIntOrNull() ?: 0,
                    gender = spGender.text?.toString()?.trim() ?: "",
                    avatarEmoji = emoji
                )
                repo.saveUserProfile(updated)
                finish()
            }
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
                        startActivity(Intent(this@EditProfileActivity, HomeActivity::class.java))
                        finish(); true
                    }
                    R.id.menu_habits -> {
                        startActivity(Intent(this@EditProfileActivity, HabitsActivity::class.java))
                        finish(); true
                    }
                    R.id.menu_mood -> {
                        startActivity(Intent(this@EditProfileActivity, MoodActivity::class.java))
                        finish(); true
                    }
                    R.id.menu_profile -> {
                        startActivity(Intent(this@EditProfileActivity, ProfileActivity::class.java))
                        finish(); true
                    }
                    else -> false
                }
            }
        }
    }

    private fun validate(): Boolean {
        var ok = true
        if (etName.text.isNullOrBlank()) { tilName.error = getString(R.string.error_required_field); ok = false } else tilName.error = null
        val age = etAge.text?.toString()?.toIntOrNull()
        if (age == null || age < 1 || age > 150) { tilAge.error = getString(R.string.error_invalid_age); ok = false } else tilAge.error = null
        if (spGender.text.isNullOrBlank()) { tilGender.error = getString(R.string.error_required_field); ok = false } else tilGender.error = null
        val emoji = etEmojiInput.text?.toString()?.trim()
        if (emoji.isNullOrEmpty()) { etEmojiInput.error = "Emoji is required"; ok = false } else etEmojiInput.error = null
        return ok
    }
}
