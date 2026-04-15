package edu.temple.myapplication

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView

class MainActivity : AppCompatActivity() {

    private lateinit var textView: TextView

    private val defaultValue = 20
    private var timerBinder: TimerService.TimerBinder? = null
    private var isBound = false

    private val handler = Handler(Looper.getMainLooper()) { msg ->
        textView.text = msg.what.toString()
        true
    }

    private val conn = object : ServiceConnection {
        override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
            timerBinder = p1 as TimerService.TimerBinder  // assign to timerBinder, not timerService
            timerBinder?.setHandler(handler)
            isBound = true

            // Show saved time in UI immediately on connect
            val saved = timerBinder?.getSavedTime() ?: 0
            textView.text = if (saved > 0) saved.toString() else defaultValue.toString()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            timerBinder = null
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textView = findViewById(R.id.textView)

        findViewById<Button>(R.id.startButton).setOnClickListener {
            if (isBound) {
                val saved = timerBinder?.getSavedTime() ?: 0
                val startValue = if (saved > 0) saved else defaultValue
                timerBinder?.start(startValue)
            }
        }

        findViewById<Button>(R.id.stopButton).setOnClickListener {
            if (isBound) timerBinder?.pause()  // Service is responsible for persisting on pause
        }
    }

    override fun onStart() {
        super.onStart()
        bindService(Intent(this, TimerService::class.java), conn, BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()
        if (isBound) {
            unbindService(conn)
            isBound = false
        }
    }
}