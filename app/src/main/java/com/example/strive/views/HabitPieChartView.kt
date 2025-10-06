package com.example.strive.views

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.min
import kotlin.math.max

class HabitPieChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val legendPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    
    private var pieData: List<HabitSlice> = emptyList()
    
    private val padding = 32f
    private val legendItemHeight = 40f
    private val legendDotSize = 24f
    private val legendTextSize = 28f
    private val centerTextSize = 36f
    
    data class HabitSlice(
        val habitName: String,
        val progress: Float,  // Current progress value
        val target: Float,    // Target value
        val color: String     // Hex color code
    )
    
    init {
        setupPaints()
    }
    
    private fun setupPaints() {
        paint.style = Paint.Style.FILL
        
        textPaint.color = Color.BLACK
        textPaint.textSize = centerTextSize
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        
        legendPaint.textSize = legendTextSize
        legendPaint.color = Color.BLACK
    }
    
    fun setData(data: List<HabitSlice>) {
        pieData = data
        invalidate()
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (pieData.isEmpty()) {
            drawEmptyState(canvas)
            return
        }

        val completionRatios = pieData.map { slice ->
            if (slice.target > 0f) {
                (slice.progress / slice.target).coerceIn(0f, 1f)
            } else {
                0f
            }
        }
        val remainingRatios = completionRatios.map { ratio ->
            (1f - ratio).coerceAtLeast(0f)
        }
        val totalCompletion = completionRatios.sum()
        val totalRemaining = remainingRatios.sum()
        val totalSegments = totalCompletion + totalRemaining
        if (totalSegments <= 0f) {
            drawEmptyState(canvas)
            return
        }
        
        val width = width.toFloat()
        val height = height.toFloat()
        
        // Calculate available space for pie chart (left side)
        val pieWidth = width * 0.5f
        val pieHeight = height
        
        // Calculate pie chart dimensions
        val diameter = min(pieWidth - padding * 2, pieHeight - padding * 2)
        val radius = diameter / 2
        val centerX = pieWidth / 2
        val centerY = height / 2
        
        val rectF = RectF(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius
        )
        
        // Calculate total progress percentage
        var totalProgress = 0f
        var totalTarget = 0f
        pieData.forEach { slice ->
            totalProgress += slice.progress
            totalTarget += slice.target
        }
        
        val overallPercentage = if (totalTarget > 0) {
            min(100f, (totalProgress / totalTarget * 100f))
        } else {
            0f
        }
        
        // Draw pie slices for completed progress
        var startAngle = -90f // Start from top
        pieData.forEachIndexed { index, slice ->
            val ratio = completionRatios.getOrNull(index) ?: 0f
            if (ratio <= 0f) {
                return@forEachIndexed
            }

            val sweepAngle = (ratio / totalSegments) * 360f

            if (sweepAngle > 0) {
                try {
                    paint.color = Color.parseColor(slice.color)
                } catch (e: Exception) {
                    // Fallback to blue if color parsing fails
                    paint.color = Color.parseColor("#2196F3")
                }
                canvas.drawArc(rectF, startAngle, sweepAngle, true, paint)
                startAngle += sweepAngle
            }
        }

        // Draw remaining portion in grey
        if (totalRemaining > 0f) {
            paint.color = Color.parseColor("#E0E0E0")
            val remainingAngle = (totalRemaining / totalSegments) * 360f
            if (remainingAngle > 0f) {
                canvas.drawArc(rectF, startAngle, remainingAngle, true, paint)
            }
        }
        
        // Draw white circle in center for donut effect
        paint.color = Color.WHITE
        val innerRadius = radius * 0.6f
        canvas.drawCircle(centerX, centerY, innerRadius, paint)
        
        // Draw overall percentage in center
        textPaint.color = Color.BLACK
        textPaint.textSize = centerTextSize * 1.2f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val percentText = "${overallPercentage.toInt()}%"
        val textBounds = Rect()
        textPaint.getTextBounds(percentText, 0, percentText.length, textBounds)
        canvas.drawText(
            percentText,
            centerX,
            centerY + textBounds.height() / 2,
            textPaint
        )
        
        // Draw "Today" label below percentage
        textPaint.textSize = legendTextSize * 0.7f
        textPaint.color = Color.GRAY
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(
            "Today",
            centerX,
            centerY + textBounds.height() / 2 + legendTextSize,
            textPaint
        )
        
        // Reset text paint color
        textPaint.color = Color.BLACK
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        
        // Draw legend on the right side
        drawLegend(canvas, pieWidth, height, totalTarget)
    }
    
    private fun drawLegend(canvas: Canvas, startX: Float, height: Float, totalTarget: Float) {
        val legendX = startX + padding
        var currentY = padding + legendItemHeight
        
        // Draw "Habits" title
        legendPaint.textSize = legendTextSize * 1.1f
        legendPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Habits", legendX, currentY, legendPaint)
        currentY += legendItemHeight * 1.5f
        
        legendPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        legendPaint.textSize = legendTextSize
        
        pieData.forEach { slice ->
            // Draw colored dot
            try {
                paint.color = Color.parseColor(slice.color)
            } catch (e: Exception) {
                // Fallback to blue if color parsing fails
                paint.color = Color.parseColor("#2196F3")
            }
            canvas.drawCircle(legendX + legendDotSize / 2, currentY - legendDotSize / 2, legendDotSize / 2, paint)
            
            // Draw habit name
            legendPaint.color = Color.BLACK
            canvas.drawText(slice.habitName, legendX + legendDotSize + 16f, currentY, legendPaint)
            
            // Draw progress fraction
            val progressText = "${slice.progress.toInt()}/${slice.target.toInt()}"
            legendPaint.textAlign = Paint.Align.RIGHT
            legendPaint.color = Color.GRAY
            canvas.drawText(progressText, width - padding, currentY, legendPaint)
            legendPaint.textAlign = Paint.Align.LEFT
            
            currentY += legendItemHeight
        }
    }
    
    private fun drawEmptyState(canvas: Canvas) {
        textPaint.color = Color.GRAY
        textPaint.textSize = legendTextSize
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(
            "No habit progress today",
            width / 2f,
            height / 2f,
            textPaint
        )
    }
    
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredHeight = (padding * 2 + legendItemHeight * (pieData.size + 2)).toInt()
        val minHeight = 400 // Minimum height in pixels
        
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        
        val height = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> min(desiredHeight, heightSize)
            else -> max(desiredHeight, minHeight)
        }
        
        val width = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(width, height)
    }
}
