package com.jarvis.app

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class GeminiHandler {

    private val client = OkHttpClient()
    private val apiKey = "PASTE_YOUR_KEY_HERE"
    private val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"

    fun getIntent(command: String, callback: (String) -> Unit) {
        val prompt = """
            You are Jarvis, an AI assistant controlling an Android tablet.
            The user said: "$command"
            
            Respond with ONLY a JSON object in this exact format:
            {
                "action": "ACTION_NAME",
                "params": {"key": "value"},
                "speech": "What you say to the user"
            }
            
            Available actions:
            - SET_BRIGHTNESS (params: level 0-255)
            - SET_VOLUME (params: level 0-15)
            - TOGGLE_WIFI (params: state "on"/"off")
            - TOGGLE_BLUETOOTH (params: state "on"/"off")
            - TOGGLE_FLASHLIGHT (params: state "on"/"off")
            - OPEN_APP (params: package name or app name)
            - SET_ALARM (params: hour, minute)
            - ANSWER_QUESTION (params: answer text)
            - UNKNOWN
            
            Respond ONLY with the JSON, no other text.
        """.trimIndent()

        val json = JSONObject()
        val contents = JSONArray()
        val content = JSONObject()
        val parts = JSONArray()
        val part = JSONObject()
        part.put("text", prompt)
        parts.put(part)
        content.put("parts", parts)
        contents.put(content)
        json.put("contents", contents)

        val body = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(url).post(body).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: java.io.IOException) {
                callback("{\"action\":\"UNKNOWN\",\"params\":{},\"speech\":\"Sorry I couldn't connect.\"}")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseText = response.body?.string() ?: ""
                try {
                    val jsonResponse = JSONObject(responseText)
                    val text = jsonResponse
                        .getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text")
                        .trim()
                    callback(text)
                } catch (e: Exception) {
                    callback("{\"action\":\"UNKNOWN\",\"params\":{},\"speech\":\"Sorry I had an error.\"}")
                }
            }
        })
    }
}
