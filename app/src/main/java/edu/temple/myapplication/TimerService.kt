package edu.temple.myapplication

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.util.Log

class TimerService : Service() {

    private var isRunning = false
    private var paused = false
    private var currentValue = 0
    private var timerHandler: Handler? = null
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
            if (this@TimerService.isRunning && !this@TimerService.paused) return

            if (this@TimerService.paused) {
                this@TimerService.resume()
            } else {
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

    override fun onCreate() {
        super.onCreate()
        Log.d("TimerService status", "Created")
    }

    override fun onBind(intent: Intent): IBinder = TimerBinder()

    fun start(startValue: Int) {
        t = TimerThread(startValue)
        t.start()
    }

    fun resume() {
        this@TimerService.paused = false
        this@TimerService.isRunning = true
        if (::t.isInitialized) {
            synchronized(t) {
                (t as java.lang.Object).notify()
            }
        }
    }

    fun pause() {
        if (::t.isInitialized && isRunning) {
            paused = true
            isRunning = false
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

                    synchronized(this) {
                        while (this@TimerService.paused) {
                            (this as java.lang.Object).wait()
                        }
                    }

                    sleep(1000)
                }
                isRunning = false
                clearSavedTime()
            } catch (e: InterruptedException) {
                isRunning = false
                paused = false
                Log.d("TimerService", "Thread interrupted: $e")
            }
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
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