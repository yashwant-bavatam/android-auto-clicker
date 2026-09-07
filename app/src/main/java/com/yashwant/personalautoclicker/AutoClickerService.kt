package com.yashwant.personalautoclicker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent

class AutoClickerService : AccessibilityService() {

    companion object {
        var instance: AutoClickerService? = null
    }

    private val handler = Handler(Looper.getMainLooper())

    private var clickRunnable: Runnable? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No event handling needed for now.
    }

    override fun onInterrupt() {
        stopAutoClick()
    }

    // -------------------------
    // TAP
    // -------------------------

    fun performTap(
        x: Float,
        y: Float
    ) {
        performTap(
            x = x,
            y = y,
            onCompleted = null
        )
    }

    fun performTap(
        x: Float,
        y: Float,
        onCompleted: (() -> Unit)?
    ) {

        val path =
            Path().apply {
                moveTo(x, y)
            }

        val gesture =
            GestureDescription.Builder()
                .addStroke(
                    GestureDescription.StrokeDescription(
                        path,
                        0,
                        50
                    )
                )
                .build()

        dispatchGesture(
            gesture,
            object : GestureResultCallback() {

                override fun onCompleted(
                    gestureDescription: GestureDescription?
                ) {
                    super.onCompleted(
                        gestureDescription
                    )

                    onCompleted?.invoke()
                }

                override fun onCancelled(
                    gestureDescription: GestureDescription?
                ) {
                    super.onCancelled(
                        gestureDescription
                    )

                    onCompleted?.invoke()
                }
            },
            null
        )
    }

    // -------------------------
    // SWIPE
    // -------------------------

    fun performSwipe(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        durationMs: Long = 500L,
        onCompleted: (() -> Unit)? = null
    ) {

        val safeDuration =
            durationMs.coerceAtLeast(100L)

        val path =
            Path().apply {

                moveTo(
                    startX,
                    startY
                )

                lineTo(
                    endX,
                    endY
                )
            }

        val gesture =
            GestureDescription.Builder()
                .addStroke(
                    GestureDescription.StrokeDescription(
                        path,
                        0,
                        safeDuration
                    )
                )
                .build()

        dispatchGesture(
            gesture,
            object : GestureResultCallback() {

                override fun onCompleted(
                    gestureDescription: GestureDescription?
                ) {

                    super.onCompleted(
                        gestureDescription
                    )

                    onCompleted?.invoke()
                }

                override fun onCancelled(
                    gestureDescription: GestureDescription?
                ) {

                    super.onCancelled(
                        gestureDescription
                    )

                    onCompleted?.invoke()
                }
            },
            null
        )
    }

    // -------------------------
    // SINGLE POINT AUTO CLICK
    // -------------------------

    fun startAutoClick(
        x: Float,
        y: Float,
        intervalMs: Long
    ) {

        stopAutoClick()

        val safeInterval =
            intervalMs.coerceAtLeast(100L)

        val runnable =
            object : Runnable {

                override fun run() {

                    performTap(
                        x,
                        y
                    )

                    handler.postDelayed(
                        this,
                        safeInterval
                    )
                }
            }

        clickRunnable =
            runnable

        handler.post(
            runnable
        )
    }

    fun stopAutoClick() {

        clickRunnable?.let {
            handler.removeCallbacks(
                it
            )
        }

        clickRunnable =
            null
    }

    override fun onDestroy() {

        stopAutoClick()

        instance = null

        super.onDestroy()
    }
}