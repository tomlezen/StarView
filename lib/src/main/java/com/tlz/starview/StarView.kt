package com.tlz.starview

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Created by Tomlezen.
 * Data: 2018/7/26.
 * Time: 13:54.
 */
class StarView(ctx: Context, attrs: AttributeSet) : View(ctx, attrs) {

    /** 是否可以选择. */
    var selectable = false
        set(value) {
            field = value
            if (isInitFinish) {
                postInvalidate()
            }
        }

    /** 星星总数. */
    private val starTotalNum: Int
    private var oldStarNum = 0f
    /** 当前星星数，支持小数 */
    var starNum: Float = 0f
        set(value) {
            val v = min(starTotalNum.toFloat(), value)
            if (field != v) {
                oldStarNum = field
                field = v
                if (isInitFinish) {
                    postInvalidate()
                }
            }
        }
    /** 星星大小. */
    private val starSize: Int
    /** 星星之间间隔. */
    var starSpace: Int = 0
        set(value) {
            field = value
            if (isInitFinish) {
                requestLayout()
            }
        }
    /** 星星半径比例. */
    var starRadiusScale: Float = .1f
        set(value) {
            field = max(.1f, min(1f, value))
            if (isInitFinish) {
                computeStarPoints()
                postInvalidate()
            }
        }
    /** 默认内间距. */
    private val defPadding: Int = resources.getDimensionPixelSize(R.dimen.def_star_padding)
    /** 星星颜色. */
    var starColor = Color.GRAY
        set(value) {
            field = value
            if (isInitFinish) {
                postInvalidate()
            }
        }
    /** 星星线条宽度. */
    var starStrokeWidth = 0
        set(value) {
            field = value
            if (isInitFinish) {
                postInvalidate()
            }
        }
    /** 星星选中状态的颜色. */
    var starSelectColor = Color.RED
        set(value) {
            field = value
            if (isInitFinish) {
                postInvalidate()
            }
        }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val starPath = Path()
    private val starPathMeasure = PathMeasure()

    private val outPoints = mutableListOf<PointF>()
    private val inPoints = mutableListOf<PointF>()
    private val starRectF = RectF()
    private val clipRectF = RectF()

    private var actionDownX = 0f
    private var actionDownY = 0f

    private var clickPosition = 0
        set(value) {
            field = value
            startAnimator()
        }
    private var animator: ValueAnimator? = null
    private var animatorValue = 0f
    private val argbEvaluator by lazy { ArgbEvaluator() }

    var onStarSelected: ((Float) -> Unit)? = null

    private var isInitFinish = false

