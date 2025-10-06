package com.example.strive.activities

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.strive.R
import com.example.strive.models.Habit
import com.example.strive.repo.StriveRepository
import com.example.strive.util.DateUtils
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigationrail.NavigationRailView
import com.google.android.material.progressindicator.CircularProgressIndicator
import android.animation.ValueAnimator
import android.view.animation.DecelerateInterpolator
import com.example.strive.views.HabitPieChartView

class HabitsActivity : AppCompatActivity() {

    private var bottomNav: BottomNavigationView? = null
    private var navRail: NavigationRailView? = null
    private lateinit var recycler: RecyclerView
    private lateinit var adapter: HabitsAdapter
    private lateinit var repo: StriveRepository
    private lateinit var chipFilters: ChipGroup
    private lateinit var fab: FloatingActionButton
    private lateinit var progressDaily: CircularProgressIndicator
    private lateinit var tvDailyProgressPercent: TextView
    private lateinit var tvDailyProgressMessage: TextView
    private lateinit var confettiOverlay: android.widget.FrameLayout
    private lateinit var pieChartHabits: HabitPieChartView
    private var dailyProgressAnimator: ValueAnimator? = null
    private var lastDailyProgress = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_habits)
        repo = StriveRepository.getInstance(this)
        initHeader()
        setupNavigation()
        initFilters()
        initList()
        initFab()
    }

    override fun onResume() {
        super.onResume()
        // Refresh the list to show any updates made in HabitDetailActivity
        refreshList()
    }

    override fun onDestroy() {
        dailyProgressAnimator?.cancel()
        super.onDestroy()
    }

    private fun setupNavigation() {
        bottomNav = findViewById(R.id.bottomNav)
        navRail = findViewById(R.id.navRail)
        
        val navigationListener = { item: MenuItem ->
            when (item.itemId) {
                R.id.menu_home -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                    true
                }
                R.id.menu_habits -> {
                    // Already on habits, do nothing
                    true
                }
                R.id.menu_mood -> {
                    startActivity(Intent(this, MoodActivity::class.java))
                    finish()
                    true
                }
                R.id.menu_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
        
        bottomNav?.setOnItemSelectedListener(navigationListener)
        navRail?.setOnItemSelectedListener(navigationListener)
        
        // Set habits as selected
        bottomNav?.selectedItemId = R.id.menu_habits
        navRail?.selectedItemId = R.id.menu_habits
    }

    private fun initList() {
        recycler = findViewById(R.id.rvHabits)
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.setHasFixedSize(false)
        recycler.isNestedScrollingEnabled = false
        adapter = HabitsAdapter(
            onQuickAdd = { habit, sourceView ->
                // Add the tick to repository
                repo.addTick(habit.id, habit.defaultIncrement)
                
                // Refresh to show the updated count
                // Using post to ensure the repository update is completed first
                recycler.post {
                    refreshList()
                }
                
                // Show user feedback
                val message = "Added ${habit.defaultIncrement} to ${habit.title}"
                val coordinatorLayout = findViewById<androidx.coordinatorlayout.widget.CoordinatorLayout>(R.id.coordinatorLayout)
                com.google.android.material.snackbar.Snackbar.make(coordinatorLayout, message, com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show()

                showCheckInCelebration(sourceView)
            },
            onCardClick = { habit ->
                val intent = HabitDetailActivity.newIntent(this, habit.id)
                startActivity(intent)
            },
            repository = repo
        )
        recycler.adapter = adapter
        refreshList()
    }

    private fun initHeader() {
        progressDaily = findViewById(R.id.progressDaily)
        progressDaily.max = 100
        tvDailyProgressPercent = findViewById(R.id.tvDailyProgressPercent)
        tvDailyProgressMessage = findViewById(R.id.tvDailyProgressMessage)
        tvDailyProgressPercent.text = getString(R.string.percent_format, 0)
        tvDailyProgressMessage.text = getString(R.string.habits_empty_message)
        confettiOverlay = findViewById(R.id.confettiOverlay)
        pieChartHabits = findViewById(R.id.pieChartHabits)
    }

    private fun initFilters() {
        chipFilters = findViewById(R.id.chipGroupHabitFilters)
        chipFilters.setOnCheckedStateChangeListener { _, _ ->
            refreshList()
        }
        // Ensure one is checked by default (All)
        if (chipFilters.checkedChipId == -1 && chipFilters.childCount > 0) {
            chipFilters.check(chipFilters.getChildAt(0).id)
        }
        
        // Wire search functionality
        val searchInput = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etSearchHabits)
        searchInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                refreshList()
            }
        })
    }

    private fun initFab() {
        fab = findViewById(R.id.fabAddHabit)
        fab.setOnClickListener {
            openAddHabitModal()
        }
    }
    
    private fun openAddHabitModal() {
        val addHabitFragment = com.example.strive.fragments.AddHabitBottomSheetFragment.newInstance(
            onHabitCreated = {
                refreshList() // Refresh the list when a new habit is created
            }
        )
        addHabitFragment.show(supportFragmentManager, "add_habit")
    }

    private fun refreshList() {
        val all = repo.getAllHabits()
        val today = DateUtils.nowDateString()
        val todayTicks = repo.getTicksForDate(today)
        val progressByHabit = all.associate { h ->
            val tick = todayTicks.firstOrNull { it.habitId == h.id }
            h.id to (tick?.amount ?: 0)
        }
        
        // Get search text
        val searchText = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etSearchHabits)?.text?.toString()?.trim() ?: ""
        
        // Filter by search text
        val searchFiltered = if (searchText.isNotEmpty()) {
            all.filter { 
                it.title.contains(searchText, ignoreCase = true) 
            }
        } else {
            all
        }
        
        // Filter by chip selection
        val checkedId = if (::chipFilters.isInitialized) chipFilters.checkedChipId else -1
        val filtered = when (checkedId) {
            -1 -> searchFiltered
            else -> {
                val chip = chipFilters.findViewById<com.google.android.material.chip.Chip>(checkedId)
                val chipText = chip?.text?.toString() ?: ""
                when (chipText) {
                    getString(R.string.filter_all) -> searchFiltered
                    getString(R.string.filter_favorites) -> searchFiltered.filter { it.isStarred }
                    else -> searchFiltered
                }
            }
        }
        adapter.submit(filtered, progressByHabit)
        updateDailyProgress(all, progressByHabit)
        updatePieChart(all, progressByHabit)
    }

    private fun updateDailyProgress(allHabits: List<Habit>, progressMap: Map<String, Int>) {
        if (!::progressDaily.isInitialized) return
        val enabled = allHabits.filter { it.enabled }
        if (enabled.isEmpty()) {
            animateDailyProgress(0)
            tvDailyProgressMessage.text = getString(R.string.habits_empty_message)
            return
        }

        val todayCompletion = enabled.map { habit ->
            val current = progressMap[habit.id]?.toFloat() ?: 0f
            val ratio = if (habit.targetPerDay > 0) (current / habit.targetPerDay).coerceAtMost(1f) else 0f
            ratio
        }
        val averageRatio = if (todayCompletion.isEmpty()) 0f else todayCompletion.sum() / todayCompletion.size
        val percent = (averageRatio * 100f).toInt()

        animateDailyProgress(percent)

        val message = when {
            percent <= 25 -> getString(R.string.daily_progress_message_start)
            percent in 26..50 -> getString(R.string.daily_progress_message_keep_going)
            percent in 51..99 -> getString(R.string.daily_progress_message_almost)
            else -> getString(R.string.daily_progress_message_complete)
        }
        tvDailyProgressMessage.text = message
    }

    private fun animateDailyProgress(targetPercent: Int) {
        val clampedTarget = targetPercent.coerceIn(0, 100)
        dailyProgressAnimator?.cancel()
        val start = lastDailyProgress
        dailyProgressAnimator = ValueAnimator.ofInt(start, clampedTarget).apply {
            duration = 800
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                val value = animator.animatedValue as Int
                progressDaily.progress = value
                tvDailyProgressPercent.text = getString(R.string.percent_format, value)
            }
            start()
        }
        progressDaily.animate().rotationBy(360f).setDuration(800).start()
        lastDailyProgress = clampedTarget
    }
    
    private fun updatePieChart(allHabits: List<Habit>, progressMap: Map<String, Int>) {
        if (!::pieChartHabits.isInitialized) return
        
        val enabled = allHabits.filter { it.enabled }
        val pieData = enabled.map { habit ->
            val progress = progressMap[habit.id]?.toFloat() ?: 0f
            HabitPieChartView.HabitSlice(
                habitName = habit.title,
                progress = progress,
                target = habit.targetPerDay.toFloat(),
                color = habit.color
            )
        }.filter { it.target > 0 } // Only show habits with targets
        
        pieChartHabits.setData(pieData)
    }

    private fun showCheckInCelebration(anchor: View) {
        if (!::confettiOverlay.isInitialized) return
        val overlayPosition = IntArray(2)
        val anchorPosition = IntArray(2)
        confettiOverlay.getLocationOnScreen(overlayPosition)
        anchor.getLocationOnScreen(anchorPosition)

        val startX = anchorPosition[0] - overlayPosition[0] + anchor.width / 2f
        val startY = anchorPosition[1] - overlayPosition[1] + anchor.height / 2f
        val emojis = listOf("🎉", "✨", "💫", "🌟")

        repeat(10) { index ->
            val confetti = TextView(this).apply {
                text = emojis[index % emojis.size]
                textSize = if (index % 2 == 0) 28f else 22f
                alpha = 0f
            }
            val params = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
            )
            params.leftMargin = startX.toInt()
            params.topMargin = startY.toInt()
            confettiOverlay.addView(confetti, params)

            confetti.scaleX = 0f
            confetti.scaleY = 0f
            confetti.translationX = 0f
            confetti.translationY = 0f

            val horizontal = kotlin.random.Random.nextInt(-160, 161)
            val vertical = kotlin.random.Random.nextInt(160, 280)
            confetti.animate()
                .setStartDelay((index * 18).toLong())
                .setDuration(520)
                .alpha(1f)
                .translationX(horizontal.toFloat())
                .translationY((-vertical).toFloat())
                .scaleX(1.2f)
                .scaleY(1.2f)
                .setInterpolator(DecelerateInterpolator())
                .withEndAction {
                    confetti.animate()
                        .setDuration(240)
                        .alpha(0f)
                        .translationY((-vertical - 80).toFloat())
                        .setInterpolator(DecelerateInterpolator())
                        .withEndAction {
                            confettiOverlay.removeView(confetti)
                        }
                        .start()
                }
                .start()
        }
    }

    private class HabitsAdapter(
        val onQuickAdd: (Habit, View) -> Unit,
        val onCardClick: (Habit) -> Unit,
        val repository: StriveRepository
    ) : RecyclerView.Adapter<HabitsAdapter.VH>() {
    private val items = mutableListOf<Habit>()
        private var progress: Map<String, Int> = emptyMap()

        class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvTitle: TextView = v.findViewById(R.id.tvTitle)
            val tvSubtitle: TextView = v.findViewById(R.id.tvSubtitle)
            val ivEmoji: TextView = v.findViewById(R.id.ivEmoji)
            val btnPlus: com.google.android.material.button.MaterialButton = v.findViewById(R.id.btnPlus)
            val ivStar: ImageView = v.findViewById(R.id.ivStar)
            val progressBar: android.widget.ProgressBar = v.findViewById(R.id.progressBar)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_habit, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val h = items[position]
            holder.tvTitle.text = h.title
            holder.ivEmoji.text = h.emoji
            val value = progress[h.id] ?: 0
            holder.tvSubtitle.text = when (h.unit) {
                "ML" -> "${value} mL / ${h.targetPerDay} mL"
                "MINUTES" -> "${value} / ${h.targetPerDay} min"
                "STEPS" -> "${value} / ${h.targetPerDay} steps"
                else -> "${value} / ${h.targetPerDay}"
            }
            val pct = if (h.targetPerDay > 0) (value * 100 / h.targetPerDay).coerceIn(0, 100) else 0
            holder.progressBar.progress = pct
            holder.ivStar.setImageResource(if (h.isStarred) R.drawable.ic_star_filled else R.drawable.ic_star_outline)
            holder.itemView.setOnClickListener { onCardClick(h) }
            holder.btnPlus.setOnClickListener { onQuickAdd(h, holder.btnPlus) }
            holder.ivStar.setOnClickListener {
                val updated = h.copy(isStarred = !h.isStarred)
                repository.updateHabit(updated)
                items[holder.bindingAdapterPosition] = updated
                notifyItemChanged(holder.bindingAdapterPosition)
            }
        }

        override fun getItemCount(): Int = items.size

        fun submit(list: List<Habit>, progressMap: Map<String, Int>) {
            val oldItems = items.toList()
            val oldProgress = progress
            progress = progressMap
            
            // compute minimal change set
            val diff = androidx.recyclerview.widget.DiffUtil.calculateDiff(object : androidx.recyclerview.widget.DiffUtil.Callback() {
                override fun getOldListSize() = oldItems.size
                override fun getNewListSize() = list.size
                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    return oldItems[oldItemPosition].id == list[newItemPosition].id
                }
                override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    val o = oldItems[oldItemPosition]
                    val n = list[newItemPosition]
                    val oldProgressValue = oldProgress[o.id] ?: 0
                    val newProgressValue = progressMap[n.id] ?: 0
                    return o == n && oldProgressValue == newProgressValue
                }
            })
            items.clear()
            items.addAll(list)
            diff.dispatchUpdatesTo(this)
        }
    }
}