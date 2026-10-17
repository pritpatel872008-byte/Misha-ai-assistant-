package com.example.tools

import android.content.Context
import android.view.accessibility.AccessibilityNodeInfo

data class ScreenInspectionResult(
    val isAccessibilityEnabled: Boolean,
    val visibleTexts: List<String>,
    val clickableButtons: List<String>,
    val inputFields: List<String>,
    val summary: String
)

class ScreenUnderstandingEngine(private val context: Context) {

    fun inspectCurrentScreen(userQuestion: String = ""): ScreenInspectionResult {
        val serviceInstance = MishaAccessibilityService.instance
        if (serviceInstance == null) {
            return ScreenInspectionResult(
                isAccessibilityEnabled = false,
                visibleTexts = emptyList(),
                clickableButtons = emptyList(),
                inputFields = emptyList(),
                summary = "Accessibility Service is not enabled. Please enable 'Misha AI Automation Service' in Android Accessibility Settings to inspect on-screen content."
            )
        }

        val rootNode = serviceInstance.rootInActiveWindow
        if (rootNode == null) {
            return ScreenInspectionResult(
                isAccessibilityEnabled = true,
                visibleTexts = emptyList(),
                clickableButtons = emptyList(),
                inputFields = emptyList(),
                summary = "Active window has no readable accessibility elements."
            )
        }

        val texts = mutableListOf<String>()
        val buttons = mutableListOf<String>()
        val inputs = mutableListOf<String>()

        traverseNode(rootNode, texts, buttons, inputs)

        val summaryBuilder = StringBuilder()
        summaryBuilder.append("Screen Analysis:\n")
        if (buttons.isNotEmpty()) {
            summaryBuilder.append("• Buttons: ").append(buttons.take(5).joinToString(", ")).append("\n")
        }
        if (inputs.isNotEmpty()) {
            summaryBuilder.append("• Input Fields: ").append(inputs.take(3).joinToString(", ")).append("\n")
        }
        if (texts.isNotEmpty()) {
            summaryBuilder.append("• Visible Text Snippets: ").append(texts.take(6).joinToString(" | "))
        }

        return ScreenInspectionResult(
            isAccessibilityEnabled = true,
            visibleTexts = texts,
            clickableButtons = buttons,
            inputFields = inputs,
            summary = summaryBuilder.toString()
        )
    }

    private fun traverseNode(
        node: AccessibilityNodeInfo,
        texts: MutableList<String>,
        buttons: MutableList<String>,
        inputs: MutableList<String>
    ) {
        val text = node.text?.toString()?.trim()
        val desc = node.contentDescription?.toString()?.trim()
        val label = if (!text.isNullOrBlank()) text else desc

        if (!label.isNullOrBlank()) {
            if (node.isClickable || node.className?.contains("Button", ignoreCase = true) == true) {
                buttons.add(label)
            } else if (node.isEditable || node.className?.contains("EditText", ignoreCase = true) == true) {
                inputs.add(label)
            } else {
                texts.add(label)
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                traverseNode(child, texts, buttons, inputs)
            }
        }
    }
}
