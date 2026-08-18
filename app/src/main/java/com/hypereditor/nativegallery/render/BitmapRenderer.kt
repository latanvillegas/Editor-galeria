package com.hypereditor.nativegallery.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import com.hypereditor.nativegallery.domain.model.EditOperation
import com.hypereditor.nativegallery.domain.model.EditorDocument
import com.hypereditor.nativegallery.domain.model.LayerModel
import java.util.Random

object BitmapRenderer {

    fun renderDocument(
        baseBitmap: Bitmap,
        document: EditorDocument,
        isPreview: Boolean = false,
        maxDimension: Int = 2048
    ): Bitmap {
        val srcWidth = baseBitmap.width
        val srcHeight = baseBitmap.height

        var workingBitmap = if (isPreview && (srcWidth > maxDimension || srcHeight > maxDimension)) {
            val scale = maxDimension.toFloat() / maxOf(srcWidth, srcHeight)
            Bitmap.createScaledBitmap(
                baseBitmap,
                (srcWidth * scale).toInt().coerceAtLeast(1),
                (srcHeight * scale).toInt().coerceAtLeast(1),
                true
            )
        } else {
            baseBitmap.copy(Bitmap.Config.ARGB_8888, true)
        }

        workingBitmap = applyColorAdjustments(workingBitmap, document.adjustments, document.appliedFilter)

        if (document.adjustments.vignette > 0f || document.adjustments.grain > 0f) {
            workingBitmap = applyVignetteAndGrain(workingBitmap, document.adjustments.vignette, document.adjustments.grain)
        }

        workingBitmap = applyTransformations(workingBitmap, document.cropTransform)

        val finalCanvas = Canvas(workingBitmap)
        renderLayers(finalCanvas, document.layers)
        renderBrushes(finalCanvas, document.brushStrokes)
        renderTexts(finalCanvas, document.textOverlays)
        renderCloneStamps(finalCanvas, document.cloneStamps, workingBitmap)

        return workingBitmap
    }

    private fun applyColorAdjustments(
        source: Bitmap,
        adj: EditOperation.Adjustments,
        filter: EditOperation.ColorFilter?
    ): Bitmap {
        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        val combinedMatrix = ColorMatrix()

        val brightnessShift = (adj.brightness * 255f) + (adj.exposure * 60f)
        val brightnessMatrix = ColorMatrix(
            floatArrayOf(
                1f, 0f, 0f, 0f, brightnessShift,
                0f, 1f, 0f, 0f, brightnessShift,
                0f, 0f, 1f, 0f, brightnessShift,
                0f, 0f, 0f, 1f, 0f
            )
        )
        combinedMatrix.postConcat(brightnessMatrix)

        val c = adj.contrast
        val contrastTranslate = (-0.5f * c + 0.5f) * 255f
        val contrastMatrix = ColorMatrix(
            floatArrayOf(
                c, 0f, 0f, 0f, contrastTranslate,
                0f, c, 0f, 0f, contrastTranslate,
                0f, 0f, c, 0f, contrastTranslate,
                0f, 0f, 0f, 1f, 0f
            )
        )
        combinedMatrix.postConcat(contrastMatrix)

        val satMatrix = ColorMatrix()
        satMatrix.setSaturation(adj.saturation)
        combinedMatrix.postConcat(satMatrix)

        val tempR = if (adj.temperature > 0) adj.temperature * 30f else 0f
        val tempB = if (adj.temperature < 0) -adj.temperature * 30f else 0f
        val tintG = if (adj.tint < 0) -adj.tint * 25f else 0f
        val tintM = if (adj.tint > 0) adj.tint * 25f else 0f
        val tempTintMatrix = ColorMatrix(
            floatArrayOf(
                1f, 0f, 0f, 0f, tempR + tintM,
                0f, 1f, 0f, 0f, tintG,
                0f, 0f, 1f, 0f, tempB + tintM,
                0f, 0f, 0f, 1f, 0f
            )
        )
        combinedMatrix.postConcat(tempTintMatrix)

        if (filter != null) {
            combinedMatrix.postConcat(getPresetColorMatrix(filter.filterName, filter.intensity))
        }

        paint.colorFilter = ColorMatrixColorFilter(combinedMatrix)
        canvas.drawBitmap(source, 0f, 0f, paint)
        return result
    }

