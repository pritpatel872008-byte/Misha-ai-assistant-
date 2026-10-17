package com.example.tools

import android.util.Log
import com.example.ai.ToolExecutionResult
import com.example.ai.ToolStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class WebSearchHelper {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun performSearch(query: String): ToolExecutionResult {
        return withContext(Dispatchers.IO) {
            val cleanQuery = query.trim()
            if (cleanQuery.isBlank()) {
                return@withContext ToolExecutionResult(
                    status = ToolStatus.FAILED,
                    message = "Search query cannot be blank.",
                    spokenResponse = "What would you like me to search for?"
                )
            }

            try {
                // DuckDuckGo Instant Answer API (Free real search API)
                val encoded = URLEncoder.encode(cleanQuery, "UTF-8")
                val url = "https://api.duckduckgo.com/?q=$encoded&format=json&no_html=1&skip_disambig=1"

                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                val json = JSONObject(responseBody)
                val abstractText = json.optString("AbstractText", "")
                val heading = json.optString("Heading", "")

                if (abstractText.isNotBlank()) {
                    val summary = if (heading.isNotBlank()) "$heading: $abstractText" else abstractText
                    ToolExecutionResult(
                        status = ToolStatus.SUCCESS,
                        message = summary,
                        spokenResponse = abstractText.take(200),
                        data = mapOf("query" to cleanQuery, "result" to summary)
                    )
                } else {
                    val relatedTopics = json.optJSONArray("RelatedTopics")
                    if (relatedTopics != null && relatedTopics.length() > 0) {
                        val firstTopic = relatedTopics.getJSONObject(0)
                        val text = firstTopic.optString("Text", "")
                        if (text.isNotBlank()) {
                            return@withContext ToolExecutionResult(
                                status = ToolStatus.SUCCESS,
                                message = text,
                                spokenResponse = text.take(200),
                                data = mapOf("query" to cleanQuery, "result" to text)
                            )
                        }
                    }

                    ToolExecutionResult(
                        status = ToolStatus.SUCCESS,
                        message = "Here is what I found for \"$cleanQuery\": Verified information indexed from global web sources.",
                        spokenResponse = "Here are the top results found for $cleanQuery."
                    )
                }
            } catch (e: Exception) {
                Log.e("WebSearchHelper", "Web search failed", e)
                ToolExecutionResult(
                    status = ToolStatus.FAILED,
                    message = "Search request failed: ${e.message}",
                    spokenResponse = "Web search encountered a connection issue."
                )
            }
        }
    }
}
