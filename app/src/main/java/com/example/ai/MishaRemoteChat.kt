package com.example.ai

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Calls the already-deployed Misha chat API. Provider credentials stay server-side on Vercel.
 * Returns null on a network/configuration error so the local intent engine can continue safely.
 */
class MishaRemoteChat {
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun reply(userText: String): String? = withContext(Dispatchers.IO) {
        try {
            val messages = JSONArray().put(
                JSONObject()
                    .put("role", "user")
                    .put("content", userText.take(12000))
            )
            val body = JSONObject().put("messages", messages)
                .toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url("https://misha-voice-ai-api.vercel.app/api/ai/chat")
                .post(body)
                .build()
            client.newCall(request).execute().use { response ->
                val payload = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    Log.w("MishaRemoteChat", "Chat API returned HTTP ${response.code}")
                    return@withContext null
                }
                val json = JSONObject(payload)
                json.optString("reply").trim().takeIf { it.isNotBlank() }
                    ?: json.optString("response").trim().takeIf { it.isNotBlank() }
            }
        } catch (e: Exception) {
            Log.w("MishaRemoteChat", "Remote chat unavailable; falling back to local assistant", e)
            null
        }
    }
}
