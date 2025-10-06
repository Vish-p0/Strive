package com.example.strive.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.strive.R
import com.example.strive.repo.StriveRepository

class SplashActivity : AppCompatActivity() {
    
    private companion object {
        const val SPLASH_DELAY = 3000L // 3 seconds
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        
        // Hide system bars for immersive splash
        supportActionBar?.hide()
        
        Handler(Looper.getMainLooper()).postDelayed({
            navigateToNextScreen()
        }, SPLASH_DELAY)
    }
    
    private fun navigateToNextScreen() {
        val repository = StriveRepository.getInstance(this)
        val userProfile = repository.getUserProfile()
        val settings = repository.getSettings()

        val targetActivity = when {
            userProfile == null && !settings.hasCompletedOnboarding -> OnboardingActivity::class.java
            userProfile == null -> SignupActivity::class.java
            else -> HomeActivity::class.java
        }

        val intent = Intent(this, targetActivity)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}