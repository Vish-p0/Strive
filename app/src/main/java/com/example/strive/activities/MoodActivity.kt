package com.example.strive.activities

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.strive.R
import com.example.strive.adapters.MoodAdapter
import com.example.strive.util.EmojiInfo
import com.example.strive.util.EmojiPalette
import com.example.strive.dialogs.EmojiGridAdapter
import com.example.strive.dialogs.MoodBottomSheetDialog
import com.example.strive.models.MoodEntry
import com.example.strive.repo.StriveRepository
import com.example.strive.views.EmojiSliderView
import com.example.strive.views.MoodChartView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigationrail.NavigationRailView
import com.google.android.material.snackbar.Snackbar
import java.util.*

class MoodActivity : AppCompatActivity() {

    private var bottomNav: BottomNavigationView? = null
    private var navRail: NavigationRailView? = null
    private lateinit var emojiSlider: EmojiSliderView
    private lateinit var coordinatorLayout: CoordinatorLayout
    private lateinit var moodRecyclerView: RecyclerView
    private lateinit var fabAddMood: FloatingActionButton
    private lateinit var btnExpandEmojiGrid: com.google.android.material.button.MaterialButton
    private lateinit var rvQuickEmojiGrid: RecyclerView
    private lateinit var layoutEmptyMoods: View
    private lateinit var moodChartView: MoodChartView
    private lateinit var chipGroupTimelineMood: ChipGroup
    private lateinit var moodAdapter: MoodAdapter
    private lateinit var quickEmojiAdapter: EmojiGridAdapter
    private lateinit var repository: StriveRepository
    private var lastAddedMoodId: String? = null
    private var isEmojiGridExpanded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mood)
        
        repository = StriveRepository.getInstance(this)
        coordinatorLayout = findViewById(R.id.coordinatorLayout)
        emojiSlider = findViewById(R.id.emojiSlider)
        moodRecyclerView = findViewById(R.id.rvMoodHistory)
        fabAddMood = findViewById(R.id.fabAddMood)
        btnExpandEmojiGrid = findViewById(R.id.btnExpandEmojiGrid)
        rvQuickEmojiGrid = findViewById(R.id.rvQuickEmojiGrid)
        layoutEmptyMoods = findViewById(R.id.layoutEmptyMoods)
        moodChartView = findViewById(R.id.moodChartView)
        chipGroupTimelineMood = findViewById(R.id.chipGroupTimelineMood)
        
        setupNavigation()
        setupEmojiSlider()
        setupMoodHistory()
        setupQuickEmojiGrid()
        setupExpandButton()
        setupMoodChart()
        setupFab()
        loadMoodHistory()
    }

    private fun setupFab() {
        fabAddMood.setOnClickListener {
            showMoodBottomSheet()
        }
    }

    private fun setupExpandButton() {
        btnExpandEmojiGrid.setOnClickListener {
            isEmojiGridExpanded = !isEmojiGridExpanded
            
            if (isEmojiGridExpanded) {
                rvQuickEmojiGrid.visibility = View.VISIBLE
                btnExpandEmojiGrid.text = "↑"
            } else {
                rvQuickEmojiGrid.visibility = View.GONE
                btnExpandEmojiGrid.text = "↓"
            }
        }
    }

    private fun setupQuickEmojiGrid() {
        quickEmojiAdapter = EmojiGridAdapter(EmojiPalette.EMOJIS) { emojiInfo ->
            // Handle emoji selection from quick grid
            val moodEntry = MoodEntry(
                id = UUID.randomUUID().toString(),
                emoji = emojiInfo.emoji,
                timestamp = System.currentTimeMillis(),
                note = null,
                score = emojiInfo.score
            )
            repository.addMood(moodEntry)
            loadMoodHistory()
            
            // Collapse the grid after selection
            isEmojiGridExpanded = false
            rvQuickEmojiGrid.visibility = View.GONE
            btnExpandEmojiGrid.text = "↓"
            
            val message = getString(R.string.mood_saved_successfully)
            Snackbar.make(coordinatorLayout, message, Snackbar.LENGTH_SHORT).show()
        }
        
        rvQuickEmojiGrid.apply {
            layoutManager = GridLayoutManager(this@MoodActivity, 5)
            adapter = quickEmojiAdapter
        }
    }

    private fun showMoodBottomSheet(moodEntry: MoodEntry? = null) {
        val dialog = MoodBottomSheetDialog.newInstance(
            moodEntry = moodEntry,
            onSave = { mood ->
                if (moodEntry != null) {
                    // Update existing mood
                    repository.updateMood(mood)
                } else {
                    // Add new mood
                    repository.addMood(mood)
                }
                loadMoodHistory()
                
                val message = getString(R.string.mood_saved_successfully)
                Snackbar.make(coordinatorLayout, message, Snackbar.LENGTH_SHORT).show()
            },
            onDelete = if (moodEntry != null) { { moodId ->
                repository.deleteMood(moodId)
                loadMoodHistory()
                
                Snackbar.make(coordinatorLayout, R.string.mood_deleted, Snackbar.LENGTH_SHORT)
                    .show()
            } } else null
        )
        
        dialog.show(supportFragmentManager, "MoodBottomSheet")
    }

    private fun setupMoodHistory() {
        moodAdapter = MoodAdapter { moodEntry ->
            showMoodBottomSheet(moodEntry)
        }
        
        moodRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MoodActivity)
            adapter = moodAdapter
        }
    }

    private fun loadMoodHistory() {
        val allMoods = repository.getAllMoods().sortedByDescending { it.timestamp }
        
        // Filter for today's moods only
        val calendar = Calendar.getInstance()
        val today = calendar.timeInMillis
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis
        
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val startOfNextDay = calendar.timeInMillis
        
        val todaysMoods = allMoods.filter { mood ->
            mood.timestamp >= startOfDay && mood.timestamp < startOfNextDay
        }
        
        if (todaysMoods.isEmpty()) {
            moodRecyclerView.visibility = View.GONE
            layoutEmptyMoods.visibility = View.VISIBLE
        } else {
            moodRecyclerView.visibility = View.VISIBLE
            layoutEmptyMoods.visibility = View.GONE
            moodAdapter.submitList(todaysMoods)
        }
        
        // Also update the chart with latest data
        loadMoodChartData()
    }

    private fun setupEmojiSlider() {
        emojiSlider.onEmojiSelectedListener = { emojiInfo ->
            quickLogMood(emojiInfo)
        }
    }

    private fun quickLogMood(emojiInfo: EmojiInfo) {
        val moodEntry = MoodEntry(
            id = generateMoodId(),
            emoji = emojiInfo.emoji,
            note = "Feeling ${emojiInfo.name.lowercase()}",
            timestamp = System.currentTimeMillis(),
            score = emojiInfo.score
        )
        
        repository.addMood(moodEntry)
        lastAddedMoodId = moodEntry.id
        
        // Refresh the mood history
        loadMoodHistory()
        
        // Show success message with undo option
        val message = getString(R.string.mood_logged_format, emojiInfo.name)
        val snackbar = Snackbar.make(coordinatorLayout, message, Snackbar.LENGTH_LONG)
            .setAction(R.string.undo) {
                undoLastMood()
            }
        snackbar.show()
    }

    private fun generateMoodId(): String {
        return "mood_${System.currentTimeMillis()}_${(0..999).random()}"
    }

    private fun undoLastMood() {
        lastAddedMoodId?.let { moodId ->
            repository.deleteMood(moodId)
            lastAddedMoodId = null
            
            // Refresh the mood history
            loadMoodHistory()
            
            Snackbar.make(coordinatorLayout, R.string.mood_deleted, Snackbar.LENGTH_SHORT)
                .show()
        }
    }

    private fun setupMoodChart() {
        // Set up timeline chip group for chart filtering
        chipGroupTimelineMood.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val timeframe = when (checkedIds.first()) {
                    R.id.chipToday -> MoodChartView.Timeframe.TODAY
                    R.id.chipDaily -> MoodChartView.Timeframe.DAILY
                    R.id.chipWeekly -> MoodChartView.Timeframe.WEEKLY
                    R.id.chipMonthly -> MoodChartView.Timeframe.MONTHLY
                    R.id.chipYearly -> MoodChartView.Timeframe.YEARLY
                    else -> MoodChartView.Timeframe.TODAY
                }
                moodChartView.setTimeframe(timeframe)
                loadMoodChartData()
            }
        }
        
        // Set initial timeframe to today
        moodChartView.setTimeframe(MoodChartView.Timeframe.TODAY)
        loadMoodChartData()
    }

    private fun loadMoodChartData() {
        val allMoods = repository.getAllMoods()
        moodChartView.setMoodData(allMoods)
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
                    startActivity(Intent(this, HabitsActivity::class.java))
                    finish()
                    true
                }
                R.id.menu_mood -> {
                    // Already on mood, do nothing
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
        
        // Set mood as selected
        bottomNav?.selectedItemId = R.id.menu_mood
        navRail?.selectedItemId = R.id.menu_mood
    }
}