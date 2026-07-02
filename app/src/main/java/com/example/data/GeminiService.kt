package com.example.data

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiApiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun generateContent(apiKey: String, prompt: String): String {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                // Build JSON request manually using standard built-in JSONObject
                val requestJson = JSONObject().apply {
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("text", prompt)
                                })
                            })
                        })
                    })
                    
                    // Request JSON response format
                    put("generationConfig", JSONObject().apply {
                        put("temperature", 0.2)
                        put("responseFormat", JSONObject().apply {
                            put("responseMimeType", "application/json")
                        })
                    })
                }

                val mediaType = "application/json; charset=utf-8".toMediaType()
                val body = requestJson.toString().toRequestBody(mediaType)
                
                val url = "$BASE_URL?key=$apiKey"
                val request = Request.Builder()
                    .url(url)
                    .post(body)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext "Error: HTTP ${response.code} ${response.message}"
                    }
                    val responseBody = response.body?.string() ?: return@withContext "Error: Empty response body"
                    
                    // Extract response text from Gemini's JSON
                    val root = JSONObject(responseBody)
                    val candidates = root.optJSONArray("candidates")
                    val firstCandidate = candidates?.optJSONObject(0)
                    val content = firstCandidate?.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    val firstPart = parts?.optJSONObject(0)
                    firstPart?.optString("text") ?: "[]"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                "Error: ${e.localizedMessage}"
            }
        }
    }
}
