package com.jarvis.app

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import java.util.Locale

class JarvisService : Service(), TextToSpeech.OnInitListener {

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var tts: TextToSpeech
    private lateinit var gemini: GeminiHandler
    private lateinit var executor: ActionExecutor
    private var isListening = false
    private var isAwake = false
    private var ttsReady = false

    companion object {
        var statusCallback: ((String, String) -> Unit)? = null
        const val CHANNEL_ID = "JarvisChannel"
    }

    override fun onCreate() {
        super.onCreate()
        gemini = GeminiHandler()
        executor = ActionExecutor(this)
        tts = TextToSpeech(this, this)
        createNotificationChannel()
        startForeground(1, buildNotification())
        setupSpeechRecognizer()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.US
            ttsReady = true
            speak("Jarvis is ready. Say wake up buddy to activate me.")
            startListening()
        }
    }

    private fun setupSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.get(0)?.lowercase() ?: ""
                handleSpeech(text)
                startListening()
            }
            override fun onError(error: Int) { startListening() }
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        try {
            speechRecognizer.startListening(intent)
        } catch (e: Exception) {
            android.os.Handler(mainLooper).postDelayed({ startListening() }, 1000)
        }
    }

    private fun handleSpeech(text: String) {
        if (text.isEmpty()) return

        if (!isAwake) {
            if (text.contains("wake up buddy") || text.contains("wake up") || text.contains("buddy")) {
                isAwake = true
                speak("I'm awake! How can I help you?")
                statusCallback?.invoke("Activated!", "Listening for commands...")
            }
            return
        }

        if (text.contains("go to sleep") || text.contains("sleep")) {
            isAwake = false
            speak("Going to sleep. Say wake up buddy to activate me again.")
            statusCallback?.invoke("Sleeping", "Say: Wake up buddy")
            return
        }

        statusCallback?.invoke(text, "Processing...")

        gemini.getIntent(text) { intentJson ->
            val response = executor.execute(intentJson)
            speak(response)
            statusCallback?.invoke(text, response)
        }
    }

    private fun speak(text: String) {
        if (ttsReady) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Jarvis", NotificationManager.IMPORTANCE_LOW)
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Jarvis is running")
            .setContentText("Say 'Wake up buddy' to activate")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        speechRecognizer.destroy()
        tts.shutdown()
        super.onDestroy()
    }
}