    private fun getPresetColorMatrix(name: String, intensity: Float): ColorMatrix {
        val mat = ColorMatrix()
        when (name.uppercase()) {
            "BW" -> mat.setSaturation(1f - intensity)
            "SEPIA" -> {
                val sepia = ColorMatrix(
                    floatArrayOf(
                        0.393f + 0.607f * (1 - intensity), 0.769f - 0.769f * (1 - intensity), 0.189f - 0.189f * (1 - intensity), 0f, 0f,
                        0.349f - 0.349f * (1 - intensity), 0.686f + 0.314f * (1 - intensity), 0.168f - 0.168f * (1 - intensity), 0f, 0f,
                        0.272f - 0.272f * (1 - intensity), 0.534f - 0.534f * (1 - intensity), 0.131f + 0.869f * (1 - intensity), 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                mat.postConcat(sepia)
            }
            "VIVID" -> mat.setSaturation(1f + (0.8f * intensity))
            "COLD" -> {
                val cold = ColorMatrix(
                    floatArrayOf(
                        1f, 0f, 0f, 0f, -10f * intensity,
                        0f, 1f, 0f, 0f, 5f * intensity,
                        0f, 0f, 1f, 0f, 40f * intensity,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                mat.postConcat(cold)
            }
            "WARM" -> {
                val warm = ColorMatrix(
                    floatArrayOf(
                        1f, 0f, 0f, 0f, 35f * intensity,
                        0f, 1f, 0f, 0f, 15f * intensity,
                        0f, 0f, 1f, 0f, -20f * intensity,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                mat.postConcat(warm)
            }
        }
        return mat
    }

    private fun applyVignetteAndGrain(source: Bitmap, vignette: Float, grain: Float): Bitmap {
        val canvas = Canvas(source)
        val width = source.width.toFloat()
        val height = source.height.toFloat()

        if (vignette > 0f) {
            val radius = maxOf(width, height) * 0.75f
            val colors = intArrayOf(Color.TRANSPARENT, Color.argb((vignette * 220).toInt(), 0, 0, 0))
            val stops = floatArrayOf(0.4f, 1.0f)
            val shader = RadialGradient(width / 2f, height / 2f, radius, colors, stops, Shader.TileMode.CLAMP)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.shader = shader }
            canvas.drawRect(0f, 0f, width, height, paint)
        }

        if (grain > 0f) {
            val grainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                alpha = (grain * 60).toInt()
                style = Paint.Style.FILL
            }
            val rand = Random(42)
            val dotCount = (width * height * 0.008f * grain).toInt()
            for (i in 0 until dotCount) {
                val gx = rand.nextFloat() * width
                val gy = rand.nextFloat() * height
                canvas.drawCircle(gx, gy, rand.nextFloat() * 1.5f, grainPaint)
            }
        }
        return source
    }

    private fun applyTransformations(source: Bitmap, crop: EditOperation.CropTransform): Bitmap {
        val srcW = source.width
        val srcH = source.height

        val cropL = (crop.cropLeftNorm * srcW).toInt().coerceIn(0, srcW - 1)
        val cropT = (crop.cropTopNorm * srcH).toInt().coerceIn(0, srcH - 1)
        val cropR = (crop.cropRightNorm * srcW).toInt().coerceIn(cropL + 1, srcW)
        val cropB = (crop.cropBottomNorm * srcH).toInt().coerceIn(cropT + 1, srcH)
        val croppedBitmap = Bitmap.createBitmap(source, cropL, cropT, cropR - cropL, cropB - cropT)

        val matrix = Matrix()
        val totalRotation = crop.rotation90Degrees.toFloat() + crop.fineStraightenAngle
        matrix.postRotate(totalRotation, (croppedBitmap.width / 2f), (croppedBitmap.height / 2f))

        val scaleX = if (crop.flipHorizontal) -1f else 1f
        val scaleY = if (crop.flipVertical) -1f else 1f
        matrix.postScale(scaleX, scaleY, (croppedBitmap.width / 2f), (croppedBitmap.height / 2f))

        if (crop.perspectiveHorizontal != 0f || crop.perspectiveVertical != 0f) {
            matrix.postSkew(crop.perspectiveHorizontal * 0.2f, crop.perspectiveVertical * 0.2f)
        }

        return Bitmap.createBitmap(
            croppedBitmap, 0, 0, croppedBitmap.width, croppedBitmap.height, matrix, true
        )
    }

    private fun renderLayers(canvas: Canvas, layers: List<LayerModel>) {
        for (layer in layers) {
            if (!layer.isVisible || layer.bitmap == null) continue
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                alpha = (layer.opacity * 255).toInt()
                xfermode = BlendModeMapper.toPorterDuffXfermode(layer.blendMode)
            }
            val matrix = Matrix().apply {
                postScale(layer.scale, layer.scale)
                postRotate(layer.rotationDegrees)
                postTranslate(layer.offsetX, layer.offsetY)
            }
            canvas.drawBitmap(layer.bitmap, matrix, paint)
        }
    }

    private fun renderBrushes(canvas: Canvas, strokes: List<EditOperation.BrushDraw>) {
        for (stroke in strokes) {
            if (stroke.points.size < 2) continue
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = stroke.colorInt
                alpha = (stroke.opacity * 255).toInt()
                strokeWidth = stroke.strokeWidth
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                if (stroke.isEraser) {
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                }
            }
            val path = Path().apply {
                moveTo(stroke.points[0].first, stroke.points[0].second)
                for (i in 1 until stroke.points.size) {
                    lineTo(stroke.points[i].first, stroke.points[i].second)
                }
            }
            canvas.drawPath(path, paint)
        }
    }

    private fun renderTexts(canvas: Canvas, texts: List<EditOperation.TextOverlay>) {
        for (item in texts) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = item.colorInt
                alpha = (item.opacity * 255).toInt()
                textSize = item.textSize
                typeface = when (item.fontFamilyName) {
                    "SERIF" -> Typeface.SERIF
                    "MONOSPACE" -> Typeface.MONOSPACE
                    else -> Typeface.SANS_SERIF
                }
                textAlign = when (item.alignment) {
                    1 -> Paint.Align.CENTER
                    2 -> Paint.Align.RIGHT
                    else -> Paint.Align.LEFT
                }
            }
            canvas.drawText(item.text, item.posX, item.posY, paint)
        }
    }

    private fun renderCloneStamps(canvas: Canvas, stamps: List<EditOperation.CloneStampPoint>, bitmap: Bitmap) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        for (stamp in stamps) {
            val sx = stamp.sourceX.toInt()
            val sy = stamp.sourceY.toInt()
            val r = stamp.radius.toInt()

            if (sx - r >= 0 && sy - r >= 0 && sx + r < bitmap.width && sy + r < bitmap.height) {
                val patch = Bitmap.createBitmap(bitmap, sx - r, sy - r, r * 2, r * 2)
                canvas.drawBitmap(patch, stamp.targetX - r, stamp.targetY - r, paint)
            }
        }
    }
}
