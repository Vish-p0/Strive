package com.example.strive.repo

import android.content.Context
import android.content.SharedPreferences
import com.example.strive.models.*
import com.example.strive.util.SerializationUtils
import com.example.strive.util.DateUtils
// removed unused coroutine/json imports

class StriveRepository private constructor(private val prefs: SharedPreferences, private val context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: StriveRepository? = null

        fun getInstance(context: Context): StriveRepository {
            return INSTANCE ?: synchronized(this) {
                val prefs = context.getSharedPreferences("strive_prefs", Context.MODE_PRIVATE)
                val instance = StriveRepository(prefs, context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    // In-memory caches
    private var userProfile: UserProfile? = null
    private var habits: MutableList<Habit>? = null
    private var ticks: MutableList<HabitTick>? = null
    private var moods: MutableList<MoodEntry>? = null
    private var settings: AppSettings? = null

    // Observers for lightweight data change notifications
    interface DataChangeListener {
        fun onTicksChanged()
    }
    private val listeners = mutableListOf<java.lang.ref.WeakReference<DataChangeListener>>()

    fun addListener(listener: DataChangeListener) {
        // Avoid duplicates and clean dead refs
        listeners.removeAll { it.get() == null || it.get() === listener }
        listeners.add(java.lang.ref.WeakReference(listener))
    }

    fun removeListener(listener: DataChangeListener) {
        listeners.removeAll { it.get() == null || it.get() === listener }
    }

    private fun notifyTicksChanged() {
        val itor = listeners.iterator()
        while (itor.hasNext()) {
            val l = itor.next().get()
            if (l == null) {
                itor.remove()
            } else {
                try { l.onTicksChanged() } catch (_: Throwable) {}
            }
        }
        // Update widgets when ticks change
        try {
            com.example.strive.widgets.StriveWidgetProvider.WidgetUpdateHelper.updateAllWidgets(context)
        } catch (_: Throwable) {}
    }

    // --- Loaders (lazy)
    private fun loadUserProfile(): UserProfile? {
        if (userProfile != null) return userProfile
        val json = prefs.getString(PrefsKeys.PREF_USER_PROFILE_JSON, null) ?: return null
        userProfile = SerializationUtils.fromJson(json)
        return userProfile
    }

    private fun loadHabits(): MutableList<Habit> {
        if (habits != null) return habits!!
        val json = prefs.getString(PrefsKeys.PREF_HABITS_JSON, null)
        habits = if (json == null) {
            // seed built-ins
            SeedData.builtInHabits().toMutableList()
        } else {
            SerializationUtils.fromJson(json)
        }
        return habits!!
    }

    private fun loadTicks(): MutableList<HabitTick> {
        if (ticks != null) return ticks!!
        val json = prefs.getString(PrefsKeys.PREF_TICKS_JSON, null)
        ticks = if (json == null) mutableListOf() else SerializationUtils.fromJson(json)
        return ticks!!
    }

    private fun loadMoods(): MutableList<MoodEntry> {
        if (moods != null) return moods!!
        val json = prefs.getString(PrefsKeys.PREF_MOODS_JSON, null)
        moods = if (json == null) mutableListOf() else SerializationUtils.fromJson(json)
        return moods!!
    }

    private fun loadSettings(): AppSettings {
        if (settings != null) return settings!!
        val json = prefs.getString(PrefsKeys.PREF_SETTINGS_JSON, null)
        settings = if (json == null) AppSettings() else SerializationUtils.fromJson(json)
        return settings!!
    }

    // --- Public API (synchronous simple wrappers) -- consider making suspend for heavy ops

    // UserProfile
    fun getUserProfile(): UserProfile? = loadUserProfile()

    fun saveUserProfile(profile: UserProfile) {
        userProfile = profile
        prefs.edit().putString(PrefsKeys.PREF_USER_PROFILE_JSON, SerializationUtils.toJson(profile)).apply()
    }

    // Habits
    fun getAllHabits(): List<Habit> = loadHabits().toList()

    fun getHabit(habitId: String): Habit? = loadHabits().firstOrNull { it.id == habitId }

    fun addHabit(habit: Habit) {
        val list = loadHabits()
        list.add(0, habit) // add to front
        persistHabits(list)
    }

    fun updateHabit(updated: Habit) {
        val list = loadHabits()
        val idx = list.indexOfFirst { it.id == updated.id }
        if (idx >= 0) {
            list[idx] = updated
            persistHabits(list)
        }
    }

    fun deleteHabit(habitId: String) {
        val list = loadHabits()
        list.removeAll { it.id == habitId }
        persistHabits(list)
        // remove ticks
        val t = loadTicks()
        t.removeAll { it.habitId == habitId }
        persistTicks(t)
    }

    private fun persistHabits(list: List<Habit>) {
        habits = list.toMutableList()
        prefs.edit().putString(PrefsKeys.PREF_HABITS_JSON, SerializationUtils.toJson(list)).apply()
    }

    // Ticks
    fun addTick(habitId: String, amount: Int) {
        val t = loadTicks()
        val today = DateUtils.nowDateString()
        // find existing tick
        val idx = t.indexOfFirst { it.habitId == habitId && it.date == today }
        if (idx >= 0) {
            val existing = t[idx]
            t[idx] = existing.copy(amount = existing.amount + amount)
        } else {
            t.add(HabitTick(habitId = habitId, date = today, amount = amount))
        }
        persistTicks(t)
        notifyTicksChanged()
    }

    fun setTick(habitId: String, date: String, amount: Int) {
        val t = loadTicks()
        val idx = t.indexOfFirst { it.habitId == habitId && it.date == date }
        if (idx >= 0) {
            t[idx] = t[idx].copy(amount = amount)
        } else {
            t.add(HabitTick(habitId = habitId, date = date, amount = amount))
        }
        persistTicks(t)
        notifyTicksChanged()
    }

    fun getTicksForHabit(habitId: String): List<HabitTick> = loadTicks().filter { it.habitId == habitId }

    fun getTicksForDate(date: String): List<HabitTick> = loadTicks().filter { it.date == date }

    private fun persistTicks(list: List<HabitTick>) {
        ticks = list.toMutableList()
        prefs.edit().putString(PrefsKeys.PREF_TICKS_JSON, SerializationUtils.toJson(list)).apply()
    }

    // Moods
    fun getAllMoods(): List<MoodEntry> = loadMoods().toList()

    fun addMood(entry: MoodEntry) {
        val m = loadMoods()
        m.add(0, entry) // newest first
        // prune older than 180 days
        pruneMoodsIfNeeded(m)
        persistMoods(m)
    }

    fun updateMood(updated: MoodEntry) {
        val m = loadMoods()
        val idx = m.indexOfFirst { it.id == updated.id }
        if (idx >= 0) {
            m[idx] = updated
            persistMoods(m)
        }
    }

    fun deleteMood(moodId: String) {
        val m = loadMoods()
        m.removeAll { it.id == moodId }
        persistMoods(m)
    }

    private fun pruneMoodsIfNeeded(list: MutableList<MoodEntry>) {
        // Implement pruning strategy; keep last 180 days approx.
        // Simple example: keep all entries with timestamp newer than System.currentTimeMillis() - 180*24*3600*1000
        val cutoff = System.currentTimeMillis() - 180L * 24 * 60 * 60 * 1000
        list.removeAll { it.timestamp < cutoff }
    }

    private fun persistMoods(list: List<MoodEntry>) {
        moods = list.toMutableList()
        prefs.edit().putString(PrefsKeys.PREF_MOODS_JSON, SerializationUtils.toJson(list)).apply()
    }

    // Settings
    fun getSettings(): AppSettings = loadSettings()

    fun saveSettings(s: AppSettings) {
        settings = s
        prefs.edit().putString(PrefsKeys.PREF_SETTINGS_JSON, SerializationUtils.toJson(s)).apply()
    }

    // Export / Import
    fun exportAllToJson(): String {
        val bundle = com.example.strive.models.ExportBundle(
            version = PrefsKeys.PREF_EXPORT_VERSION,
            exportedAt = System.currentTimeMillis(),
            userProfile = userProfile ?: loadUserProfile(),
            habits = loadHabits(),
            ticks = loadTicks(),
            moods = loadMoods(),
            settings = loadSettings()
        )
        return SerializationUtils.toJson(bundle)
    }

    fun importFromJson(jsonStr: String, merge: Boolean = false) {
        // Decode strongly-typed bundle
        val bundle = SerializationUtils.fromJson<com.example.strive.models.ExportBundle>(jsonStr)

        if (bundle.version != PrefsKeys.PREF_EXPORT_VERSION) {
            // In future handle migrations; for now proceed
        }

        if (!merge) {
            bundle.userProfile?.let { saveUserProfile(it) }
            persistHabits(bundle.habits)
            persistTicks(bundle.ticks)
            persistMoods(bundle.moods)
            saveSettings(bundle.settings)
        } else {
            // Merge strategy
            bundle.userProfile?.let {
                if (getUserProfile() == null) saveUserProfile(it)
            }

            if (bundle.habits.isNotEmpty()) {
                val existing = loadHabits()
                val existingIds = existing.map { it.id }.toSet()
                val toAdd = bundle.habits.filter { it.id !in existingIds }
                if (toAdd.isNotEmpty()) {
                    existing.addAll(toAdd)
                    persistHabits(existing)
                }
            }

            if (bundle.ticks.isNotEmpty()) {
                val existing = loadTicks()
                val existingKeys = existing.map { "${'$'}{it.habitId}|${'$'}{it.date}" }.toSet()
                val toAdd = bundle.ticks.filter { "${'$'}{it.habitId}|${'$'}{it.date}" !in existingKeys }
                if (toAdd.isNotEmpty()) {
                    existing.addAll(toAdd)
                    persistTicks(existing)
                }
            }

            if (bundle.moods.isNotEmpty()) {
                val existing = loadMoods()
                val existingIds = existing.map { it.id }.toSet()
                val toAdd = bundle.moods.filter { it.id !in existingIds }
                if (toAdd.isNotEmpty()) {
                    existing.addAll(0, toAdd)
                    persistMoods(existing)
                }
            }

            // Replace settings entirely for now
            saveSettings(bundle.settings)
        }
    }

    // Reset all data (delete all prefs keys & re-seed built-ins)
    fun resetAll() {
        prefs.edit().clear().apply()
        // clear cached references
        userProfile = null
        habits = null
        ticks = null
        moods = null
        settings = null
        // re-seed built-ins into prefs
        persistHabits(SeedData.builtInHabits())
    }
}