    init {

        val ta = ctx.obtainStyledAttributes(attrs, R.styleable.StarView)

        selectable = ta.getBoolean(R.styleable.StarView_star_selectable, false)

        starTotalNum = max(1, ta.getInteger(R.styleable.StarView_star_total_num, resources.getInteger(R.integer.def_star_total_num)))
        starNum = ta.getFloat(R.styleable.StarView_star_num, 0f)

        starSize = ta.getDimensionPixelSize(R.styleable.StarView_star_size, resources.getDimensionPixelSize(R.dimen.def_star_size))
        starSpace = ta.getDimensionPixelSize(R.styleable.StarView_star_space, resources.getDimensionPixelSize(R.dimen.def_star_space))
        starStrokeWidth = ta.getDimensionPixelSize(R.styleable.StarView_star_stroke_width, resources.getDimensionPixelSize(R.dimen.def_star_stroke_width))

        starColor = ta.getColor(R.styleable.StarView_star_color, resources.getColor(R.color.def_star_color))
        starSelectColor = ta.getColor(R.styleable.StarView_star_select_color, resources.getColor(R.color.def_star_select_color))

        starRadiusScale = ta.getFloat(R.styleable.StarView_star_radius_scale, .6f)

        ta.recycle()

        isInitFinish = true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthSpecMode = View.MeasureSpec.getMode(widthMeasureSpec)
        val widthSpecSize = View.MeasureSpec.getSize(widthMeasureSpec)
        var widthSize = widthSpecSize
        if (widthSpecMode == MeasureSpec.UNSPECIFIED || widthSpecMode == MeasureSpec.AT_MOST) {
            widthSize = paddingLeft + paddingRight + defPadding * 2 + starSize * starTotalNum + starSpace * (starTotalNum - 1)
        }
        val heightSpecMode = View.MeasureSpec.getMode(heightMeasureSpec)
        val heightSpecSize = View.MeasureSpec.getSize(heightMeasureSpec)
        var heightSize = heightSpecSize
        if (heightSpecMode == MeasureSpec.UNSPECIFIED || widthSpecMode == MeasureSpec.AT_MOST) {
            heightSize = paddingTop + paddingBottom + defPadding * 2 + starSize
        }
        setMeasuredDimension(widthSize, heightSize)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (changed) {
            // 计算第一个星星绘制区域
            val startX = (width / 2 - (starSize * starTotalNum + starSpace * (starTotalNum - 1)) / 2f)
            starRectF.set(startX, height / 2f - starSize / 2, startX + starSize, height / 2f + starSize / 2)

            computeStarPoints()
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.let { cvs ->
            if (inPoints.isNotEmpty() && inPoints.size == outPoints.size) {
                val dist = starRectF.width() + starSpace
                (0 until starTotalNum).forEach { index ->
                    val tDist = dist * index
                    starPath.reset()
                    val isAnimatorRunning = animator?.isRunning == true
                    (0 until 5).forEach {
                        val inP = inPoints[it]
                        if (it == 0) {
                            starPath.moveTo(inP.x + tDist, inP.y)
                        } else {
                            starPath.lineTo(inP.x + tDist, inP.y)
                        }

                        val outP = outPoints[it]
                        starPath.lineTo(outP.x + tDist, outP.y)
                    }
                    starPath.close()
                    val d = starNum - index.toFloat() - 1f
                    when {
                        d >= 0f -> {
                            cvs.save()
                            paint.color = starSelectColor
                            if (isAnimatorRunning && (index == clickPosition || (selectable && starNum > oldStarNum && (index + 1) in (oldStarNum + 1..starNum)))) {
                                cvs.rotate(animatorValue * 72, starRectF.centerX() + tDist, starRectF.centerY())
                                if (selectable && (index != clickPosition || (index + 1 > oldStarNum && oldStarNum - oldStarNum.toInt() == 0f))) {
                                    paint.color = computeColor(starColor, starSelectColor)
                                }
                            }
                            paint.style = Paint.Style.FILL_AND_STROKE
                            cvs.drawPath(starPath, paint)
                            cvs.restore()
                        }
                        d <= -1f -> {
                            cvs.save()
                            paint.color = starColor
                            if (isAnimatorRunning && (index == clickPosition || (selectable && starNum < oldStarNum && (index + 1) in (starNum + 1..oldStarNum)))) {
                                cvs.rotate(animatorValue * 72, starRectF.centerX() + tDist, starRectF.centerY())
                                if (selectable && index != clickPosition) {
                                    paint.color = computeColor(starSelectColor, starColor)
                                    paint.style = Paint.Style.FILL_AND_STROKE
                                    cvs.drawPath(starPath, paint)
                                }
                            }
                            paint.style = Paint.Style.STROKE
                            paint.strokeWidth = starStrokeWidth.toFloat()
                            cvs.drawPath(starPath, paint)
                            cvs.restore()
                        }
                        else -> {
                            cvs.save()
                            paint.color = starSelectColor
                            paint.style = Paint.Style.STROKE
                            paint.strokeWidth = starStrokeWidth.toFloat()
                            clipRectF.set(starRectF)
                            clipRectF.offset(tDist, 0f)
                            clipRectF.left = clipRectF.right - clipRectF.width() * (1f + starNum.toInt() - starNum)
                            if (selectable || index == clickPosition) {
                                cvs.rotate(animatorValue * 72, starRectF.centerX() + tDist, starRectF.centerY())
                            }
                            if (selectable && animator?.isRunning == true && index != clickPosition) {
                                if (starNum > oldStarNum) {
                                    paint.color = starSelectColor
                                    clipRectF.left = clipRectF.right - clipRectF.width() * (oldStarNum.toInt() + 1f - oldStarNum) * (1 - animatorValue)
                                } else {
                                    paint.color = computeColor(starSelectColor, starColor)
                                    clipRectF.left = clipRectF.right - clipRectF.width() * ((oldStarNum.toInt() + 1f - oldStarNum) + (oldStarNum - oldStarNum.toInt()) * animatorValue)
                                }
                            }
                            cvs.drawPath(starPath, paint)
                            cvs.restore()
                            canvas.clipRect(clipRectF, Region.Op.DIFFERENCE)
                            cvs.save()
                            if (selectable || index == clickPosition) {
                                cvs.rotate(animatorValue * 72, starRectF.centerX() + tDist, starRectF.centerY())
                            }
                            paint.style = Paint.Style.FILL_AND_STROKE
                            cvs.drawPath(starPath, paint)
                            cvs.restore()
                            paint.color = Color.TRANSPARENT
                            cvs.drawRect(clipRectF, paint)
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                actionDownX = event.x
                actionDownY = event.y
            }
            MotionEvent.ACTION_UP -> {
                if (abs(actionDownX - event.x) < 10f && abs(actionDownY - event.y) < 10f) {
                    computeClickPosition(event.x, event.y)
                }
            }
        }
        return true
    }

    /**
     * 计算点击位置.
     * @param x Float
     * @param y Float
     */
    private fun computeClickPosition(x: Float, y: Float) {
        val dist = starRectF.width() + starSpace
        run Break@{
            (0 until 5).forEach {
                clipRectF.set(starRectF)
                clipRectF.offset(dist * it, 0f)
                if (clipRectF.contains(x, y)) {
                    clickPosition = it
                    if (selectable) {
                        oldStarNum = starNum
                        starNum = it + 1f
                        onStarSelected?.invoke(starNum)
                    }
                    return@Break
                }
            }
        }
    }

    private fun startAnimator() {
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            addUpdateListener {
                animatorValue = it.animatedValue as Float
                postInvalidate()
            }
            start()
        }
    }

    /**
     * 计算五角星的10个点.
     * 从右下边第一个顺时针计算.
     */
    private fun computeStarPoints() {
        outPoints.clear()
        inPoints.clear()
        starPath.reset()
        starPath.addCircle(starRectF.centerX(), starRectF.centerY(), starRectF.width() / 2, Path.Direction.CW)
        starPathMeasure.setPath(starPath, false)
        val pArray = FloatArray(2)
        (0 until 5).forEach {
            starPathMeasure.getPosTan(starPathMeasure.length * (it * 72 + 54) / 360, pArray, null)
            outPoints.add(PointF(pArray[0], pArray[1]))
        }
        starPath.reset()
        starPath.addCircle(starRectF.centerX(), starRectF.centerY(), starRectF.width() / 2 * starRadiusScale, Path.Direction.CW)
        starPathMeasure.setPath(starPath, false)
        (0 until 5).forEach {
            starPathMeasure.getPosTan(starPathMeasure.length * (it * 72 + 18) / 360, pArray, null)
            inPoints.add(PointF(pArray[0], pArray[1]))
        }
    }

    private fun computeColor(startColor: Int, endColor: Int) =
            argbEvaluator.evaluate(animatorValue, startColor, endColor) as Int

}