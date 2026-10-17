package com.example.tools

import android.util.Log
import com.example.ai.ToolExecutionResult
import com.example.ai.ToolStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class WeatherHelper {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun getWeather(location: String): ToolExecutionResult {
        return withContext(Dispatchers.IO) {
            val queryCity = if (location.isBlank()) "Ahmedabad" else location.trim()

            try {
                // Step 1: Geocode city name to lat/long using Open-Meteo Geocoding API
                val geocodeUrl = "https://geocoding-api.open-meteo.com/v1/search?name=${queryCity}&count=1&language=en&format=json"
                val geoRequest = Request.Builder().url(geocodeUrl).build()
                val geoResponse = client.newCall(geoRequest).execute()
                val geoBody = geoResponse.body?.string() ?: ""

                val geoJson = JSONObject(geoBody)
                val results = geoJson.optJSONArray("results")
                if (results == null || results.length() == 0) {
                    return@withContext ToolExecutionResult(
                        status = ToolStatus.FAILED,
                        message = "Could not find coordinates for $queryCity.",
                        spokenResponse = "I couldn't find location information for $queryCity."
                    )
                }

                val firstLocation = results.getJSONObject(0)
                val lat = firstLocation.getDouble("latitude")
                val lon = firstLocation.getDouble("longitude")
                val resolvedName = firstLocation.getString("name")
                val country = firstLocation.optString("country", "")

                // Step 2: Fetch current weather metrics
                val weatherUrl = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m"
                val weatherReq = Request.Builder().url(weatherUrl).build()
                val weatherResp = client.newCall(weatherReq).execute()
                val weatherBody = weatherResp.body?.string() ?: ""

                val weatherJson = JSONObject(weatherBody)
                val current = weatherJson.getJSONObject("current")
                val temp = current.getDouble("temperature_2m")
                val feelsLike = current.getDouble("apparent_temperature")
                val humidity = current.getInt("relative_humidity_2m")
                val wind = current.getDouble("wind_speed_10m")
                val weatherCode = current.getInt("weather_code")

                val condition = parseWmoWeatherCode(weatherCode)

                val summary = "$resolvedName ($country): $temp°C, $condition. Feels like $feelsLike°C with humidity $humidity%."
                val spoken = "$resolvedName mein abhi temperature $temp degrees hai, aur mausam $condition hai."

                ToolExecutionResult(
                    status = ToolStatus.SUCCESS,
                    message = summary,
                    spokenResponse = spoken,
                    data = mapOf(
                        "city" to resolvedName,
                        "temperature" to temp,
                        "condition" to condition,
                        "feelsLike" to feelsLike,
                        "humidity" to humidity,
                        "windSpeed" to wind
                    )
                )
            } catch (e: Exception) {
                Log.e("WeatherHelper", "Weather fetch failed", e)
                ToolExecutionResult(
                    status = ToolStatus.FAILED,
                    message = "Weather request failed: ${e.message}",
                    spokenResponse = "Weather service is currently unreachable."
                )
            }
        }
    }

    private fun parseWmoWeatherCode(code: Int): String {
        return when (code) {
            0 -> "Clear Sky"
            1, 2, 3 -> "Partly Cloudy"
            45, 48 -> "Foggy"
            51, 53, 55 -> "Drizzle"
            61, 63, 65 -> "Rainy"
            71, 73, 75 -> "Snowy"
            80, 81, 82 -> "Rain Showers"
            95, 96, 99 -> "Thunderstorm"
            else -> "Pleasant"
        }
    }
}
