package com.example.routines

import com.example.ai.IntentConstants
import com.example.ai.MishaParsedIntent
import com.example.ai.ToolExecutionResult
import com.example.ai.ToolStatus
import com.example.data.local.entities.RoutineEntity
import com.example.data.repository.MishaRepository
import com.example.tools.DeviceController
import com.example.tools.WeatherHelper
import org.json.JSONArray
import org.json.JSONObject

data class RoutineStep(
    val title: String,
    val intent: String,
    val args: Map<String, String> = emptyMap()
)

class RoutineManager(
    private val repository: MishaRepository,
    private val deviceController: DeviceController,
    private val weatherHelper: WeatherHelper
) {

    suspend fun initializeDefaultRoutines() {
        val goodMorningSteps = listOf(
            RoutineStep("Read Battery", IntentConstants.BATTERY_STATUS),
            RoutineStep("Ahmedabad Weather", IntentConstants.WEATHER, mapOf("location" to "Ahmedabad")),
            RoutineStep("Flashlight Off", IntentConstants.FLASHLIGHT_OFF)
        )

        val studyModeSteps = listOf(
            RoutineStep("Volume Down", IntentConstants.VOLUME_DOWN),
            RoutineStep("Turn off Flashlight", IntentConstants.FLASHLIGHT_OFF)
        )

        val nightModeSteps = listOf(
            RoutineStep("Turn off Flashlight", IntentConstants.FLASHLIGHT_OFF),
            RoutineStep("Volume Down", IntentConstants.VOLUME_DOWN)
        )

        val routines = listOf(
            RoutineEntity(
                name = "Good Morning",
                triggerPhrase = "good morning",
                actionsJson = serializeSteps(goodMorningSteps),
                scheduledTime = "07:00 AM",
                iconName = "wb_sunny"
            ),
            RoutineEntity(
                name = "Study Mode",
                triggerPhrase = "study mode",
                actionsJson = serializeSteps(studyModeSteps),
                iconName = "menu_book"
            ),
            RoutineEntity(
                name = "Night Mode",
                triggerPhrase = "good night",
                actionsJson = serializeSteps(nightModeSteps),
                scheduledTime = "11:00 PM",
                iconName = "nightlight_round"
            )
        )

        for (r in routines) {
            repository.saveRoutine(r)
        }
    }

    suspend fun executeRoutine(routine: RoutineEntity): List<ToolExecutionResult> {
        val steps = parseSteps(routine.actionsJson)
        val results = mutableListOf<ToolExecutionResult>()

        for (step in steps) {
            val res = when (step.intent) {
                IntentConstants.BATTERY_STATUS -> deviceController.getBatteryInfo()
                IntentConstants.FLASHLIGHT_ON -> deviceController.toggleFlashlight(true)
                IntentConstants.FLASHLIGHT_OFF -> deviceController.toggleFlashlight(false)
                IntentConstants.VOLUME_DOWN -> deviceController.adjustVolume(false)
                IntentConstants.VOLUME_UP -> deviceController.adjustVolume(true)
                IntentConstants.WEATHER -> weatherHelper.getWeather(step.args["location"] ?: "Ahmedabad")
                else -> ToolExecutionResult(ToolStatus.SUCCESS, "Executed step: ${step.title}", step.title)
            }
            results.add(res)
            repository.addAction(step.intent, JSONObject(step.args).toString(), res.status.name, res.message)
        }

        return results
    }

    private fun serializeSteps(steps: List<RoutineStep>): String {
        val array = JSONArray()
        for (s in steps) {
            val obj = JSONObject().apply {
                put("title", s.title)
                put("intent", s.intent)
                put("args", JSONObject(s.args))
            }
            array.put(obj)
        }
        return array.toString()
    }

    fun parseSteps(json: String): List<RoutineStep> {
        return try {
            val list = mutableListOf<RoutineStep>()
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val title = obj.getString("title")
                val intent = obj.getString("intent")
                val argsObj = obj.optJSONObject("args")
                val args = mutableMapOf<String, String>()
                argsObj?.keys()?.forEach { k ->
                    args[k] = argsObj.getString(k)
                }
                list.add(RoutineStep(title, intent, args))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }
}
