package com.example.ai

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiIntentEngine {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val adapter = moshi.adapter(MishaParsedIntent::class.java)

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val systemPrompt = """
        You are MISHA, a futuristic, highly intelligent, concise personal AI assistant for Android.
        Your job is to parse the user's natural spoken input (English, Hindi, or Hinglish) into a structured JSON object.
        
        Supported Intent Strings:
        - GENERAL_AI (general conversation, knowledge, greetings)
        - TRIGGER_SOS (emergency help, requires SOS dispatch to emergency contacts)
        - TEST_SOS (send a test emergency SOS message to contacts)
        - CANCEL_SOS (abort or cancel active SOS countdown/mode)
        - CONFIRM_SOS (confirm pending emergency SOS dispatch)
        - WEB_SEARCH (current events, specific online lookups)
        - WEATHER (weather queries, requires argument 'location')
        - MAKE_CALL (make a phone call, requires argument 'recipient' or 'number')
        - ANSWER_CALL, REJECT_CALL, END_CALL
        - SEND_SMS (requires argument 'recipient' and 'message')
        - SEND_WHATSAPP (requires argument 'recipient' and 'message')
        - OPEN_APP (requires argument 'app_name')
        - OPEN_SETTINGS
        - FLASHLIGHT_ON, FLASHLIGHT_OFF
        - LOCK_DEVICE
        - VOLUME_UP, VOLUME_DOWN, SET_VOLUME (argument 'level')
        - SET_BRIGHTNESS (argument 'level')
        - WIFI_SETTINGS, BLUETOOTH_SETTINGS
        - BATTERY_STATUS, DEVICE_INFO
        - SET_ALARM (argument 'time')
        - SET_REMINDER (argument 'title' and 'time')
        - CAMERA, MUSIC, STOP, CANCEL, HELP
        
        Rules:
        1. Always respond in STRICT JSON conforming to:
        {
          "intent": "INTENT_NAME",
          "arguments": {"key": "val"},
          "spoken_response": "Short natural response (Hindi/Hinglish/English depending on user)",
          "requires_confirmation": boolean,
          "confirmation_prompt": null or "prompt if confirmation needed"
        }
        2. Keep spoken_response short, natural, confident, and direct. (e.g., "Turning on flashlight.", "Flashlight on kar di.", "Battery 64 percent hai.").
        3. For sensitive actions like calls, SMS, WhatsApp, set requires_confirmation=true if recipient or message is ambiguous.
        4. Never include markdown backticks like ```json in the output. Just raw JSON.
    """.trimIndent()

    suspend fun parseUserCommand(userQuery: String, contextInfo: String = ""): MishaParsedIntent {
        return withContext(Dispatchers.IO) {
            // First check if user query has direct unambiguous offline intent
            val offlineIntent = parseOfflineFastRule(userQuery)
            if (offlineIntent != null) {
                return@withContext offlineIntent
            }

            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
                // Fallback to local rule engine if no API key set
                return@withContext parseOfflineFallback(userQuery)
            }

            try {
                val fullUserPrompt = if (contextInfo.isNotBlank()) {
                    "Device Context: $contextInfo\nUser: $userQuery"
                } else {
                    userQuery
                }

                val jsonBody = JSONObject().apply {
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "user")
                            put("parts", JSONArray().apply {
                                put(JSONObject().put("text", fullUserPrompt))
                            })
                        })
                    })
                    put("systemInstruction", JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", systemPrompt))
                        })
                    })
                    put("generationConfig", JSONObject().apply {
                        put("temperature", 0.2)
                        put("responseMimeType", "application/json")
                    })
                }

                val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
                // Use the modern gemini-3.5-flash as per gemini-api skill instructions
                val request = Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                val responseString = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    Log.e("GeminiIntentEngine", "Gemini HTTP error ${response.code}: $responseString")
                    return@withContext parseOfflineFallback(userQuery)
                }

                val parsedJson = JSONObject(responseString)
                val candidates = parsedJson.optJSONArray("candidates")
                val text = candidates?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.optJSONObject(0)
                    ?.optString("text") ?: ""

                val cleanJson = text.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                adapter.fromJson(cleanJson) ?: parseOfflineFallback(userQuery)
            } catch (e: Exception) {
                Log.e("GeminiIntentEngine", "Gemini call exception", e)
                parseOfflineFallback(userQuery)
            }
        }
    }

    private fun parseOfflineFastRule(query: String): MishaParsedIntent? {
        val q = query.lowercase().trim()

        // Emergency SOS fast rules
        if (q == "test sos" || q.contains("send test sos") || q.contains("test emergency") || q.contains("emergency test")) {
            return MishaParsedIntent(
                intent = IntentConstants.TEST_SOS,
                spoken_response = "Preparing to send a test emergency SOS message to your contacts."
            )
        }

        if (q == "cancel sos" || q == "abort sos" || q.contains("sos cancel") || q.contains("sos ruko") || q.contains("emergency cancel")) {
            return MishaParsedIntent(
                intent = IntentConstants.CANCEL_SOS,
                spoken_response = "Emergency SOS cancelled."
            )
        }

        if (q == "sos" || q == "emergency" || q == "help" || q.contains("sos bhej") || q.contains("emergency help") ||
            q.contains("help me") || q.contains("bachao") || q.contains("misha help") || q.contains("misha emergency") || q.contains("misha sos")) {
            return MishaParsedIntent(
                intent = IntentConstants.TRIGGER_SOS,
                spoken_response = "Emergency mode activated. Do you want me to send an SOS?",
                requires_confirmation = true,
                confirmation_prompt = "Emergency SOS bhejne wala hoon. Confirm?"
            )
        }

        if (q == "confirm sos" || q == "yes send sos" || q == "send sos now" || q == "ha bhej de") {
            return MishaParsedIntent(
                intent = IntentConstants.CONFIRM_SOS,
                spoken_response = "Sending SOS."
            )
        }

        // Flashlight fast rule
        if (q.contains("flashlight on") || q.contains("torch on") || q.contains("light on") || q.contains("torch chalu")) {
            return MishaParsedIntent(
                intent = IntentConstants.FLASHLIGHT_ON,
                spoken_response = "Flashlight on kar di."
            )
        }
        if (q.contains("flashlight off") || q.contains("torch off") || q.contains("light off") || q.contains("torch band")) {
            return MishaParsedIntent(
                intent = IntentConstants.FLASHLIGHT_OFF,
                spoken_response = "Flashlight off."
            )
        }

        // Lock fast rule
        if (q == "lock" || q.contains("lock device") || q.contains("phone lock kar") || q.contains("lock screen")) {
            return MishaParsedIntent(
                intent = IntentConstants.LOCK_DEVICE,
                spoken_response = "Locking device."
            )
        }

        // Volume controls
        if (q.contains("volume up") || q.contains("awaz badhao") || q.contains("sound up")) {
            return MishaParsedIntent(
                intent = IntentConstants.VOLUME_UP,
                spoken_response = "Volume increased."
            )
        }
        if (q.contains("volume down") || q.contains("awaz kam karo") || q.contains("sound down")) {
            return MishaParsedIntent(
                intent = IntentConstants.VOLUME_DOWN,
                spoken_response = "Volume decreased."
            )
        }

        // Stop / Cancel
        if (q == "stop" || q == "cancel" || q == "ruko" || q == "chup") {
            return MishaParsedIntent(
                intent = IntentConstants.STOP,
                spoken_response = "Stopped."
            )
        }

        return null
    }

    private fun parseOfflineFallback(query: String): MishaParsedIntent {
        val q = query.lowercase().trim()

        return when {
            q.contains("test sos") || q.contains("test emergency") -> {
                MishaParsedIntent(
                    intent = IntentConstants.TEST_SOS,
                    spoken_response = "Preparing test SOS message."
                )
            }
            q.contains("sos") || q.contains("emergency") || q.contains("bachao") -> {
                MishaParsedIntent(
                    intent = IntentConstants.TRIGGER_SOS,
                    spoken_response = "Emergency mode activated. Do you want me to send an SOS?",
                    requires_confirmation = true,
                    confirmation_prompt = "Emergency SOS bhejne wala hoon. Confirm?"
                )
            }
            q.contains("battery") -> {
                MishaParsedIntent(
                    intent = IntentConstants.BATTERY_STATUS,
                    spoken_response = "Checking battery status."
                )
            }
            q.contains("weather") || q.contains("mausam") -> {
                val city = if (q.contains("in ") || q.contains("ka ")) {
                    q.substringAfter("in ").substringAfter("ka ").substringBefore(" ").trim()
                } else "Ahmedabad"
                MishaParsedIntent(
                    intent = IntentConstants.WEATHER,
                    arguments = mapOf("location" to city),
                    spoken_response = "Checking weather for $city."
                )
            }
            q.contains("call") || q.contains("phone laga") -> {
                val name = q.substringAfter("call ").substringAfter("ko ").substringBefore(" ").trim()
                MishaParsedIntent(
                    intent = IntentConstants.MAKE_CALL,
                    arguments = mapOf("recipient" to name),
                    spoken_response = "Initiating call to $name.",
                    requires_confirmation = true,
                    confirmation_prompt = "Do you want to call $name?"
                )
            }
            q.contains("whatsapp") -> {
                val recipient = q.substringAfter("to ").substringAfter("ko ").substringBefore(" ").trim()
                val message = if (q.contains("saying ") || q.contains("bhej ")) {
                    q.substringAfter("saying ").substringAfter("bhej ").trim()
                } else "Hello"
                MishaParsedIntent(
                    intent = IntentConstants.SEND_WHATSAPP,
                    arguments = mapOf("recipient" to recipient, "message" to message),
                    spoken_response = "Preparing WhatsApp message for $recipient.",
                    requires_confirmation = true,
                    confirmation_prompt = "Send \"$message\" to $recipient on WhatsApp?"
                )
            }
            q.contains("sms") || q.contains("message") -> {
                val recipient = q.substringAfter("to ").substringAfter("ko ").substringBefore(" ").trim()
                val msg = q.substringAfter("saying ").substringAfter("likh ").trim()
                MishaParsedIntent(
                    intent = IntentConstants.SEND_SMS,
                    arguments = mapOf("recipient" to recipient, "message" to msg),
                    spoken_response = "Preparing SMS to $recipient.",
                    requires_confirmation = true,
                    confirmation_prompt = "Send SMS to $recipient?"
                )
            }
            q.contains("open ") || q.contains("khol") -> {
                val app = q.substringAfter("open ").substringBefore(" khol").trim()
                MishaParsedIntent(
                    intent = IntentConstants.OPEN_APP,
                    arguments = mapOf("app_name" to app),
                    spoken_response = "Opening $app."
                )
            }
            q.contains("wifi") -> {
                MishaParsedIntent(
                    intent = IntentConstants.WIFI_SETTINGS,
                    spoken_response = "Opening Wi-Fi settings."
                )
            }
            q.contains("bluetooth") -> {
                MishaParsedIntent(
                    intent = IntentConstants.BLUETOOTH_SETTINGS,
                    spoken_response = "Opening Bluetooth settings."
                )
            }
            q.contains("search") || q.contains("who is") || q.contains("what is") -> {
                MishaParsedIntent(
                    intent = IntentConstants.WEB_SEARCH,
                    arguments = mapOf("query" to query),
                    spoken_response = "Searching for $query."
                )
            }
            else -> {
                MishaParsedIntent(
                    intent = IntentConstants.GENERAL_AI,
                    arguments = mapOf("query" to query),
                    spoken_response = "I am Misha AI. How can I assist you with your device, calls, messages, or information?"
                )
            }
        }
    }
}
