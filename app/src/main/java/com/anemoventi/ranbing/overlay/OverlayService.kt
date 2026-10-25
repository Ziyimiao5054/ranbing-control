package com.anemoventi.ranbing.overlay

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.IBinder
import android.os.SystemClock
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import com.anemoventi.ranbing.App
import com.anemoventi.ranbing.ble.ConnectionState
import com.anemoventi.ranbing.device.DeviceState
import com.anemoventi.ranbing.device.float
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** Small always-on-top readout: cold side, hot side and current power. */
class OverlayService : Service() {

    companion object {
        @Volatile
        var isRunning = false
            private set
        private const val PREFS = "overlay_position"
        private const val KEY_X = "x"
        private const val KEY_Y = "y"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var windowManager: WindowManager
    private var root: View? = null
    private lateinit var coldView: TextView
    private lateinit var hotView: TextView
    private lateinit var powerView: TextView
    private var params: WindowManager.LayoutParams? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        show()
        scope.launch {
            (application as App).device.state.collect { render(it) }
        }
    }

    override fun onDestroy() {
        isRunning = false
        params?.let {
            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putInt(KEY_X, it.x)
                .putInt(KEY_Y, it.y)
                .apply()
        }
        scope.cancel()
        root?.let { runCatching { windowManager.removeView(it) } }
        root = null
        super.onDestroy()
    }

    private fun show() {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(9), dp(12), dp(9))
            background = GradientDrawable().apply {
                cornerRadius = dp(12).toFloat()
                setColor(0x99000000.toInt())
            }
        }
        coldView = addRow(card, "冷端")
        hotView = addRow(card, "热端")
        powerView = addRow(card, "功率")

        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        val layout = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = prefs.getInt(KEY_X, dp(12))
            y = prefs.getInt(KEY_Y, dp(120))
        }
        params = layout

        card.setOnTouchListener(object : View.OnTouchListener {
            private var downX = 0f
            private var downY = 0f
            private var startX = 0
            private var startY = 0
            private var moved = false
            private var lastTapAt = 0L

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        downX = event.rawX
                        downY = event.rawY
                        startX = layout.x
                        startY = layout.y
                        moved = false
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (abs(event.rawX - downX) > dp(6).toFloat() ||
                            abs(event.rawY - downY) > dp(6).toFloat()
                        ) {
                            moved = true
                        }
                        val metrics = resources.displayMetrics
                        val maxX = (metrics.widthPixels - card.width).coerceAtLeast(0)
                        val maxY = (metrics.heightPixels - card.height).coerceAtLeast(0)
                        layout.x = (startX + (event.rawX - downX).toInt()).coerceIn(0, maxX)
                        layout.y = (startY + (event.rawY - downY).toInt()).coerceIn(0, maxY)
                        runCatching { windowManager.updateViewLayout(card, layout) }
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!moved) {
                            val now = SystemClock.uptimeMillis()
                            if (now - lastTapAt < 300L) {
                                stopSelf()
                            } else {
                                lastTapAt = now
                            }
                        }
                        return true
                    }
                }
                return false
            }
        })

        runCatching { windowManager.addView(card, layout) }
            .onFailure {
                stopSelf()
                return
            }
        root = card
    }

    private fun addRow(parent: LinearLayout, label: String): TextView {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(4), 0, 0)
        }
        row.addView(
            TextView(this).apply {
                text = label
                setTextColor(0xFFB4B4B4.toInt())
                textSize = 11f
            },
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f),
        )
        val value = TextView(this).apply {
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 13f
            typeface = Typeface.MONOSPACE
        }
        row.addView(
            value,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { leftMargin = dp(6) },
        )
        parent.addView(row)
        return value
    }

    private fun render(state: DeviceState) {
        val connected = state.connection is ConnectionState.Connected
        coldView.text = if (connected) formatTemperature(state.float("l_temp", 25f)) else "--"
        hotView.text = if (connected) formatTemperature(state.float("r_temp", 25f)) else "--"
        powerView.text = if (connected) "%.1f W".format(state.float("power")) else "--"
    }

    private fun formatTemperature(value: Float): String =
        if (value < -100f) "--" else "%.1f °C".format(value)

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
