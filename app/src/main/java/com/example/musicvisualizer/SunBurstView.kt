package com.example.musicvisualizer

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.view.View
import android.view.animation.LinearInterpolator

class SunBurstView(context: Context) : View(context) {

    private var angle = 0f
    private var pulse = 0f
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val animator = ValueAnimator.ofFloat(0f, 360f).apply {
        duration = 12000
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            angle = it.animatedValue as Float
            pulse = (Math.sin(angle / 20.0) * 0.5 + 0.5).toFloat()
            invalidate()
        }
    }

    fun start() {
        if (!animator.isStarted) animator.start()
    }

    fun stop() {
        animator.cancel()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val maxRadius = Math.max(width, height) * 0.75f

        val hue = (angle * 0.5f) % 360f
        val color = Color.HSVToColor(floatArrayOf(hue, 0.55f, 1f))

        paint.shader = RadialGradient(
            cx, cy, maxRadius * (0.6f + pulse * 0.2f),
            intArrayOf(color, Color.TRANSPARENT),
            null, Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        paint.shader = null
        paint.color = Color.argb(60, 255, 255, 255)
        canvas.save()
        canvas.rotate(angle, cx, cy)
        val rayCount = 12
        for (i in 0 until rayCount) {
            canvas.save()
            canvas.rotate((360f / rayCount) * i, cx, cy)
            canvas.drawRect(
                cx - 4f, cy - maxRadius, cx + 4f, cy - maxRadius * 0.3f, paint
            )
            canvas.restore()
        }
        canvas.restore()
    }
}
