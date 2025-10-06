package com.example.strive.activities

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.graphics.Color
import com.example.strive.R
// removed unused model imports
import com.example.strive.repo.StriveRepository
import com.example.strive.util.DateUtils
import com.example.strive.util.EmojiPalette
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.navigationrail.NavigationRailView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import java.util.*
import kotlin.math.min
import kotlin.random.Random

class HomeActivity : AppCompatActivity(), com.example.strive.repo.StriveRepository.DataChangeListener {

    private lateinit var repository: StriveRepository
    private lateinit var tvGreeting: TextView
    private lateinit var tvProgressMessage: TextView
    private lateinit var tvMotivationQuote: TextView
    private lateinit var tvMotivationEmoji: TextView
    private lateinit var tvProgressPercent: TextView
    private lateinit var tvTasksCompleted: TextView
    private lateinit var tvHabitsProgress: TextView
    private lateinit var progressRingHabits: ProgressBar
    private lateinit var rvFavoriteHabitsVertical: RecyclerView
    private lateinit var cardMotivationBanner: MaterialCardView
    private lateinit var cardLatestMood: MaterialCardView
    private lateinit var btnAddMood: MaterialButton
    private lateinit var btnSeeAllHabits: MaterialButton
    private lateinit var tvMoodBannerTitle: TextView
    private lateinit var tvMoodBannerMessage: TextView
    private lateinit var tvMoodEmoji: TextView
    private lateinit var tvMoodLabel: TextView
    private lateinit var tvMoodTimestamp: TextView

    private val motivationMessages = listOf(
        MotivationMessage("Small steps every day add up.", "💪"),
        MotivationMessage("Consistency beats intensity.", "🔥"),
        MotivationMessage("Progress, not perfection.", "✨"),
        MotivationMessage("You’re stronger than you think.", "🛡️"),
        MotivationMessage("One habit at a time.", "🧩"),
        MotivationMessage("Future you will thank you.", "⏳"),
        MotivationMessage("Believe you can and you’re halfway there.", "🚀"),
        MotivationMessage("Celebrate every win, no matter how small.", "🎉"),
        MotivationMessage("Keep showing up for yourself.", "🌟")
    )

    private var currentMotivationIndex = -1
    private var currentMoodTone: MoodTone? = null

    private data class MotivationMessage(val quote: String, val emoji: String)

    private data class MoodTone(
        val backgroundColor: Int,
        val textColor: Int,
        val accentColor: Int,
        val messages: List<String>
    )
    
    private var bottomNav: BottomNavigationView? = null
    private var navRail: NavigationRailView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        
        repository = StriveRepository.getInstance(this)
        
