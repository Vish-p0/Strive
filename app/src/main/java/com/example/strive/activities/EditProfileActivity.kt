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
        // Emoji: Only allow single emoji characters
        val emojiFilter = InputFilter { source, start, end, dest, dstart, dend ->
            val input = dest.toString().substring(0, dstart) +
                       source.subSequence(start, end) +
                       dest.toString().substring(dend)

            // Allow empty input
            if (input.isEmpty()) return@InputFilter null

            // Check if input contains only emoji characters and is not longer than 1 character
            val emojiPattern = "[\\u1F600-\\u1F64F\\u1F300-\\u1F5FF\\u1F680-\\u1F6FF\\u1F1E0-\\u1F1FF\\u2600-\\u26FF\\u2700-\\u27BF\\uFE00-\\uFE0F\\u1F926-\\u1F937\\u10000-\\u1007F\\u2640-\\u2642\\u2600-\\u2B55\\u200D\\u23CF\\u23E9\\u231A\\uFE0F\\u3030\\u0023\\u002A\\u0030-\\u0039\\u00A9\\u00AE\\u203C\\u2049\\u2122\\u2139\\u2194-\\u2199\\u21A9\\u21AA\\u231A\\u231B\\u2328\\u2388\\u23CF\\u23E9-\\u23F3\\u23F8-\\u23FA\\u25AA\\u25AB\\u25B6\\u25C0\\u25FB\\u25FC\\u2600-\\u2604\\u260E\\u2611\\u2614\\u2615\\u2618\\u2620\\u2622\\u2623\\u2626\\u262A\\u262E\\u262F\\u2638\\u2639\\u263A\\u2648-\\u2653\\u2660\\u2663\\u2665\\u2666\\u2668\\u267B\\u267F\\u2693\\u26A0\\u26A1\\u26AA\\u26AB\\u26BD\\u26BE\\u26C4\\u26C5\\u26CE\\u26D4\\u26EA\\u26F2\\u26F3\\u26F5\\u26FA\\u26FD\\u2702\\u2705\\u2708-\\u270D\\u270F\\u2712\\u2714\\u2716\\u2728\\u2733\\u2734\\u2744\\u2747\\u2934\\u2935\\u2B05-\\u2B07\\u2B1B\\u2B1C\\u2B50\\u2B55\\u3030\\u303D\\u3297\\u3299\\uFE0F\\u1F004\\u1F0CF\\u1F18E\\u1F191-\\u1F19A\\u1F201\\u1F21A\\u1F22F\\u1F30D-\\u1F30F\\u1F315\\u1F31C\\u1F321-\\u1F325\\u1F337-\\u1F37C\\u1F396-\\u1F397\\u1F399-\\u1F39B\\u1F3A0-\\u1F3C4\\u1F3C6-\\u1F3CA\\u1F3E0-\\u1F3F0\\u1F400-\\u1F43E\\u1F440\\u1F442-\\u1F4F7\\u1F4F9-\\u1F4FC\\u1F500-\\u1F53D\\u1F549-\\u1F54E\\u1F550-\\u1F567\\u1F595-\\u1F596\\u1F5FB-\\u1F5FF\\u1F600-\\u1F636\\u1F645-\\u1F64F\\u1F680-\\u1F6C5\\u1F6CB-\\u1F6D2\\u1F6F4-\\u1F6F9\\u1F7E0-\\u1F7EB\\u1F90D-\\u1F93A\\u1F93C-\\u1F945\\u1F947-\\u1F9FF\\u1FA70-\\u1FA73\\u1FA78-\\u1FA7A\\u1FA80-\\u1FA82\\u1FA90-\\u1FA95\\u1FAC0-\\u1FAC2\\u1FAD0-\\u1FAD6]".toRegex()
            val isOnlyEmojis = input.all { emojiPattern.matches(it.toString()) }
            if (isOnlyEmojis && input.length <= 1) {
                null // Accept
            } else {
                "" // Reject
            }
        }
        etEmojiInput.filters = arrayOf(emojiFilter, InputFilter.LengthFilter(1))

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
