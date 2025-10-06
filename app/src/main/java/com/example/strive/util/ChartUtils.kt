package com.example.strive.util

import com.example.strive.models.HabitTick
import com.example.strive.models.MoodEntry
import com.example.strive.util.DateUtils
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Suppress("unused")
object ChartUtils {
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    @Suppress("unused")
    fun aggregateTicksByDay(ticks: List<HabitTick>, daysBack: Int, zone: ZoneId = ZoneId.systemDefault()): Map<String, Int> {
        val result = linkedMapOf<String, Int>()
        for (i in daysBack downTo 0) {
            val d = LocalDate.now(zone).minusDays(i.toLong()).format(dateFormatter)
            result[d] = 0
        }
        ticks.forEach { t ->
            result[t.date] = (result[t.date] ?: 0) + t.amount
        }
        return result
    }

    fun emojiValence(emoji: String): Int {
        return when (emoji) {
            "\uD83D\uDE00" -> 2 // 😀
            "\uD83D\uDE42" -> 1 // 🙂
            "\uD83D\uDE10" -> 0 // 😐
            "\uD83D\uDE41" -> -1 // 🙁
            "\uD83D\uDE22" -> -2 // 😢/😭
            "\uD83D\uDE2C" -> -1 // 😬 approx
            "\uD83E\uDD28" -> 1 // 🤨 approx
            "\uD83D\uDE0D" -> 2 // 😍
            "\uD83E\uDD75" -> -1 // 🤒 approx
            else -> 0
        }
    }

    @Suppress("unused")
    fun aggregateMoodByDay(entries: List<MoodEntry>, daysBack: Int, zone: ZoneId = ZoneId.systemDefault()): Map<String, Double> {
        val buckets = linkedMapOf<String, MutableList<Int>>()
        for (i in daysBack downTo 0) {
            val d = LocalDate.now(zone).minusDays(i.toLong()).format(dateFormatter)
            buckets[d] = mutableListOf()
        }
        entries.forEach { e ->
            val date = DateUtils.millisToDateString(e.timestamp, zone)
            val valence = emojiValence(e.emoji)
            if (buckets.containsKey(date)) buckets[date]?.add(valence)
        }
        val result = linkedMapOf<String, Double>()
        buckets.forEach { (k, v) ->
            val avg = if (v.isEmpty()) 0.0 else v.average()
            result[k] = (avg * 100).roundToInt() / 100.0
        }
        return result
    }
}

