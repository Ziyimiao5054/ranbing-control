package com.anemoventi.ranbing.overlay

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import com.anemoventi.ranbing.App
import com.anemoventi.ranbing.device.DeviceState
import com.anemoventi.ranbing.device.float
import com.anemoventi.ranbing.ble.ConnectionState
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
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var windowManager: WindowManager
    private var root: View? = null
    private lateinit var coldView: TextView
    private lateinit var hotView: TextView
    private lateinit var powerView: TextView

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
        scope.cancel()
        root?.let { runCatching { windowManager.removeView(it) } }
        root = null
        super.onDestroy()
    }

    private fun show() {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
            minimumWidth = dp(148)
            background = GradientDrawable().apply {
                cornerRadius = dp(16).toFloat()
                setColor(0xE6101010.toInt())
                setStroke(dp(1), 0x33FFFFFF)
            }
        }
        card.addView(
            TextView(this).apply {
                text = "记录"
                setTextColor(0xFF9AA0A6.toInt())
                textSize = 11f
                letterSpacing = 0.08f
            },
        )
        coldView = addRow(card, "冷端")
        hotView = addRow(card, "热端")
        powerView = addRow(card, "功率")

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = dp(12)
            y = dp(96)
        }

        card.setOnTouchListener(object : View.OnTouchListener {
            private var downX = 0f
            private var downY = 0f
            private var startX = 0
            private var startY = 0

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        downX = event.rawX
                        downY = event.rawY
                        startX = params.x
                        startY = params.y
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = startX - (event.rawX - downX).toInt()
                        params.y = startY + (event.rawY - downY).toInt()
                        runCatching { windowManager.updateViewLayout(card, params) }
                        return true
                    }
                }
                return false
            }
        })

        runCatching { windowManager.addView(card, params) }
            .onFailure {
                stopSelf()
                return
            }
        root = card
    }

    private fun addRow(parent: LinearLayout, label: String): TextView {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(8), 0, 0)
        }
        row.addView(
            TextView(this).apply {
                text = label
                setTextColor(0xFF9AA0A6.toInt())
                textSize = 12f
            },
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f),
        )
        val value = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 15f
            typeface = Typeface.MONOSPACE
        }
        row.addView(value)
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
