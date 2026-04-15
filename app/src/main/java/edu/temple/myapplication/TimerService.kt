package edu.temple.myapplication

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.util.Log

@Suppress("ControlFlowWithEmptyBody")
class TimerService : Service() {

    private var isRunning = false
    private var timerHandler: Handler? = null
    private var paused = false
    private var currentValue = 0
    lateinit var t: TimerThread

    private val prefs by lazy {
        getSharedPreferences("timer_prefs", Context.MODE_PRIVATE)
    }

    inner class TimerBinder : Binder() {

        val isRunning: Boolean
            get() = this@TimerService.isRunning

        val paused: Boolean
            get() = this@TimerService.paused

        fun start(startValue: Int) {
            if (isRunning && !paused) return  // already running, do nothing

            if (paused) {
                // Resume from where we left off
                this@TimerService.paused = false
                this@TimerService.isRunning = true
                synchronized(t) { t.notify() }
            } else {
                // Fresh start — clear any saved value
                clearSavedTime()
                if (::t.isInitialized) t.interrupt()
                this@TimerService.start(startValue)
            }
        }

        fun setHandler(handler: Handler) {
            timerHandler = handler
        }

        fun pause() {
            this@TimerService.pause()
        }

        fun getSavedTime(): Int {
            return prefs.getInt("saved_time", 0)
        }
    }

    override fun onBind(intent: Intent): IBinder = TimerBinder()

    fun start(startValue: Int) {
        t = TimerThread(startValue)
        t.start()
    }

    fun pause() {
        if (::t.isInitialized && isRunning) {
            paused = true
            isRunning = false
            // Save the current value when paused
            prefs.edit().putInt("saved_time", currentValue).apply()
        }
    }

    fun clearSavedTime() {
        prefs.edit().remove("saved_time").apply()
    }

    inner class TimerThread(private val startValue: Int) : Thread() {
        override fun run() {
            isRunning = true
            try {
                for (i in startValue downTo 1) {
                    currentValue = i
                    timerHandler?.sendEmptyMessage(i)

                    // Block cleanly while paused
                    synchronized(this) {
                        while (paused) wait()
                    }

                    sleep(1000)
                }
                // Countdown completed naturally — clear saved state
                isRunning = false
                clearSavedTime()
            } catch (e: InterruptedException) {
                isRunning = false
                paused = false
            }
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        // If not paused, interrupt the thread — no save occurs (requirement 3)
        if (::t.isInitialized && isRunning && !paused) {
            t.interrupt()
        }
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("TimerService status", "Destroyed")
    }
}