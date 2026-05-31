package com.jarvis.app

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var tvSubStatus: TextView
    private lateinit var tvCommand: TextView
    private lateinit var tvResponse: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        tvSubStatus = findViewById(R.id.tvSubStatus)
        tvCommand = findViewById(R.id.tvCommand)
        tvResponse = findViewById(R.id.tvResponse)

        val serviceIntent = Intent(this, JarvisService::class.java)
        startForegroundService(serviceIntent)

        JarvisService.statusCallback = { command, response ->
            runOnUiThread {
                tvCommand.text = command
                tvResponse.text = response
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        JarvisService.statusCallback = null
    }
}
