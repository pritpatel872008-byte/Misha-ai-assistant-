package com.example.vision

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.compose.ui.geometry.Offset
import java.util.UUID

enum class VisionTool {
    SELECT,
    BRUSH,
    ERASER,
    MASK_BRUSH,
    MASK_ERASER,
    TEXT,
    CROP,
    ADJUST
}

enum class StudioTab {
    GENERATE,
    EDIT_INPAINT,
    BACKGROUND,
    UPSCALE,
    ADJUSTMENTS,
    LAYERS,
    PROJECTS,
    MODELS
}

enum class LayerType {
    IMAGE,
    TEXT,
    SHAPE,
    AI_GENERATED,
    MASK
}

data class StudioLayer(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: LayerType = LayerType.IMAGE,
    val isVisible: Boolean = true,
    val opacity: Float = 1.0f,
    val positionX: Float = 0f,
    val positionY: Float = 0f,
    val scale: Float = 1.0f,
    val rotation: Float = 0f,
    val bitmap: Bitmap? = null,
    val textContent: String = "",
    val textColor: Long = 0xFFFFFFFF,
    val fontSizeSp: Float = 22f
)

data class MaskStroke(
    val points: List<Offset>,
    val strokeWidth: Float,
    val isEraser: Boolean = false
)

data class StudioVersion(
    val versionId: Int,
    val title: String,
    val operation: String,
    val prompt: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val thumbnailBitmap: Bitmap? = null
)

data class HardwareSpecs(
    val cpuCores: Int = 4,
    val ramGb: Float = 8.0f,
    val gpuName: String = "Adreno / Local GPU",
    val vramGb: Float = 4.0f,
    val recommendedPreset: String = "BALANCED",
    val pythonConnected: Boolean = true
)

data class LocalModelItem(
    val id: String,
    val name: String,
    val type: String,
    val version: String,
    val isInstalled: Boolean,
    val isLoaded: Boolean,
    val isCompatible: Boolean,
    val vramRequirementGb: Float,
    val description: String
)

data class GenerationJobUi(
    val jobId: String,
    val state: String, // QUEUED, LOADING, RUNNING, COMPLETED, FAILED
    val progress: Float,
    val message: String,
    val operation: String,
    val resultBitmap: Bitmap? = null,
    val durationSec: Float = 0f
)
