package com.example.strive.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.example.strive.R
import com.example.strive.repo.StriveRepository
import com.google.android.material.button.MaterialButton

class OnboardingActivity : AppCompatActivity() {
    
    private lateinit var viewPager: ViewPager2
    private lateinit var indicatorLayout: LinearLayout
    private lateinit var btnNext: MaterialButton
    private lateinit var tvSkip: TextView
    private lateinit var repository: StriveRepository
    
    private val onboardingPages = listOf(
        OnboardingPage(
            imageRes = R.drawable.strive_logo,
            title = "Welcome to Strive ✨",
            description = "Every big goal starts with one small habit. Let's make consistency your superpower."
        ),
        OnboardingPage(
            imageRes = R.drawable.onboarding2,
            title = "Build Focused Routines ⏱️",
            description = "Plan your day, set priorities, and track habits that move you closer to your goals."
        ),
        OnboardingPage(
            imageRes = R.drawable.onboarding3,
            title = "Measure Progress. Achieve More. 🌟",
            description = "Watch your streaks grow, unlock milestones, and stay motivated with every win."
        )
    )
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)
        
        repository = StriveRepository.getInstance(this)
        
        initViews()
        setupViewPager()
        setupClickListeners()
        updateIndicators(0)
    }
    
    private fun initViews() {
        viewPager = findViewById(R.id.viewPager)
        indicatorLayout = findViewById(R.id.indicatorLayout)
        btnNext = findViewById(R.id.btnNext)
        tvSkip = findViewById(R.id.tvSkip)
    }
    
    private fun setupViewPager() {
        val adapter = OnboardingAdapter(onboardingPages)
        viewPager.adapter = adapter
        
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateIndicators(position)
                updateButtonText(position)
            }
        })
    }
    
    private fun setupClickListeners() {
        btnNext.setOnClickListener {
            val currentPage = viewPager.currentItem
            if (currentPage < onboardingPages.size - 1) {
                viewPager.currentItem = currentPage + 1
            } else {
                completeOnboarding()
            }
        }
        
        tvSkip.setOnClickListener {
            completeOnboarding()
        }
    }
    
    private fun updateIndicators(position: Int) {
        indicatorLayout.removeAllViews()
        
        for (i in onboardingPages.indices) {
            val indicator = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    if (i == position) 40 else 20,
                    20
                ).apply {
                    marginEnd = 16
                }
                background = ContextCompat.getDrawable(
                    this@OnboardingActivity,
                    if (i == position) R.drawable.indicator_active else R.drawable.indicator_inactive
                )
            }
            indicatorLayout.addView(indicator)
        }
    }
    
    private fun updateButtonText(position: Int) {
        btnNext.text = if (position == onboardingPages.size - 1) {
            "Start Today"
        } else {
            "Next"
        }
    }
    
    private fun completeOnboarding() {
        // Mark onboarding as completed
        val settings = repository.getSettings()
        repository.saveSettings(settings.copy(hasCompletedOnboarding = true))
        
        // Navigate to signup
        val intent = Intent(this, SignupActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    
    data class OnboardingPage(
        val imageRes: Int,
        val title: String,
        val description: String
    )
    
    private class OnboardingAdapter(
        private val pages: List<OnboardingPage>
    ) : androidx.recyclerview.widget.RecyclerView.Adapter<OnboardingAdapter.ViewHolder>() {
        
        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
            val view = android.view.LayoutInflater.from(parent.context)
                .inflate(R.layout.item_onboarding_page, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(pages[position])
        }
        
        override fun getItemCount() = pages.size
        
        class ViewHolder(itemView: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
            private val ivImage: ImageView = itemView.findViewById(R.id.ivOnboardingImage)
            private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
            private val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
            
            fun bind(page: OnboardingPage) {
                ivImage.setImageResource(page.imageRes)
                tvTitle.text = page.title
                tvDescription.text = page.description
            }
        }
    }
}