package com.xiaopeng.xposed.instrument.compass.ribbon.widget

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator

class CompassRibbonView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : View(context, attrs, defStyleAttr) {

    // 角度相关
    private var displayAngle: Float = 0f
    private var previousAngle: Float = 0f

    // 字体和颜色配置
    private var textSizeInPx: Float = 0f  // 字体大小（px单位）
    private var textColor: Int = 0xFFFFFFFF.toInt()  // 默认白色

    // 动画相关
    private var textAlpha: Float = 1f
    private var fadeAnimator: ValueAnimator? = null
    private val fadeAnimationDuration = 300L  // 300ms

    // 绘制画笔
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    init {
        // 初始化默认字体大小为12sp（转换为px）
        val defaultSizeInSp = 12f
        textSizeInPx = spToPx(defaultSizeInSp)
        updateTextPaint()
        updateDotPaint()
    }

    private val centerLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFF0000.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    // 配置参数
    private val directionMap = mapOf(
        0f to "北",
        45f to "东北",
        90f to "东",
        135f to "东南",
        180f to "南",
        225f to "西南",
        270f to "西",
        315f to "西北"
    )
    private val dotRadius = 1f
    private val dotAngleStep = 9f
    private val visibleRange = 200f

    // ==================== 公共接口 ====================

    /**
     * 设置当前朝向角度
     * @param angle 角度值，0度 = 北，顺时针递增（范围：0-360）
     */
    fun setValue(angle: Float) {
        val normalizedAngle = convertAngleTo0To360(angle)
        val angleDiff = calculateShortestAngleDiff(displayAngle, normalizedAngle)
        val newDisplayAngle = convertAngleTo0To360(displayAngle + angleDiff)
        
        // 检查是否需要触发动画（方向文字是否改变）
        if (shouldTriggerFadeAnimation(displayAngle, newDisplayAngle)) {
            triggerFadeAnimation()
        }
        
        previousAngle = displayAngle
        displayAngle = newDisplayAngle
        invalidate()
    }

    /**
     * 设置字体大小（单位：px）
     * @param sizeInPx 字体大小，单位为px，默认值为12sp转换后的px
     */
    fun setTextSize(sizeInPx: Float) {
        if (textSizeInPx == sizeInPx) {
            return
        }

        textSizeInPx = sizeInPx
        updateTextPaint()
        invalidate()
    }

    /**
     * 设置字体颜色
     * @param color 颜色值，默认为白色。点的颜色会跟随字体颜色
     */
    fun setTextColor(color: Int) {
        if (textColor == color) {
            return
        }

        textColor = color
        updateTextPaint()
        updateDotPaint()
        invalidate()
    }

    // ==================== 配置更新相关 ====================

    /**
     * 更新文字画笔
     */
    private fun updateTextPaint() {
        textPaint.textSize = textSizeInPx
        val alpha = (textAlpha * 255).toInt().coerceIn(0, 255)
        val baseColor = textColor and 0x00FFFFFF  // 移除原alpha值
        textPaint.color = (alpha shl 24) or baseColor
    }

    /**
     * 更新点画笔
     */
    private fun updateDotPaint() {
        dotPaint.color = textColor
    }

