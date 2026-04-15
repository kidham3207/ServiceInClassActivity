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

    lateinit var textView: TextView

    var timerBinder: TimerService.TimerBinder? = null
    var isConnected = false

    val timeHandler = Handler(Looper.getMainLooper()){
        textView.text = it.what.toString()
        true
    }

    val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            timerBinder = service as TimerService.TimerBinder
            timerBinder!!.setHandler(timeHandler)
            isConnected = true

            val saved = timerBinder?.getSavedTime() ?: 0
            if (saved > 0) textView.text = "$saved"
        }


        override fun onServiceDisconnected(name: ComponentName?) {
            isConnected = false
        }
    }






    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textView = findViewById(R.id.textView)


        bindService(
            Intent(this, TimerService::class.java),
            serviceConnection,
            BIND_AUTO_CREATE
        )

        findViewById<Button>(R.id.startButton).setOnClickListener {
            if (isConnected) {
                val savedTime = timerBinder?.getSavedTime() ?: 0
                val startValue = if (savedTime > 0) savedTime else 100
                timerBinder?.start(startValue)
            }
        }


        findViewById<Button>(R.id.stopButton).setOnClickListener {
            if(isConnected) timerBinder?.pause()
        }
    }

    override fun onDestroy() {
        unbindService(serviceConnection)
        super.onDestroy()
    }


//    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
//        menuInflater.inflate(R.menu.main, menu)
//        return super.onCreateOptionsMenu(menu)
//    }
//
//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        when(item.itemId){
//            R.id.action_start -> {
//                timerBinder?.start(100)
//            }
//            R.id.action_stop ->{
//                timerBinder?.pause()
//            }
//            else -> return false
//        }
//        return true
//    }


}