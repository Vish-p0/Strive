package com.example.strive.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputFilter
import android.text.TextWatcher
import android.text.Editable
import java.text.BreakIterator
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.strive.R
import com.example.strive.models.UserProfile
import com.example.strive.repo.StriveRepository
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class SignupActivity : AppCompatActivity() {

    private lateinit var repository: StriveRepository
    private lateinit var tvAvatarEmoji: TextView
    private lateinit var etEmojiInput: TextInputEditText
    private lateinit var tilName: TextInputLayout
    private lateinit var etName: TextInputEditText
    private lateinit var tilAge: TextInputLayout
    private lateinit var etAge: TextInputEditText
    private lateinit var tilGender: TextInputLayout
    private lateinit var spGender: MaterialAutoCompleteTextView
    private lateinit var btnSignup: MaterialButton

    private var selectedAvatarEmoji = "\uD83D\uDE03" // 😃
    private val defaultAvatarEmoji = "\uD83D\uDE03"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)
        
        repository = StriveRepository.getInstance(this)
        
        initViews()
        setupInputValidation()
        setupGenderSpinner()
        setupClickListeners()
    }

    private fun initViews() {
        tvAvatarEmoji = findViewById(R.id.tvAvatarEmoji)
        etEmojiInput = findViewById(R.id.etEmojiInput)
        tilName = findViewById(R.id.tilName)
        etName = findViewById(R.id.etName)
        tilAge = findViewById(R.id.tilAge)
        etAge = findViewById(R.id.etAge)
        tilGender = findViewById(R.id.tilGender)
        spGender = findViewById(R.id.spGender)
        btnSignup = findViewById(R.id.btnSignup)

        tvAvatarEmoji.text = selectedAvatarEmoji
        etEmojiInput.setText(selectedAvatarEmoji)
    }

    private fun setupInputValidation() {
        // Name: Only letters and spaces
        val nameFilter = InputFilter { source, start, end, dest, dstart, dend ->
            val filtered = StringBuilder()
            for (i in start until end) {
                val char = source[i]
                if (char.isLetter() || char == ' ') {
                    filtered.append(char)
                }
            }
            if (filtered.length == end - start) {
                null // Accept all characters
            } else {
                filtered.toString() // Return filtered string
            }
        }
        etName.filters = arrayOf(nameFilter, InputFilter.LengthFilter(50))
        
        // Age: Only positive numbers, max 3 digits
        val ageFilter = InputFilter { source, start, end, dest, dstart, dend ->
            try {
                val input = dest.toString().substring(0, dstart) +
                           source.subSequence(start, end) +
                           dest.toString().substring(dend)

                if (input.isEmpty()) return@InputFilter null

                val age = input.toInt()
                if (age > 0 && age <= 150 && input.length <= 3) {
                    null // Accept
                } else {
                    "" // Reject
                }
            } catch (e: NumberFormatException) {
                "" // Reject invalid numbers
            }
        }
        etAge.filters = arrayOf(ageFilter)

        etEmojiInput.filters = arrayOf()
        
        // Add real-time validation
        etName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateName()
            }
        })
        
        etAge.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateAge()
            }
        })

        etEmojiInput.addTextChangedListener(object : TextWatcher {
            var selfChange = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (selfChange) return
                val rawInput = s?.toString().orEmpty()
                val sanitized = sanitizeEmojiInput(rawInput)
                if (sanitized != rawInput) {
                    selfChange = true
                    etEmojiInput.setText(sanitized)
                    etEmojiInput.setSelection(etEmojiInput.text?.length ?: 0)
                    selfChange = false
                }

                val emoji = if (sanitized.isNotBlank()) sanitized else defaultAvatarEmoji
                selectedAvatarEmoji = emoji
                tvAvatarEmoji.text = emoji
            }
        })
    }

    private fun setupGenderSpinner() {
        val genderOptions = resources.getStringArray(R.array.gender_options)
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, genderOptions)
        spGender.setAdapter(adapter)

        spGender.setOnItemClickListener { _, _, _, _ ->
            validateGender()
        }
    }

    private fun setupClickListeners() {
        tvAvatarEmoji.setOnClickListener {
            etEmojiInput.requestFocus()
            etEmojiInput.selectAll()
        }

        btnSignup.setOnClickListener {
            if (validateAllInputs()) {
                saveProfile()
            }
        }

        findViewById<TextView>(R.id.tvRestoreBackup).setOnClickListener {
            BackupRestoreHelper.startRestoreFlow(this)
        }
    }

    private fun sanitizeEmojiInput(input: String): String {
        if (input.isBlank()) return ""
        val iterator = BreakIterator.getCharacterInstance()
        iterator.setText(input)
        var start = iterator.first()
        var end = iterator.next()
        while (end != BreakIterator.DONE) {
            val cluster = input.substring(start, end)
            if (cluster.isNotBlank()) {
                return cluster.trim()
            }
            start = end
            end = iterator.next()
        }
        return ""
    }


    private fun validateName(): Boolean {
        val name = etName.text?.toString()?.trim()
        return if (name.isNullOrEmpty()) {
            tilName.error = "Name is required"
            false
        } else if (name.length < 2) {
            tilName.error = "Name must be at least 2 characters"
            false
        } else {
            tilName.error = null
            true
        }
    }

    private fun validateAge(): Boolean {
        val ageText = etAge.text?.toString()?.trim()
        return if (ageText.isNullOrEmpty()) {
            tilAge.error = "Age is required"
            false
        } else {
            val age = ageText.toIntOrNull()
            if (age == null || age < 1 || age > 150) {
                tilAge.error = "Age must be between 1 and 150"
                false
            } else {
                tilAge.error = null
                true
            }
        }
    }

    private fun validateGender(): Boolean {
        val gender = spGender.text?.toString()?.trim()
        return if (gender.isNullOrEmpty()) {
            tilGender.error = "Gender is required"
            false
        } else {
            tilGender.error = null
            true
        }
    }

    private fun validateAllInputs(): Boolean {
        val isNameValid = validateName()
        val isAgeValid = validateAge()
        val isGenderValid = validateGender()
        val isEmojiValid = validateEmoji()

        return isNameValid && isAgeValid && isGenderValid && isEmojiValid
    }

    private fun validateEmoji(): Boolean {
        val emoji = etEmojiInput.text?.toString()?.trim()
        return if (emoji.isNullOrEmpty()) {
            etEmojiInput.error = "Emoji is required"
            false
        } else {
            etEmojiInput.error = null
            true
        }
    }

    private fun saveProfile() {
        val name = etName.text?.toString()?.trim() ?: ""
        val age = etAge.text?.toString()?.trim()?.toIntOrNull() ?: 0
        val gender = spGender.text?.toString()?.trim() ?: ""
        val emoji = etEmojiInput.text?.toString()?.trim() ?: "\uD83D\uDE03"

        val profile = UserProfile(
            name = name,
            age = age,
            gender = gender,
            avatarEmoji = emoji
        )

        repository.saveUserProfile(profile)

        // Navigate to home
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        // Handle backup restore
        if (requestCode == 9002) { // 9002 is restore request code
            BackupRestoreHelper.handleActivityResult(this, requestCode, resultCode, data)

            if (resultCode == RESULT_OK) {
                Handler(Looper.getMainLooper()).postDelayed({
                    val profile = repository.getUserProfile()
                    if (profile != null) {
                        selectedAvatarEmoji = profile.avatarEmoji

                        tvAvatarEmoji.text = selectedAvatarEmoji
                        etEmojiInput.setText(selectedAvatarEmoji)
                        etName.setText(profile.name)
                        etAge.setText(profile.age.takeIf { it > 0 }?.toString() ?: "")
                        spGender.setText(profile.gender, false)

                        tilName.error = null
                        tilAge.error = null
                        tilGender.error = null

                        Toast.makeText(this, "Backup restored! Review your details and tap Sign Up to continue.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, "Restore failed. Please try again or fill out the form manually.", Toast.LENGTH_LONG).show()
                    }
                }, 100)
            }
            return
        }

        // Handle other activity results
        BackupRestoreHelper.handleActivityResult(this, requestCode, resultCode, data)
    }
}