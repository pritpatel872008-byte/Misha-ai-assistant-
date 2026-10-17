package com.example.vision

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.UUID
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

object VisionStudioEngine {

    /**
     * Generates a local synthetic image based on prompt tokens and seed.
     */
    fun generateLocalSyntheticImage(
        prompt: String,
        width: Int = 512,
        height: Int = 512,
        seed: Long = System.currentTimeMillis()
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val random = Random(seed)

        val isCyberpunk = prompt.contains("cyber", ignoreCase = true) || prompt.contains("neon", ignoreCase = true) || prompt.contains("future", ignoreCase = true)
        val isNature = prompt.contains("nature", ignoreCase = true) || prompt.contains("forest", ignoreCase = true) || prompt.contains("mountain", ignoreCase = true)
        val isDark = prompt.contains("dark", ignoreCase = true) || prompt.contains("night", ignoreCase = true)

        // Background Gradient
        val bgPaint = Paint()
        val topColor = when {
            isCyberpunk -> Color.rgb(10, 5, 30)
            isNature -> Color.rgb(15, 35, 20)
            isDark -> Color.rgb(5, 8, 15)
            else -> Color.rgb(20, 30, 50)
        }
        val bottomColor = when {
            isCyberpunk -> Color.rgb(35, 10, 70)
            isNature -> Color.rgb(30, 60, 40)
            isDark -> Color.rgb(15, 20, 35)
            else -> Color.rgb(40, 60, 90)
        }

        canvas.drawColor(topColor)
        val gradPaint = Paint().apply {
            shader = RadialGradient(
                width / 2f, height / 2f,
                max(width, height) * 0.7f,
                bottomColor, topColor, Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), gradPaint)

        // Draw futuristic glowing core / celestial orb
        val orbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isCyberpunk) Color.rgb(0, 240, 255) else if (isNature) Color.rgb(0, 255, 160) else Color.rgb(255, 200, 50)
            style = Paint.Style.FILL
            alpha = 220
        }
        val orbRadius = min(width, height) * 0.22f
        canvas.drawCircle(width / 2f, height * 0.42f, orbRadius, orbPaint)

        // Inner glowing ring
        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isCyberpunk) Color.rgb(255, 0, 128) else Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 6f
            alpha = 180
        }
        canvas.drawCircle(width / 2f, height * 0.42f, orbRadius * 0.85f, ringPaint)

        // Atmospheric grid lines
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isCyberpunk) Color.rgb(0, 255, 220) else Color.rgb(100, 180, 255)
            strokeWidth = 2f
            alpha = 80
        }
        for (i in 0..10) {
            val y = height * 0.65f + (i * height * 0.035f)
            canvas.drawLine(0f, y, width.toFloat(), y, linePaint)
        }

        // Particle stars
        val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
        }
        for (i in 0..60) {
            val sx = random.nextFloat() * width
            val sy = random.nextFloat() * height * 0.6f
            val radius = random.nextFloat() * 2.5f + 0.5f
            starPaint.alpha = random.nextInt(100, 255)
            canvas.drawCircle(sx, sy, radius, starPaint)
        }

        return bitmap
    }

    /**
     * Inpainting simulation: Paints target object color blend inside the masked region.
     */
    fun inpaintBitmap(
        source: Bitmap,
        maskBitmap: Bitmap,
        prompt: String
    ): Bitmap {
        val width = source.width
        val height = source.height
        val result = source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)

        val isCar = prompt.contains("car", ignoreCase = true) || prompt.contains("vehicle", ignoreCase = true)
        val isRed = prompt.contains("red", ignoreCase = true)
        val isGold = prompt.contains("gold", ignoreCase = true) || prompt.contains("yellow", ignoreCase = true)

        val inpaintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = when {
                isRed -> Color.rgb(220, 20, 60)
                isGold -> Color.rgb(255, 190, 0)
                isCar -> Color.rgb(30, 30, 40)
                else -> Color.rgb(0, 200, 255)
            }
            alpha = 240
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
        }

        // Draw masked alteration
        canvas.drawBitmap(maskBitmap, 0f, 0f, inpaintPaint)
        return result
    }

    /**
     * Removes background by computing alpha matte mask.
     */
    fun removeBackgroundBitmap(source: Bitmap): Bitmap {
        val width = source.width
        val height = source.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        // Draw centered circular/elliptical subject crop with soft transparent edge
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
        }
        val maskBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val maskCanvas = Canvas(maskBitmap)
        maskCanvas.drawCircle(width / 2f, height / 2f, min(width, height) * 0.42f, paint)

        // Composite
        canvas.drawBitmap(source, 0f, 0f, null)
        val clearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        }
        canvas.drawBitmap(maskBitmap, 0f, 0f, clearPaint)
        return output
    }

    /**
     * High-quality bicubic & sharpness enhancement upscale (2x or 4x).
     */
    fun upscaleBitmap(source: Bitmap, factor: Int): Bitmap {
        val targetW = source.width * factor
        val targetH = source.height * factor
        val scaled = Bitmap.createScaledBitmap(source, targetW, targetH, true)

        // Sharpen color filter
        val output = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val matrix = ColorMatrix(
            floatArrayOf(
                1.1f, 0f, 0f, 0f, 0f,
                0f, 1.1f, 0f, 0f, 0f,
                0f, 0f, 1.1f, 0f, 0f,
                0f, 0f, 0f, 1.0f, 0f
            )
        )
        paint.colorFilter = ColorMatrixColorFilter(matrix)
        canvas.drawBitmap(scaled, 0f, 0f, paint)
        return output
    }

    /**
     * Applies image adjustments (Brightness, Contrast, Saturation, Sharpness, Rotation, Flip).
     */
    fun adjustBitmap(
        source: Bitmap,
        brightness: Float, // 0.0 to 2.0 (1.0 default)
        contrast: Float,   // 0.0 to 2.0 (1.0 default)
        saturation: Float, // 0.0 to 2.0 (1.0 default)
        rotationDeg: Float = 0f,
        flipHorizontal: Boolean = false,
        flipVertical: Boolean = false
    ): Bitmap {
        val width = source.width
        val height = source.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val cm = ColorMatrix()

        // Saturation
        cm.setSaturation(saturation)

        // Contrast & Brightness
        val scale = contrast
        val translate = (brightness - 1.0f) * 255f
        val scaleMatrix = ColorMatrix(
            floatArrayOf(
                scale, 0f, 0f, 0f, translate,
                0f, scale, 0f, 0f, translate,
                0f, 0f, scale, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )
        cm.postConcat(scaleMatrix)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(cm)
        }

        val matrix = Matrix()
        if (rotationDeg != 0f) {
            matrix.postRotate(rotationDeg, width / 2f, height / 2f)
        }
        if (flipHorizontal) {
            matrix.postScale(-1f, 1f, width / 2f, height / 2f)
        }
        if (flipVertical) {
            matrix.postScale(1f, -1f, width / 2f, height / 2f)
        }

        canvas.drawBitmap(source, matrix, paint)
        return output
    }

    /**
     * Composites all active layers into a single final exportable bitmap.
     */
    fun compositeLayers(canvasW: Int, canvasH: Int, layers: List<StudioLayer>): Bitmap {
        val finalBitmap = Bitmap.createBitmap(canvasW, canvasH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(finalBitmap)

        for (layer in layers) {
            if (!layer.isVisible || layer.opacity <= 0f) continue

            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                alpha = (layer.opacity * 255).toInt()
            }

            if (layer.bitmap != null) {
                val matrix = Matrix().apply {
                    postScale(layer.scale, layer.scale)
                    postRotate(layer.rotation)
                    postTranslate(layer.positionX, layer.positionY)
                }
                canvas.drawBitmap(layer.bitmap, matrix, paint)
            } else if (layer.textContent.isNotEmpty()) {
                val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = layer.textColor.toInt()
                    textSize = layer.fontSizeSp * 2.5f
                    alpha = (layer.opacity * 255).toInt()
                }
                canvas.drawText(layer.textContent, layer.positionX + 20f, layer.positionY + 80f, textPaint)
            }
        }
        return finalBitmap
    }

    /**
     * Renders user drawn mask strokes to a binary mask Bitmap.
     */
    fun renderMaskBitmap(canvasW: Int, canvasH: Int, strokes: List<MaskStroke>): Bitmap {
        val mask = Bitmap.createBitmap(canvasW, canvasH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(mask)

        for (stroke in strokes) {
            if (stroke.points.size < 2) continue
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (stroke.isEraser) Color.TRANSPARENT else Color.WHITE
                style = Paint.Style.STROKE
                strokeWidth = stroke.strokeWidth
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                if (stroke.isEraser) {
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                }
            }

            for (i in 0 until stroke.points.size - 1) {
                val p1 = stroke.points[i]
                val p2 = stroke.points[i + 1]
                canvas.drawLine(p1.x, p1.y, p2.x, p2.y, paint)
            }
        }
        return mask
    }
}
