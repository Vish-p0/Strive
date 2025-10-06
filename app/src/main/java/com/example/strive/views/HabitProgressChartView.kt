package com.example.strive.views

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.example.strive.R
import kotlin.math.max

class HabitProgressChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val targetLinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gradientPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var chartData: List<ChartDataPoint> = emptyList()
    private var maxValue: Float = 0f
    private var timeframe: String = "Daily"

    private val barWidth = 32f
    private val barSpacing = 12f
    private val padding = 56f
    private val textSize = 26f
    private val gridTextSize = 22f
    private val cornerRadius = 12f

    data class ChartDataPoint(
        val label: String,
        val value: Float,
        val target: Float,
        val date: String
    )

    init {
        setupPaints()
    }

    private fun setupPaints() {
        try {
            val primaryColor = context.getColor(R.color.colorPrimary)
            val secondaryColor = context.getColor(R.color.progressBackground)
            val textColor = context.getColor(R.color.textPrimary)
            val gridColor = context.getColor(R.color.textSecondary)

            barPaint.color = primaryColor
            barPaint.style = Paint.Style.FILL

            paint.color = secondaryColor
            paint.style = Paint.Style.FILL

            targetLinePaint.color = Color.parseColor("#FF9800")
            targetLinePaint.style = Paint.Style.STROKE
            targetLinePaint.strokeWidth = 3f
            targetLinePaint.pathEffect = DashPathEffect(floatArrayOf(10f, 5f), 0f)

            gradientPaint.style = Paint.Style.FILL

            textPaint.color = textColor
            textPaint.textSize = textSize
            textPaint.textAlign = Paint.Align.CENTER
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

            gridPaint.color = gridColor
            gridPaint.textSize = gridTextSize
            gridPaint.textAlign = Paint.Align.RIGHT
            gridPaint.strokeWidth = 1f
        } catch (e: Exception) {
            // Fallback to default colors if resource access fails
            e.printStackTrace()
            barPaint.color = Color.BLUE
            barPaint.style = Paint.Style.FILL

            paint.color = Color.GRAY
            paint.style = Paint.Style.FILL

            targetLinePaint.color = Color.parseColor("#FF9800")
            targetLinePaint.style = Paint.Style.STROKE
            targetLinePaint.strokeWidth = 3f

            textPaint.color = Color.BLACK
            textPaint.textSize = textSize
            textPaint.textAlign = Paint.Align.CENTER

            gridPaint.color = Color.GRAY
            gridPaint.textSize = gridTextSize
            gridPaint.textAlign = Paint.Align.RIGHT
            gridPaint.strokeWidth = 1f
        }
    }

    fun setChartData(data: List<ChartDataPoint>, timeframe: String) {
        try {
            this.chartData = data
            this.timeframe = timeframe
            this.maxValue = data.maxOfOrNull { max(it.value, it.target) } ?: 0f
            invalidate()
        } catch (e: Exception) {
            e.printStackTrace()
            this.chartData = emptyList()
            this.timeframe = timeframe
            this.maxValue = 0f
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        try {
            if (chartData.isEmpty()) {
                drawEmptyState(canvas)
                return
            }

            val chartWidth = width - (padding * 2)
            val chartHeight = height - (padding * 2)

            // Draw grid lines
            drawGrid(canvas, chartHeight)

            // Draw bars
            drawBars(canvas, chartWidth, chartHeight)

            // Draw labels
            drawLabels(canvas, chartWidth, chartHeight)
        } catch (e: Exception) {
            e.printStackTrace()
            // Draw a simple error message
            textPaint.textAlign = Paint.Align.CENTER
            textPaint.color = Color.RED
            canvas.drawText("Chart error", width / 2f, height / 2f, textPaint)
        }
    }

    private fun drawEmptyState(canvas: Canvas) {
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.color = context.getColor(R.color.textSecondary)

        val text = "No data available for $timeframe view"
        val x = width / 2f
        val y = height / 2f

        canvas.drawText(text, x, y, textPaint)
    }

    private fun drawGrid(canvas: Canvas, chartHeight: Float) {
        if (maxValue <= 0) return

        val gridLines = 5
        val stepValue = maxValue / gridLines

        for (i in 0..gridLines) {
            val value = i * stepValue
            val y = padding + chartHeight - (i * chartHeight / gridLines)

            // Softer grid line
            gridPaint.style = Paint.Style.STROKE
            gridPaint.alpha = 30
            gridPaint.strokeWidth = 1f
            canvas.drawLine(padding, y, width - padding, y, gridPaint)

            // Grid label with better styling
            gridPaint.style = Paint.Style.FILL
            gridPaint.alpha = 180
            val label = if (value == value.toInt().toFloat()) {
                value.toInt().toString()
            } else {
                String.format("%.1f", value)
            }
            canvas.drawText(label, padding - 16f, y + 6f, gridPaint)
        }
    }

    private fun drawBars(canvas: Canvas, chartWidth: Float, chartHeight: Float) {
        if (chartData.isEmpty() || maxValue <= 0) return

        val totalBarsWidth = chartData.size * (barWidth + barSpacing) - barSpacing
        val startX = padding + (chartWidth - totalBarsWidth) / 2f

        chartData.forEachIndexed { index, dataPoint ->
            val x = startX + index * (barWidth + barSpacing)

            // Background bar (target) with subtle styling
            val targetHeight = (dataPoint.target / maxValue) * chartHeight
            val targetTop = padding + chartHeight - targetHeight
            val targetBottom = padding + chartHeight

            paint.color = context.getColor(R.color.progressBackground)
            paint.alpha = 100
            canvas.drawRoundRect(
                x, targetTop, x + barWidth, targetBottom,
                cornerRadius, cornerRadius, paint
            )

            // Progress bar with gradient effect
            val valueHeight = (dataPoint.value / maxValue) * chartHeight
            val valueTop = padding + chartHeight - valueHeight

            val isComplete = dataPoint.value >= dataPoint.target
            val startColor = if (isComplete) {
                Color.parseColor("#4CAF50") // Success green
            } else {
                Color.parseColor("#2196F3") // Primary blue
            }
            val endColor = if (isComplete) {
                Color.parseColor("#81C784") // Light green
            } else {
                Color.parseColor("#64B5F6") // Light blue
            }

            // Create gradient from bottom to top
            val gradient = LinearGradient(
                x, targetBottom, x, valueTop,
                endColor, startColor,
                Shader.TileMode.CLAMP
            )
            gradientPaint.shader = gradient

            // Draw rounded bar with gradient
            val barRect = RectF(x, valueTop, x + barWidth, targetBottom)
            canvas.drawRoundRect(barRect, cornerRadius, cornerRadius, gradientPaint)

            // Draw target line if there's a target
            if (dataPoint.target > 0) {
                val targetLineY = padding + chartHeight - targetHeight
                canvas.drawLine(
                    x - 4f, targetLineY,
                    x + barWidth + 4f, targetLineY,
                    targetLinePaint
                )
            }
        }
    }

    private fun drawLabels(canvas: Canvas, chartWidth: Float, chartHeight: Float) {
        if (chartData.isEmpty()) return

        val totalBarsWidth = chartData.size * (barWidth + barSpacing) - barSpacing
        val startX = padding + (chartWidth - totalBarsWidth) / 2f

        textPaint.color = context.getColor(R.color.textSecondary)
        textPaint.textSize = gridTextSize
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

        chartData.forEachIndexed { index, dataPoint ->
            val x = startX + index * (barWidth + barSpacing) + barWidth / 2f
            val y = padding + chartHeight + 36f

            canvas.drawText(dataPoint.label, x, y, textPaint)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredHeight = 300.dpToPx()
        val height = resolveSize(desiredHeight, heightMeasureSpec)
        val width = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(width, height)
    }

    private fun Int.dpToPx(): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }
}

