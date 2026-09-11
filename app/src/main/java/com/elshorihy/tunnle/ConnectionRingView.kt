package com.elshorihy.tunnle

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.sin

class ConnectionRingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 7f }
    private val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2f }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; typeface = android.graphics.Typeface.DEFAULT_BOLD }
    private var connected = false
    private var connecting = false
    private var phase = 0f

    fun setState(isConnected: Boolean, isConnecting: Boolean = false) {
        connected = isConnected
        connecting = isConnecting
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val radius = (minOf(width, height) / 2f) - 22f

        ringPaint.color = android.graphics.Color.rgb(52, 226, 240)
        ringPaint.setShadowLayer(22f, 0f, 0f, android.graphics.Color.rgb(0, 210, 255))
        setLayerType(View.LAYER_TYPE_SOFTWARE, ringPaint)
        canvas.drawCircle(cx, cy, radius, ringPaint)
        ringPaint.clearShadowLayer()

        innerPaint.color = android.graphics.Color.argb(120, 80, 220, 240)
        innerPaint.strokeWidth = 2f
        canvas.drawArc(RectF(cx-radius+16, cy-radius+16, cx+radius-16, cy+radius-16), -55f + phase, 250f, false, innerPaint)

        textPaint.color = android.graphics.Color.rgb(52, 226, 240)
        textPaint.textSize = 20f
        val state = when { connecting -> "CONNECTING"; connected -> "CONNECTED"; else -> "DISCONNECTED" }
        canvas.drawText(state, cx, cy + 10f, textPaint)

        textPaint.textSize = 30f
        textPaint.color = android.graphics.Color.WHITE
        canvas.drawText("⏻", cx, cy - 28f, textPaint)

        textPaint.textSize = 12f
        textPaint.color = android.graphics.Color.LTGRAY
        canvas.drawText("↓ 0 B/s     ↑ 0 B/s", cx, cy + 42f, textPaint)
        canvas.drawText("Ping  —", cx, cy + 62f, textPaint)

        if (connecting) {
            phase += 4f
            postInvalidateDelayed(32)
        }
    }
}
