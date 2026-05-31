package com.jarvis.app

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.provider.Settings
import android.hardware.camera2.CameraManager
import org.json.JSONObject

class ActionExecutor(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private var flashlightOn = false

    fun execute(intentJson: String): String {
        return try {
            val json = JSONObject(intentJson)
            val action = json.getString("action")
            val params = json.optJSONObject("params") ?: JSONObject()
            val speech = json.optString("speech", "Done!")

            when (action) {
                "SET_BRIGHTNESS" -> {
                    val level = params.optInt("level", 128)
                    Settings.System.putInt(
                        context.contentResolver,
                        Settings.System.SCREEN_BRIGHTNESS,
                        level
                    )
                }
                "SET_VOLUME" -> {
                    val level = params.optInt("level", 8)
                    audioManager.setStreamVolume(
                        AudioManager.STREAM_MUSIC, level, 0
                    )
                }
                "TOGGLE_WIFI" -> {
                    val state = params.optString("state", "on")
                    @Suppress("DEPRECATION")
                    wifiManager.isWifiEnabled = state == "on"
                }
                "TOGGLE_FLASHLIGHT" -> {
                    val state = params.optString("state", "on")
                    val cameraId = cameraManager.cameraIdList[0]
                    flashlightOn = state == "on"
                    cameraManager.setTorchMode(cameraId, flashlightOn)
                }
                "OPEN_APP" -> {
                    val appName = params.optString("package", "")
                    openApp(appName)
                }
                "SET_ALARM" -> {
                    val hour = params.optInt("hour", 8)
                    val minute = params.optInt("minute", 0)
                    val alarmIntent = Intent(android.provider.AlarmClock.ACTION_SET_ALARM).apply {
                        putExtra(android.provider.AlarmClock.EXTRA_HOUR, hour)
                        putExtra(android.provider.AlarmClock.EXTRA_MINUTES, minute)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(alarmIntent)
                }
                "ANSWER_QUESTION" -> {
                    // Just speak the answer
                }
            }
            speech
        } catch (e: Exception) {
            "Sorry I couldn't do that"
        }
    }

    private fun openApp(appName: String) {
        val pm = context.packageManager
        val intent = pm.getLaunchIntentForPackage(appName)
        if (intent != null) {
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        } else {
            val searchIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val apps = pm.queryIntentActivities(searchIntent, 0)
            for (app in apps) {
                val label = app.loadLabel(pm).toString().lowercase()
                if (label.contains(appName.lowercase())) {
                    val launchIntent = pm.getLaunchIntentForPackage(app.activityInfo.packageName)
                    launchIntent?.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    launchIntent?.let { context.startActivity(it) }
                    break
                }
            }
        }
    }
}