        initViews()
        setupNavigation()
        setupClickListeners()
        loadData()
    }

    private fun initViews() {
        tvGreeting = findViewById(R.id.tvGreeting)
        tvProgressMessage = findViewById(R.id.tvProgressMessage)
        tvMotivationQuote = findViewById(R.id.tvMotivationQuote)
        tvMotivationEmoji = findViewById(R.id.tvMotivationEmoji)
        tvProgressPercent = findViewById(R.id.tvProgressPercent)
        tvTasksCompleted = findViewById(R.id.tvTasksCompleted)
        tvHabitsProgress = findViewById(R.id.tvHabitsProgress)
        progressRingHabits = findViewById(R.id.progressRingHabits)
    cardMotivationBanner = findViewById(R.id.cardMotivationBanner)
    cardLatestMood = findViewById(R.id.cardLatestMood)
        btnAddMood = findViewById(R.id.btnAddMood)
        btnSeeAllHabits = findViewById(R.id.btnSeeAllHabits)
        rvFavoriteHabitsVertical = findViewById(R.id.rvFavoriteHabitsVertical)
        tvMoodBannerTitle = findViewById(R.id.tvMoodBannerTitle)
        tvMoodBannerMessage = findViewById(R.id.tvMoodBannerMessage)
        tvMoodEmoji = findViewById(R.id.tvMoodEmoji)
        tvMoodLabel = findViewById(R.id.tvMoodLabel)
        tvMoodTimestamp = findViewById(R.id.tvMoodTimestamp)
        
        // Navigation components (one will be null depending on orientation)
        bottomNav = findViewById(R.id.bottomNav)
        navRail = findViewById(R.id.navRail)
        
        // Setup RecyclerView
        rvFavoriteHabitsVertical.layoutManager = LinearLayoutManager(this)
    }

    private fun setupNavigation() {
        val navigationListener = { item: MenuItem ->
            when (item.itemId) {
                R.id.menu_home -> {
                    // Already on home, do nothing
                    true
                }
                R.id.menu_habits -> {
                    startActivity(Intent(this, HabitsActivity::class.java))
                    true
                }
                R.id.menu_mood -> {
                    startActivity(Intent(this, MoodActivity::class.java))
                    true
                }
                R.id.menu_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }
        
        bottomNav?.setOnItemSelectedListener(navigationListener)
        navRail?.setOnItemSelectedListener(navigationListener)
        
        // Set home as selected
        bottomNav?.selectedItemId = R.id.menu_home
        navRail?.selectedItemId = R.id.menu_home
    }

    private fun setupClickListeners() {
        val openMood: (android.view.View) -> Unit = {
            startActivity(Intent(this, MoodActivity::class.java))
        }
    val motivationClick: (android.view.View) -> Unit = { showNextMotivationQuote() }
    cardMotivationBanner.setOnClickListener(motivationClick)
    tvMotivationQuote.setOnClickListener(motivationClick)
    tvMotivationEmoji.setOnClickListener(motivationClick)
        cardLatestMood.setOnClickListener(openMood)
        btnAddMood.setOnClickListener(openMood)
        btnSeeAllHabits.setOnClickListener {
            startActivity(Intent(this, HabitsActivity::class.java))
        }
    }

    private fun loadData() {
        updateGreeting()
        updateHabitsProgress()
        updateFavoriteHabits()
        updateMoodBanner()
        updateBanner()
        // Removed today list and upcoming reminders
    }

    private fun updateGreeting() {
        val profile = repository.getUserProfile()
        val name = profile?.name ?: getString(R.string.default_user)
        
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        
        val greeting = when (hour) {
            in 5..11 -> getString(R.string.greeting_morning, name)
            in 12..16 -> getString(R.string.greeting_afternoon, name)
            in 17..20 -> getString(R.string.greeting_evening, name)
            else -> getString(R.string.greeting_night, name)
        }
        
        tvGreeting.text = greeting
    }

    private fun updateHabitsProgress() {
        val habits = repository.getAllHabits().filter { it.enabled }
        val today = DateUtils.nowDateString()
        val todayTicks = repository.getTicksForDate(today)
        
        val completedCount = habits.count { habit ->
            val tick = todayTicks.firstOrNull { it.habitId == habit.id }
            val progress = (tick?.amount ?: 0).toFloat() / habit.targetPerDay
            progress >= 1.0f
        }
        
        tvHabitsProgress.text = getString(R.string.habits_progress, completedCount, habits.size)
        
        // Use average of capped per-habit completion for the ring
        val overallPercent = computeAverageCompletionPercent(habits, todayTicks)
        progressRingHabits.progress = overallPercent
    }

    private fun updateBanner() {
        val habits = repository.getAllHabits().filter { it.enabled }
        val today = DateUtils.nowDateString()
        val todayTicks = repository.getTicksForDate(today)

        var tasksCompleted = 0
        habits.forEach { h ->
            val done = todayTicks.firstOrNull { it.habitId == h.id }?.amount ?: 0
            if (done >= h.targetPerDay) tasksCompleted += 1
        }

        // Calculate percent as average of capped per-habit completion
        val percent = computeAverageCompletionPercent(habits, todayTicks)
        tvProgressPercent.text = "$percent% complete"
        tvTasksCompleted.text = "$tasksCompleted out of ${habits.size} habits completed"

        val message = when {
            percent <= 25 -> "Let's complete all the habits today"
            percent in 26..50 -> "Halfway done, just a bit to go"
            percent in 51..99 -> "Almost there, I know you can do it"
            else -> "Let's go, you finished it!"
        }
        tvProgressMessage.text = message

        ensureMotivationQuote()
    }

    private fun computeAverageCompletionPercent(
        habits: List<com.example.strive.models.Habit>,
        todayTicks: List<com.example.strive.models.HabitTick>
    ): Int {
        if (habits.isEmpty()) return 0
        var sum = 0f
        habits.forEach { h ->
            val done = todayTicks.firstOrNull { it.habitId == h.id }?.amount ?: 0
            val ratio = min(done.toFloat() / h.targetPerDay, 1f)
            sum += ratio
        }
        val avg = (sum / habits.size) * 100f
        return avg.toInt()
    }

    private fun updateFavoriteHabits() {
        val favs = repository.getAllHabits().filter { it.enabled && it.isStarred }
        val todayTicks = repository.getTicksForDate(DateUtils.nowDateString())
        rvFavoriteHabitsVertical.adapter = FavoriteAdapter(favs, todayTicks,
            onQuickAdd = { habit ->
                repository.addTick(habit.id, habit.defaultIncrement)
                updateHabitsProgress(); updateBanner(); updateFavoriteHabits()
            },
            onOpen = { habit ->
                startActivity(HabitDetailActivity.newIntent(this, habit.id))
            }
        )
    }

    private fun ensureMotivationQuote() {
        if (motivationMessages.isEmpty()) return
        if (currentMotivationIndex !in motivationMessages.indices) {
            currentMotivationIndex = Random.nextInt(motivationMessages.size)
        }
        applyMotivationMessage(currentMotivationIndex)
    }

    private fun showNextMotivationQuote() {
        if (motivationMessages.isEmpty()) return
        ensureMotivationQuote()
        val nextIndex = if (motivationMessages.size == 1) {
            currentMotivationIndex
        } else {
            var candidate: Int
            do {
                candidate = Random.nextInt(motivationMessages.size)
            } while (candidate == currentMotivationIndex)
            candidate
        }
        currentMotivationIndex = nextIndex
        applyMotivationMessage(nextIndex)
    }

    private fun applyMotivationMessage(index: Int) {
        if (motivationMessages.isEmpty()) return
        val message = motivationMessages[index]
        tvMotivationQuote.text = message.quote
        tvMotivationEmoji.text = message.emoji
        currentMoodTone?.let { applyMotivationTone(it) }
    }

    private fun updateMoodBanner() {
        val latestMood = repository.getAllMoods().firstOrNull()
        if (latestMood == null) {
            val defaultTone = MoodTone(
                backgroundColor = Color.parseColor("#F5F0FF"),
                textColor = Color.parseColor("#49454F"),
                accentColor = Color.parseColor("#6750A4"),
                messages = listOf(getString(R.string.mood_banner_empty_message))
            )
            currentMoodTone = defaultTone
            applyMoodTone(defaultTone)
            applyMotivationTone(defaultTone)
            tvMoodBannerTitle.text = getString(R.string.mood_banner_empty_title)
            tvMoodBannerMessage.text = getString(R.string.mood_banner_empty_message)
            tvMoodEmoji.text = "🙂"
            tvMoodLabel.text = getString(R.string.mood_banner_empty_status)
            tvMoodTimestamp.text = getString(R.string.mood_banner_empty_cta)
            return
        }

        val tone = resolveMoodTone(latestMood.score)
        currentMoodTone = tone
        applyMoodTone(tone)
        applyMotivationTone(tone)

        val emojiName = EmojiPalette.getEmojiName(latestMood.emoji)
        val relative = android.text.format.DateUtils.getRelativeTimeSpanString(
            latestMood.timestamp,
            System.currentTimeMillis(),
            android.text.format.DateUtils.MINUTE_IN_MILLIS
        ).toString()

        tvMoodBannerTitle.text = getString(R.string.mood_banner_title)
        tvMoodBannerMessage.text = latestMood.note?.takeIf { it.isNotBlank() } ?: tone.messages.random()
        tvMoodEmoji.text = latestMood.emoji
        tvMoodLabel.text = getString(R.string.mood_banner_label_format, emojiName)
        tvMoodTimestamp.text = getString(R.string.mood_banner_last_logged, relative)
    }

    private fun applyMoodTone(tone: MoodTone) {
        cardLatestMood.setCardBackgroundColor(tone.backgroundColor)
        tvMoodBannerTitle.setTextColor(tone.textColor)
        tvMoodBannerMessage.setTextColor(tone.textColor)
        tvMoodEmoji.setTextColor(tone.accentColor)
        tvMoodLabel.setTextColor(tone.accentColor)
        tvMoodTimestamp.setTextColor(ColorUtils.setAlphaComponent(tone.textColor, (0.72f * 255).toInt()))

        val accentStateList = ColorStateList.valueOf(tone.accentColor)
        btnAddMood.setTextColor(tone.accentColor)
        btnAddMood.iconTint = accentStateList
        btnAddMood.strokeColor = accentStateList
        val backgroundTint = ColorUtils.setAlphaComponent(tone.accentColor, (0.12f * 255).toInt())
        btnAddMood.backgroundTintList = ColorStateList.valueOf(backgroundTint)
        btnAddMood.rippleColor = ColorStateList.valueOf(ColorUtils.setAlphaComponent(tone.accentColor, (0.24f * 255).toInt()))
    }

    private fun applyMotivationTone(tone: MoodTone) {
        val baseColor = ColorUtils.setAlphaComponent(tone.accentColor, (0.12f * 255).toInt())
        cardMotivationBanner.setCardBackgroundColor(baseColor)
        tvMotivationQuote.setTextColor(tone.textColor)
        tvMotivationEmoji.setTextColor(tone.accentColor)
    }

    private fun resolveMoodTone(score: Int): MoodTone {
        return when {
            score >= 4 -> MoodTone(
                backgroundColor = Color.parseColor("#E7F5EF"),
                textColor = Color.parseColor("#1B4332"),
                accentColor = Color.parseColor("#2D6A4F"),
                messages = listOf(
                    getString(R.string.mood_quote_positive_one),
                    getString(R.string.mood_quote_positive_two),
                    getString(R.string.mood_quote_positive_three)
                )
            )
            score == 3 -> MoodTone(
                backgroundColor = Color.parseColor("#E8F0FE"),
                textColor = Color.parseColor("#1A237E"),
                accentColor = Color.parseColor("#3949AB"),
                messages = listOf(
                    getString(R.string.mood_quote_neutral_one),
                    getString(R.string.mood_quote_neutral_two),
                    getString(R.string.mood_quote_neutral_three)
                )
            )
            score == 2 -> MoodTone(
                backgroundColor = Color.parseColor("#FFF4E6"),
                textColor = Color.parseColor("#8C3A00"),
                accentColor = Color.parseColor("#FB8C00"),
                messages = listOf(
                    getString(R.string.mood_quote_low_one),
                    getString(R.string.mood_quote_low_two),
                    getString(R.string.mood_quote_low_three)
                )
            )
            else -> MoodTone(
                backgroundColor = Color.parseColor("#FCE4EC"),
                textColor = Color.parseColor("#880E4F"),
                accentColor = Color.parseColor("#D81B60"),
                messages = listOf(
                    getString(R.string.mood_quote_lower_one),
                    getString(R.string.mood_quote_lower_two),
                    getString(R.string.mood_quote_lower_three)
                )
            )
        }
    }
    private class FavoriteAdapter(
        val items: List<com.example.strive.models.Habit>,
        val todayTicks: List<com.example.strive.models.HabitTick>,
        val onQuickAdd: (com.example.strive.models.Habit) -> Unit,
        val onOpen: (com.example.strive.models.Habit) -> Unit
    ) : RecyclerView.Adapter<FavoriteAdapter.VH>() {
        class VH(v: android.view.View) : RecyclerView.ViewHolder(v) {
            val title: TextView = v.findViewById(R.id.tvHabitTitle)
            val progressText: TextView = v.findViewById(R.id.tvProgressText)
            val progressBarChart: android.view.ViewGroup = v.findViewById(R.id.progressBarChart)
            val btnAdd: MaterialButton = v.findViewById(R.id.btnAdd)
            val divider: android.view.View = v.findViewById(R.id.divider)
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): VH {
            val view = android.view.LayoutInflater.from(parent.context)
                .inflate(R.layout.item_favorite_habit_vertical, parent, false)
            return VH(view)
        }

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val h = items[position]
            val tick = todayTicks.firstOrNull { it.habitId == h.id }
            val current = tick?.amount ?: 0
            val target = h.targetPerDay
            val progress = min(current.toFloat() / target, 1f)
            
            holder.title.text = "${h.emoji} ${h.title}"
            
            // Build progress text
            val progressText = when {
                progress >= 1.0f -> "done today!"
                h.unit == "COUNT" -> "$current/${target} times"
                h.unit == "ML" -> "$current/${target} ml"
                h.unit == "LITERS" -> "$current/${target} L"
                h.unit == "MINUTES" -> "$current/${target} mins"
                h.unit == "STEPS" -> "$current/${target} steps"
                else -> "$current/${target}"
            }
            holder.progressText.text = progressText
            
            // Build progress bar chart
            holder.progressBarChart.removeAllViews()
            val totalBlocks = 9
            val filledBlocks = (progress * totalBlocks).toInt()
            
            // Determine colors based on completion status
            val isCompleted = progress >= 1.0f
            val filledColor = if (isCompleted) {
                android.graphics.Color.parseColor("#4CAF50") // Green when completed
            } else {
                android.graphics.Color.parseColor("#6750A4") // Purple when not completed
            }
            val emptyColor = if (isCompleted) {
                android.graphics.Color.parseColor("#CAC4D0") // Light purple when completed
            } else {
                android.graphics.Color.parseColor("#E0E0E0") // Light grey when not completed
            }
            
            for (i in 0 until totalBlocks) {
                val block = TextView(holder.itemView.context)
                block.text = if (i < filledBlocks) "▓" else "░"
                block.textSize = 14f
                block.setPadding(1, 0, 1, 0)
                block.setTextColor(
                    if (i < filledBlocks) filledColor else emptyColor
                )
                holder.progressBarChart.addView(block)
            }
            
            // Hide divider for last item
            holder.divider.visibility = if (position == items.size - 1) android.view.View.GONE else android.view.View.VISIBLE
            
            holder.itemView.setOnClickListener { onOpen(h) }
            holder.btnAdd.setOnClickListener { onQuickAdd(h) }
        }
    }

    // Removed updateTodayHabits and updateUpcomingReminders

    override fun onResume() {
        super.onResume()
        loadData() // Refresh data when returning to the activity
        repository.addListener(this)
    }

    override fun onPause() {
        super.onPause()
        repository.removeListener(this)
    }

    override fun onTicksChanged() {
        // Called from repository on any tick changes; refresh dependent UI
        runOnUiThread {
            updateHabitsProgress()
            updateBanner()
            updateFavoriteHabits()
        }
    }
}