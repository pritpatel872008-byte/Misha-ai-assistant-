package com.example.tools

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

data class VisionAnalysisResult(
    val success: Boolean,
    val description: String,
    val extractedText: String? = null,
    val objectsDetected: List<String> = emptyList(),
    val errorMessage: String? = null
)

class VisionEngine(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Analyzes an image Bitmap or Uri using Gemini multimodal vision API.
     * Temporary analysis only (does not store image unless user requests save to Knowledge Vault).
     */
    suspend fun analyzeImage(
        bitmap: Bitmap,
        userPrompt: String = "Describe what you see in this image, identify any key objects or visible text in English/Hindi."
    ): VisionAnalysisResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext VisionAnalysisResult(
                success = true,
                description = "Vision Engine active: Image captured (${bitmap.width}x${bitmap.height}). Add your Gemini API Key in Settings to enable full multimodal scene description and OCR.",
                extractedText = null
            )
        }

        try {
            val base64Image = encodeBitmapToBase64(bitmap)
            val cleanJsonBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", userPrompt))
                            put(JSONObject().apply {
                                put("inlineData", JSONObject().apply {
                                    put("mimeType", "image/jpeg")
                                    put("data", base64Image)
                                })
                            })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.3)
                })
            }

            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
                .post(cleanJsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val resString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext VisionAnalysisResult(
                    success = false,
                    description = "Vision API error code ${response.code}",
                    errorMessage = resString
                )
            }

            val parsed = JSONObject(resString)
            val text = parsed.optJSONArray("candidates")?.optJSONObject(0)
                ?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.optString("text")
                ?: "No description generated."

            VisionAnalysisResult(
                success = true,
                description = text.trim()
            )
        } catch (e: Exception) {
            VisionAnalysisResult(
                success = false,
                description = "Vision analysis failed: ${e.localizedMessage}",
                errorMessage = e.message
            )
        }
    }

    suspend fun analyzeFromUri(uri: Uri, prompt: String): VisionAnalysisResult = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val bitmap = BitmapFactory.decodeStream(stream)
                if (bitmap != null) {
                    return@withContext analyzeImage(bitmap, prompt)
                }
            }
            VisionAnalysisResult(success = false, description = "Could not read image file.")
        } catch (e: Exception) {
            VisionAnalysisResult(success = false, description = "Error opening image: ${e.message}")
        }
    }

    private fun encodeBitmapToBase64(bitmap: Bitmap): String {
        // Resize to max 1024x1024 for fast network transfer
        val maxDim = 1024
        val scaled = if (bitmap.width > maxDim || bitmap.height > maxDim) {
            val ratio = Math.min(maxDim.toFloat() / bitmap.width, maxDim.toFloat() / bitmap.height)
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true)
        } else {
            bitmap
        }

        val outputStream = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }
}
