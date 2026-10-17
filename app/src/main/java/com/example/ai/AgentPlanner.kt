package com.example.ai

import com.example.BuildConfig
import com.example.data.local.entities.AgentPlanEntity
import com.example.data.repository.MishaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class AgentPlanStep(
    val stepNumber: Int,
    val title: String,
    val description: String,
    val toolIntent: String,
    val arguments: Map<String, String>,
    val requiresConfirmation: Boolean = false,
    val isHighImpact: Boolean = false,
    var status: StepStatus = StepStatus.PENDING,
    var executionOutcome: String = ""
)

enum class StepStatus {
    PENDING,
    WAITING_CONFIRMATION,
    EXECUTING,
    VERIFIED_SUCCESS,
    FAILED,
    SKIPPED
}

data class PlanGenerationResult(
    val requestId: String,
    val goal: String,
    val summary: String,
    val steps: List<AgentPlanStep>,
    val requiresOverallConfirmation: Boolean = true
)

class AgentPlanner(
    private val repository: MishaRepository
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val allowlistedIntents = setOf(
        IntentConstants.SET_REMINDER,
        IntentConstants.SET_ALARM,
        IntentConstants.FLASHLIGHT_ON,
        IntentConstants.FLASHLIGHT_OFF,
        IntentConstants.SET_VOLUME,
        IntentConstants.OPEN_APP,
        IntentConstants.WEATHER,
        IntentConstants.WEB_SEARCH,
        IntentConstants.KNOWLEDGE_QUERY,
        IntentConstants.SEND_SMS,
        IntentConstants.SEND_WHATSAPP,
        IntentConstants.MAKE_CALL
    )

    suspend fun createPlan(goalPrompt: String, requestId: String = MishaEventBus.newRequestId()): PlanGenerationResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val prompt = """
                    You are MISHA AGENT PLANNER. Break down the user's complex Android task into discrete, structured, executable steps.
                    User Request: "$goalPrompt"
                    
                    Allowlisted Tool Intents:
                    - SET_REMINDER (args: title, time)
                    - SET_ALARM (args: time)
                    - FLASHLIGHT_ON / FLASHLIGHT_OFF
                    - SET_VOLUME (args: level)
                    - OPEN_APP (args: app_name)
                    - WEATHER (args: location)
                    - WEB_SEARCH (args: query)
                    - KNOWLEDGE_QUERY (args: query)
                    - SEND_SMS (args: recipient, message) - HIGH IMPACT
                    - SEND_WHATSAPP (args: recipient, message) - HIGH IMPACT
                    - MAKE_CALL (args: recipient) - HIGH IMPACT
                    
                    Return STRICT JSON conforming to:
                    {
                      "summary": "Brief explanation of the plan",
                      "steps": [
                        {
                          "stepNumber": 1,
                          "title": "Step title",
                          "description": "Details",
                          "toolIntent": "INTENT_NAME",
                          "arguments": {"key": "val"},
                          "requiresConfirmation": boolean,
                          "isHighImpact": boolean
                        }
                      ]
                    }
                """.trimIndent()

                val jsonBody = JSONObject().apply {
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "user")
                            put("parts", JSONArray().apply {
                                put(JSONObject().put("text", prompt))
                            })
                        })
                    })
                    put("generationConfig", JSONObject().apply {
                        put("temperature", 0.2)
                        put("responseMimeType", "application/json")
                    })
                }

                val request = Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
                    .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = client.newCall(request).execute()
                val resString = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val parsed = JSONObject(resString)
                    val candidateText = parsed.optJSONArray("candidates")?.optJSONObject(0)
                        ?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.optString("text") ?: ""
                    val cleanText = candidateText.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                    val planObj = JSONObject(cleanText)
                    val summary = planObj.optString("summary", "Automated multi-step plan")
                    val stepsArray = planObj.optJSONArray("steps")
                    val stepsList = mutableListOf<AgentPlanStep>()

                    if (stepsArray != null) {
                        for (i in 0 until stepsArray.length()) {
                            val stepJson = stepsArray.getJSONObject(i)
                            val intent = stepJson.optString("toolIntent")
                            if (intent in allowlistedIntents) {
                                val argsMap = mutableMapOf<String, String>()
                                val argsObj = stepJson.optJSONObject("arguments")
                                argsObj?.keys()?.forEach { k ->
                                    argsMap[k] = argsObj.getString(k)
                                }
                                val isHigh = stepJson.optBoolean("isHighImpact", false) || intent in setOf("SEND_SMS", "SEND_WHATSAPP", "MAKE_CALL")
                                stepsList.add(
                                    AgentPlanStep(
                                        stepNumber = stepJson.optInt("stepNumber", i + 1),
                                        title = stepJson.optString("title", "Step ${i + 1}"),
                                        description = stepJson.optString("description", ""),
                                        toolIntent = intent,
                                        arguments = argsMap,
                                        requiresConfirmation = stepJson.optBoolean("requiresConfirmation", isHigh),
                                        isHighImpact = isHigh
                                    )
                                )
                            }
                        }
                    }

                    if (stepsList.isNotEmpty()) {
                        val entity = AgentPlanEntity(
                            requestId = requestId,
                            goalPrompt = goalPrompt,
                            planSummary = summary,
                            stepsJson = cleanText,
                            status = "WAITING_CONFIRMATION"
                        )
                        repository.saveAgentPlan(entity)
                        return@withContext PlanGenerationResult(
                            requestId = requestId,
                            goal = goalPrompt,
                            summary = summary,
                            steps = stepsList,
                            requiresOverallConfirmation = true
                        )
                    }
                }
            } catch (e: Exception) {
                // fallback to rule-based planner below
            }
        }

        // Rule-based Fallback Planner for Offline / No API Key
        val defaultPlan = generateOfflinePlan(goalPrompt, requestId)
        val entity = AgentPlanEntity(
            requestId = requestId,
            goalPrompt = goalPrompt,
            planSummary = defaultPlan.summary,
            stepsJson = "[]",
            status = "WAITING_CONFIRMATION"
        )
        repository.saveAgentPlan(entity)
        defaultPlan
    }

    private fun generateOfflinePlan(goalPrompt: String, requestId: String): PlanGenerationResult {
        val q = goalPrompt.lowercase()
        val steps = mutableListOf<AgentPlanStep>()
        var summary = "Plan for: $goalPrompt"

        if (q.contains("study") || q.contains("exam") || q.contains("padhai")) {
            summary = "Structured 3-Hour Exam Study & Reminder Plan"
            steps.add(
                AgentPlanStep(
                    stepNumber = 1,
                    title = "Set 50-Min Session 1 Reminder",
                    description = "Focus on Core Concepts & Formulas",
                    toolIntent = IntentConstants.SET_REMINDER,
                    arguments = mapOf("title" to "Exam Study: Module 1", "time" to "in 50 minutes")
                )
            )
            steps.add(
                AgentPlanStep(
                    stepNumber = 2,
                    title = "Set 10-Min Break Reminder",
                    description = "Hydration and Rest",
                    toolIntent = IntentConstants.SET_REMINDER,
                    arguments = mapOf("title" to "Short Break Time", "time" to "in 60 minutes")
                )
            )
            steps.add(
                AgentPlanStep(
                    stepNumber = 3,
                    title = "Set 50-Min Session 2 Reminder",
                    description = "Practice Questions & Past Papers",
                    toolIntent = IntentConstants.SET_REMINDER,
                    arguments = mapOf("title" to "Exam Study: Practice Sets", "time" to "in 120 minutes")
                )
            )
            steps.add(
                AgentPlanStep(
                    stepNumber = 4,
                    title = "Adjust Media Volume for Focus",
                    description = "Mute or lower distracting audio",
                    toolIntent = IntentConstants.SET_VOLUME,
                    arguments = mapOf("level" to "2")
                )
            )
        } else {
            summary = "Multi-step automated execution plan"
            steps.add(
                AgentPlanStep(
                    stepNumber = 1,
                    title = "Acknowledge Goal",
                    description = goalPrompt,
                    toolIntent = IntentConstants.WEB_SEARCH,
                    arguments = mapOf("query" to goalPrompt)
                )
            )
            steps.add(
                AgentPlanStep(
                    stepNumber = 2,
                    title = "Schedule Follow-up Reminder",
                    description = "Verify progress on $goalPrompt",
                    toolIntent = IntentConstants.SET_REMINDER,
                    arguments = mapOf("title" to goalPrompt, "time" to "in 1 hour")
                )
            )
        }

        return PlanGenerationResult(
            requestId = requestId,
            goal = goalPrompt,
            summary = summary,
            steps = steps,
            requiresOverallConfirmation = true
        )
    }
}
