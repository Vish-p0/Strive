package com.example.strive.views

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.example.strive.models.MoodEntry
import com.example.strive.util.EmojiPalette
import com.example.strive.util.DateUtils
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

class MoodChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var moodData: List<MoodEntry> = emptyList()
    private var timeframe: Timeframe = Timeframe.TODAY
    private var chartPoints: List<ChartPoint> = emptyList()

    private val padding = 60f
    private val pointRadius = 6f
    private val textSize = 32f
    private val gridTextSize = 28f

    enum class Timeframe {
        TODAY, DAILY, WEEKLY, MONTHLY, YEARLY
    }

    data class ChartPoint(
        val x: Float,
        val y: Float,
        val value: Int,
        val timestamp: Long,
        val emoji: String
    )

    init {
        setupPaints()
        // Ensure we recalculate once the view is laid out
        addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            if (width > 0 && height > 0 && moodData.isNotEmpty()) {
                calculateChartPoints()
                invalidate()
            }
        }
    }

    private fun setupPaints() {
        // Line paint for connecting mood points
        linePaint.apply {
            color = 0xFF4CAF50.toInt() // Green color
            style = Paint.Style.STROKE
            strokeWidth = 6f // Made thicker
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        // Point paint for mood dots
        pointPaint.apply {
            style = Paint.Style.FILL
        }

        // Text paint for labels
        textPaint.apply {
            color = 0xFF666666.toInt()
            textSize = this@MoodChartView.textSize
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD // Make emoji text bold
        }

        // Grid paint for axis lines
        gridPaint.apply {
            color = 0xFFE0E0E0.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        // Background gradient paint
        backgroundPaint.apply {
            style = Paint.Style.FILL
        }
    }

    fun setMoodData(data: List<MoodEntry>) {
        moodData = data
        if (width > 0 && height > 0) {
            calculateChartPoints()
            invalidate()
        } else {
            // Defer calculation until layout is ready
            requestLayout()
        }
    }

    fun setTimeframe(timeframe: Timeframe) {
        this.timeframe = timeframe
        if (width > 0 && height > 0) {
            calculateChartPoints()
            invalidate()
        } else {
            requestLayout()
        }
    }

    private fun calculateChartPoints() {
        if (moodData.isEmpty()) {
            chartPoints = emptyList()
            return
        }
        // If view size isn't ready yet, defer calculation
        if (width <= 0 || height <= 0) {
            post {
                if (width > 0 && height > 0) {
                    calculateChartPoints()
                    invalidate()
                }
            }
            return
        }

        val filteredData = when (timeframe) {
            Timeframe.TODAY -> getTodayMoods()
            Timeframe.DAILY -> getLastNDays(7)
            Timeframe.WEEKLY -> getLastNWeeks(8)
            Timeframe.MONTHLY -> getLastNMonths(12)
            Timeframe.YEARLY -> getLastNYears(5)
        }

        if (filteredData.isEmpty()) {
            chartPoints = emptyList()
            return
        }

        val points = mutableListOf<ChartPoint>()
        val chartWidth = width - 2 * padding
        val chartHeight = height - 2 * padding

        filteredData.forEachIndexed { index, mood ->
            val x = if (filteredData.size == 1) {
                width / 2f // Center single point
            } else {
                padding + (index * chartWidth / (filteredData.size - 1))
            }
            val normalizedValue = (mood.score - 1) / 4f // Normalize 1-5 to 0-1
            val y = padding + chartHeight - (normalizedValue * chartHeight)

            points.add(ChartPoint(
                x = x,
                y = y,
                value = mood.score,
                timestamp = mood.timestamp,
                emoji = mood.emoji
            ))
        }

        chartPoints = points
    }

    private fun getTodayMoods(): List<MoodEntry> {
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis

        return moodData
            .filter { it.timestamp >= startTime && it.timestamp <= endTime }
            .sortedBy { it.timestamp }
    }

    private fun getLastNDays(days: Int): List<MoodEntry> {
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, -days)
        val startTime = calendar.timeInMillis

        return moodData
            .filter { it.timestamp >= startTime && it.timestamp <= endTime }
            .sortedBy { it.timestamp }
    }

    private fun getLastNWeeks(weeks: Int): List<MoodEntry> {
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.add(Calendar.WEEK_OF_YEAR, -weeks)
        val startTime = calendar.timeInMillis

        // Group by week and average the scores
        return moodData
            .filter { it.timestamp >= startTime && it.timestamp <= endTime }
            .groupBy { getWeekOfYear(it.timestamp) }
            .map { (_, moods) ->
                val avgScore = moods.map { it.score }.average().toInt().coerceIn(1, 5)
                val representativeEmoji = EmojiPalette.EMOJIS.find { it.score == avgScore }?.emoji ?: "😐"
                moods.first().copy(
                    score = avgScore,
                    emoji = representativeEmoji,
                    timestamp = moods.map { it.timestamp }.average().toLong()
                )
            }
            .sortedBy { it.timestamp }
    }

    private fun getLastNMonths(months: Int): List<MoodEntry> {
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.add(Calendar.MONTH, -months)
        val startTime = calendar.timeInMillis

        // Group by month and average the scores
        return moodData
            .filter { it.timestamp >= startTime && it.timestamp <= endTime }
            .groupBy { getMonthOfYear(it.timestamp) }
            .map { (_, moods) ->
                val avgScore = moods.map { it.score }.average().toInt().coerceIn(1, 5)
                val representativeEmoji = EmojiPalette.EMOJIS.find { it.score == avgScore }?.emoji ?: "😐"
                moods.first().copy(
                    score = avgScore,
                    emoji = representativeEmoji,
                    timestamp = moods.map { it.timestamp }.average().toLong()
                )
            }
            .sortedBy { it.timestamp }
    }

    private fun getLastNYears(years: Int): List<MoodEntry> {
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.add(Calendar.YEAR, -years)
        val startTime = calendar.timeInMillis

        // Group by year and average the scores
        return moodData
            .filter { it.timestamp >= startTime && it.timestamp <= endTime }
            .groupBy { getYear(it.timestamp) }
            .map { (_, moods) ->
                val avgScore = moods.map { it.score }.average().toInt().coerceIn(1, 5)
                val representativeEmoji = EmojiPalette.EMOJIS.find { it.score == avgScore }?.emoji ?: "😐"
                moods.first().copy(
                    score = avgScore,
                    emoji = representativeEmoji,
                    timestamp = moods.map { it.timestamp }.average().toLong()
                )
            }
            .sortedBy { it.timestamp }
    }

    private fun getWeekOfYear(timestamp: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        return "${calendar.get(Calendar.YEAR)}-W${calendar.get(Calendar.WEEK_OF_YEAR)}"
    }

    private fun getMonthOfYear(timestamp: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        return "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.MONTH)}"
    }

    private fun getYear(timestamp: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        return calendar.get(Calendar.YEAR).toString()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (width == 0 || height == 0) return

        drawBackground(canvas)
        drawGrid(canvas)

        if (chartPoints.isEmpty()) {
            drawEmptyState(canvas)
        } else {
            drawMoodLine(canvas)
            drawMoodPoints(canvas)
            drawLabels(canvas)
        }
    }

    private fun drawEmptyState(canvas: Canvas) {
        textPaint.textSize = textSize
        textPaint.color = 0xFF999999.toInt()
        textPaint.textAlign = Paint.Align.CENTER

        val message = when (timeframe) {
            Timeframe.TODAY -> "No moods logged today"
            Timeframe.DAILY -> "No moods in the past week"
            Timeframe.WEEKLY -> "No moods in the past 8 weeks"
            Timeframe.MONTHLY -> "No moods in the past year"
            Timeframe.YEARLY -> "No moods in the past 5 years"
        }

        canvas.drawText(
            message,
            width / 2f,
            height / 2f,
            textPaint
        )
    }

    private fun drawBackground(canvas: Canvas) {
        // Draw white background instead of gradient
        val chartHeight = height - 2 * padding
        
        backgroundPaint.shader = null
        backgroundPaint.color = Color.TRANSPARENT
        
        canvas.drawRect(
            padding, padding,
            width - padding, height - padding,
            backgroundPaint
        )
    }

    private fun drawGrid(canvas: Canvas) {
        val chartHeight = height - 2 * padding
        val chartWidth = width - 2 * padding

        // Enhanced grid paint for better visibility
        gridPaint.color = 0xFFE0E0E0.toInt()
        gridPaint.strokeWidth = 1.5f

        // Draw horizontal grid lines for mood levels (5 levels)
        for (i in 1..5) {
            val y = padding + chartHeight - ((i - 1) * chartHeight / 4)
            canvas.drawLine(padding, y, width - padding, y, gridPaint)

            // Draw mood level labels
            val label = when (i) {
                1 -> "😢"
                2 -> "😕"
                3 -> "😐"
                4 -> "😊"
                5 -> "😁"
                else -> ""
            }

            textPaint.textSize = gridTextSize
            canvas.drawText(label, padding - 30, y + gridTextSize / 3, textPaint)
        }

        // Draw vertical grid lines
        val numberOfVerticalLines = 6 // Including start and end
        for (i in 0 until numberOfVerticalLines) {
            val x = padding + (i * chartWidth / (numberOfVerticalLines - 1))
            canvas.drawLine(x, padding, x, height - padding, gridPaint)
        }
    }

    private fun drawMoodLine(canvas: Canvas) {
        if (chartPoints.size < 2) return

        // Create smooth curved path for better visual appeal
        val path = Path()
        chartPoints.forEachIndexed { index, point ->
            if (index == 0) {
                path.moveTo(point.x, point.y)
            } else {
                val prevPoint = chartPoints[index - 1]
                val controlPointX = (prevPoint.x + point.x) / 2f

                // Use quadratic curve for smoother lines
                path.quadTo(controlPointX, prevPoint.y, point.x, point.y)
            }
        }

        // Draw the line with gradient effect
        val gradient = LinearGradient(
            0f, padding,
            0f, height - padding,
            intArrayOf(0xFF4CAF50.toInt(), 0xFF81C784.toInt()),
            null,
            Shader.TileMode.CLAMP
        )
        linePaint.shader = gradient

        canvas.drawPath(path, linePaint)

        // Reset shader for other uses
        linePaint.shader = null
    }

    private fun drawMoodPoints(canvas: Canvas) {
        chartPoints.forEach { point ->
            // For TODAY timeframe, show larger emojis instead of dots
            if (timeframe == Timeframe.TODAY) {
                // Draw large emoji
                textPaint.textSize = textSize * 1.5f // Make emojis bigger for today
                textPaint.color = 0xFF333333.toInt()
                canvas.drawText(point.emoji, point.x, point.y + textSize / 2f, textPaint)

                // Optional: Draw subtle background circle for emoji
                pointPaint.color = 0x30FFFFFF.toInt()
                canvas.drawCircle(point.x, point.y, pointRadius * 2f, pointPaint)
            } else {
                // For other timeframes, show colored dots with small emojis above
                // Set color based on mood value
                pointPaint.color = when (point.value) {
                    1 -> 0xFFFF5722.toInt() // Red for very sad
                    2 -> 0xFFFF9800.toInt() // Orange for sad
                    3 -> 0xFFFFC107.toInt() // Yellow for neutral
                    4 -> 0xFF8BC34A.toInt() // Light green for happy
                    5 -> 0xFF4CAF50.toInt() // Green for very happy
                    else -> 0xFF9E9E9E.toInt() // Gray for unknown
                }

                // Draw larger point for better visibility
                canvas.drawCircle(point.x, point.y, pointRadius * 1.5f, pointPaint)

                // Draw white border around point
                pointPaint.color = 0xFFFFFFFF.toInt()
                pointPaint.style = Paint.Style.STROKE
                pointPaint.strokeWidth = 3f
                canvas.drawCircle(point.x, point.y, pointRadius * 1.5f, pointPaint)
                pointPaint.style = Paint.Style.FILL
                pointPaint.strokeWidth = 0f

                // Draw emoji above the point (smaller for non-today views)
                textPaint.textSize = textSize * 0.8f
                textPaint.color = 0xFF333333.toInt()
                canvas.drawText(point.emoji, point.x, point.y - pointRadius * 2f - 5f, textPaint)
            }
        }
    }

    private fun drawLabels(canvas: Canvas) {
        if (chartPoints.isEmpty()) return

        textPaint.textSize = gridTextSize
        textPaint.color = 0xFF666666.toInt()

        val dateFormat = when (timeframe) {
            Timeframe.TODAY -> SimpleDateFormat("HH:mm", Locale.getDefault())
            Timeframe.DAILY -> SimpleDateFormat("EEE", Locale.getDefault())
            Timeframe.WEEKLY -> SimpleDateFormat("MMM dd", Locale.getDefault())
            Timeframe.MONTHLY -> SimpleDateFormat("MMM", Locale.getDefault())
            Timeframe.YEARLY -> SimpleDateFormat("yyyy", Locale.getDefault())
        }

        chartPoints.forEachIndexed { index, point ->
            if (index % max(1, chartPoints.size / 5) == 0) { // Show max 5 labels
                val label = dateFormat.format(Date(point.timestamp))
                canvas.drawText(
                    label,
                    point.x,
                    height - padding / 2,
                    textPaint
                )
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = 400
        val desiredHeight = 280 // Increased height for better chart visibility

        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val width = when (widthMode) {
            MeasureSpec.EXACTLY -> widthSize
            MeasureSpec.AT_MOST -> minOf(desiredWidth, widthSize)
            else -> desiredWidth
        }

        val height = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> minOf(desiredHeight, heightSize)
            else -> desiredHeight
        }

        setMeasuredDimension(width, height)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0 && moodData.isNotEmpty()) {
            calculateChartPoints()
            invalidate()
        }
    }
}