    /**
     * 将sp转换为px
     */
    private fun spToPx(sp: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, resources.displayMetrics)
    }

    // ==================== 角度管理相关 ====================

    /**
     * 将角度转换为0-360范围
     */
    private fun convertAngleTo0To360(angle: Float): Float {
        return (angle % 360f + 360f) % 360f
    }

    /**
     * 计算两个角度之间的最短差值（处理跨越0度的情况）
     * 例如：330° -> 10° 应该返回 +40° 而不是 -320°
     */
    private fun calculateShortestAngleDiff(from: Float, to: Float): Float {
        var diff = to - from
        if (diff > 180f) {
            diff -= 360f
        } else if (diff < -180f) {
            diff += 360f
        }
        return diff
    }

    // ==================== 绘制相关 ====================

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = getViewCenterX()
        val centerY = getViewCenterY()
        val viewTop = 0f
        val viewBottom = height.toFloat()

        drawDirectionTexts(canvas, centerX, viewTop)
        drawDots(canvas, centerX, viewBottom)
        drawMiddleRedLine(canvas, centerX, centerY)
    }

    /**
     * 绘制方向文字
     */
    private fun drawDirectionTexts(canvas: Canvas, centerX: Float, viewTop: Float) {
        val textBaseY = calculateTextBaseY(viewTop)
        val drawnPositions = mutableSetOf<Pair<Int, String>>()
        val (startAngle, endAngle) = getVisibleAngleRange()

        var angleIndex = startAngle.toInt()
        while (angleIndex <= endAngle.toInt()) {
            val normalizedAngle = convertAngleTo0To360(angleIndex.toFloat())
            drawDirectionTextIfNeeded(canvas, normalizedAngle, centerX, textBaseY, drawnPositions)
            angleIndex++
        }
    }

    /**
     * 绘制单个方向文字（如果需要）
     */
    private fun drawDirectionTextIfNeeded(canvas: Canvas, angle: Float, centerX: Float, textBaseY: Float, drawnPositions: MutableSet<Pair<Int, String>>) {
        val directionText = getDirectionText(angle)
                            ?: return

        val textX = calculateTextX(angle, centerX).toInt()
        val positionKey = Pair(textX, directionText)

        if (drawnPositions.contains(positionKey)) {
            return
        }

        if (!isTextXInView(textX)) {
            return
        }

        canvas.drawText(directionText, textX.toFloat(), textBaseY, textPaint)
        drawnPositions.add(positionKey)
    }

    /**
     * 绘制点
     */
    private fun drawDots(canvas: Canvas, centerX: Float, viewBottom: Float) {
        val (startAngle, endAngle) = getVisibleAngleRange()

        var angleIndex = (startAngle / dotAngleStep).toInt() * dotAngleStep.toInt()
        val endAngleIndex = (endAngle / dotAngleStep).toInt() * dotAngleStep.toInt()

        while (angleIndex <= endAngleIndex) {
            val normalizedAngle = convertAngleTo0To360(angleIndex.toFloat())
            val dotX = calculateTextX(normalizedAngle, centerX)
            drawSingleDot(canvas, dotX, viewBottom)
            angleIndex += dotAngleStep.toInt()
        }
    }

    /**
     * 绘制单个点
     */
    private fun drawSingleDot(canvas: Canvas, x: Float, bottomY: Float) {
        val dotCenterY = bottomY - dotRadius
        canvas.drawCircle(x, dotCenterY, dotRadius, dotPaint)
    }

    /**
     * 绘制中间位置的红线（60%高度）
     */
    private fun drawMiddleRedLine(canvas: Canvas, centerX: Float, centerY: Float) {
        val lineHeight = height * 0.6f
        val lineTop = centerY - lineHeight / 2f
        val lineBottom = centerY + lineHeight / 2f
        canvas.drawLine(centerX, lineTop, centerX, lineBottom, centerLinePaint)
    }

    // ==================== 测量相关 ====================

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val measuredWidth = View.getDefaultSize(suggestedMinimumWidth, widthMeasureSpec)
        val measuredHeight = calculateViewHeight(heightMeasureSpec)
        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    /**
     * 计算视图高度
     */
    private fun calculateViewHeight(heightMeasureSpec: Int): Int {
        val fontMetrics = textPaint.fontMetrics
        val textHeight = fontMetrics.bottom - fontMetrics.top
        val dotDiameter = dotRadius * 2f
        val totalHeight = (textHeight + dotDiameter).toInt()
        return View.resolveSize(totalHeight, heightMeasureSpec)
    }

    // ==================== 位置计算相关 ====================

    /**
     * 获取视图中心X坐标
     */
    private fun getViewCenterX(): Float {
        return width / 2f
    }

    /**
     * 获取视图中心Y坐标
     */
    private fun getViewCenterY(): Float {
        return height / 2f
    }

    /**
     * 获取可见角度范围
     */
    private fun getVisibleAngleRange(): Pair<Float, Float> {
        val halfRange = visibleRange / 2f
        val startAngle = displayAngle - halfRange
        val endAngle = displayAngle + halfRange
        return Pair(startAngle, endAngle)
    }

    /**
     * 计算文字的X坐标位置
     */
    private fun calculateTextX(angle: Float, centerX: Float): Float {
        val offsetX = calculateTextOffsetX(angle)
        return centerX + offsetX
    }

    /**
     * 计算文字相对于中心角度的X偏移量
     */
    private fun calculateTextOffsetX(angle: Float): Float {
        val angleDiff = calculateShortestAngleDiff(displayAngle, angle)
        val spacingPerDegree = getSpacingPerDegree()
        return angleDiff * spacingPerDegree
    }

    /**
     * 获取每度对应的像素数
     */
    private fun getSpacingPerDegree(): Float {
        if (width <= 0) {
            return 20f
        }
        return width / visibleRange
    }

    /**
     * 计算文字基线的Y坐标（文字顶部对齐view顶部）
     */
    private fun calculateTextBaseY(viewTop: Float): Float {
        val fontMetrics = textPaint.fontMetrics
        return viewTop - fontMetrics.top
    }

    // ==================== 辅助功能 ====================

    /**
     * 判断文字X坐标是否在可见区域内
     */
    private fun isTextXInView(textX: Int): Boolean {
        return textX in 0..width
    }

    // ==================== 动画相关 ====================

    /**
     * 检查是否需要触发淡入淡出动画
     * 当中心位置的文字内容改变时触发动画
     */
    private fun shouldTriggerFadeAnimation(oldAngle: Float, newAngle: Float): Boolean {
        val oldText = getDirectionText(oldAngle)
        val newText = getDirectionText(newAngle)
        return oldText != newText
    }

    /**
     * 触发淡入淡出动画
     */
    private fun triggerFadeAnimation() {
        // 取消之前的动画
        fadeAnimator?.cancel()

        // 创建淡出-淡入动画
        fadeAnimator = ValueAnimator.ofFloat(1f, 0f, 1f).apply {
            duration = fadeAnimationDuration
            interpolator = AccelerateDecelerateInterpolator()
            
            addUpdateListener { animator ->
                textAlpha = animator.animatedValue as Float
                updateTextPaint()
                invalidate()
            }
            
            start()
        }
    }

    /**
     * 获取角度对应的方向文字
     * 当角度在标准方位±10度内时显示中文方位，否则按间隔显示角度数字
     */
    private fun getDirectionText(angle: Float): String? {
        val normalizedAngle = convertAngleTo0To360(angle)
        val roundedAngle = kotlin.math.round(normalizedAngle).toInt()

        // 检查是否在标准方位±10度以内
        for ((keyAngle, directionText) in directionMap) {
            val keyNormalized = convertAngleTo0To360(keyAngle)
            val angleDiff = kotlin.math.abs(calculateShortestAngleDiff(normalizedAngle, keyNormalized))

            if (angleDiff <= 10f) {
                return directionText
            }
        }

        // 不在标准方位附近，每15度显示一次角度值
        if (roundedAngle % 15 == 0) {
            return "${roundedAngle}°"
        }

        return null
    }
}
