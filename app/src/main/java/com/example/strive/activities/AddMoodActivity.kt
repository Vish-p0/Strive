package com.example.strive.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.strive.R

class AddMoodActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_mood)
        
        // Set up cancel button
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancel).setOnClickListener {
            finish()
        }
        
        // For now, this is a placeholder activity
        // In a real implementation, this would show an emoji picker and allow adding mood entries
    }
}