package com.example.musicvisualizer

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Shader
import android.view.View
import android.view.animation.LinearInterpolator

class SpinningCdView(context: Context) : View(context) {

    private var bitmap: Bitmap? = null
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(180, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }
    private var spinAnimator: ObjectAnimator? = null

    fun setAlbumArt(bmp: Bitmap?) {
        bitmap = bmp
        invalidate()
    }

    fun startSpin() {
        spinAnimator?.cancel()
        spinAnimator = ObjectAnimator.ofFloat(this, "rotation", rotation, rotation + 360f).apply {
            duration = 8000
            interpolator = LinearInterpolator()
            repeatCount = ObjectAnimator.INFINITE
            start()
        }
    }

    fun stopSpin() {
        spinAnimator?.cancel()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size = Math.min(width, height).toFloat()
        val cx = width / 2f
        val cy = height / 2f
        val radius = size / 2f - 8f

        val bmp = bitmap
        if (bmp != null) {
            val shader = BitmapShader(bmp, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            val scale = size / Math.min(bmp.width, bmp.height)
            val matrix = android.graphics.Matrix()
            matrix.setScale(scale, scale)
            matrix.postTranslate(cx - bmp.width * scale / 2f, cy - bmp.height * scale / 2f)
            shader.setLocalMatrix(matrix)
            paint.shader = shader
        } else {
            paint.shader = null
            paint.color = Color.DKGRAY
        }

        canvas.drawCircle(cx, cy, radius, paint)
        canvas.drawCircle(cx, cy, radius, ringPaint)
        paint.shader = null
        paint.color = Color.argb(220, 20, 20, 20)
        canvas.drawCircle(cx, cy, radius * 0.12f, paint)
    }
}
