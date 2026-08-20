package com.hypereditor.nativegallery.data

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

object IntentImageLoader {

    fun extractUriFromIntent(intent: Intent?): Uri? {
        if (intent == null) return null
        return when (intent.action) {
            Intent.ACTION_EDIT -> intent.data
            Intent.ACTION_SEND -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM)
                }
            }
            else -> intent.data
        }
    }

    suspend fun loadBitmapFromUri(
        context: Context,
        uri: Uri,
        maxDimension: Int = 4096
    ): Result<Bitmap> = withContext(Dispatchers.IO) {
        try {
            val contentResolver: ContentResolver = context.contentResolver

            val firstStream = contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(Exception("No se pudo abrir stream"))

            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            firstStream.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            val origW = options.outWidth
            val origH = options.outHeight
            var sampleSize = 1
            while ((origW / sampleSize) > maxDimension || (origH / sampleSize) > maxDimension) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
                inMutable = true
            }

            val secondStream = contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(Exception("No se pudo abrir stream para decodificar"))

            var decodedBitmap: Bitmap = secondStream.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return@withContext Result.failure(Exception("Fallo al decodificar mapa de bits"))

            contentResolver.openInputStream(uri)?.use { exifStream ->
                val exif = ExifInterface(exifStream)
                val orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
                val matrix = Matrix()
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                    ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                    ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                    ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                }
                if (!matrix.isIdentity) {
                    val rotated = Bitmap.createBitmap(
                        decodedBitmap, 0, 0, decodedBitmap.width, decodedBitmap.height, matrix, true
                    )
                    decodedBitmap = rotated
                }
            }

            Result.success(decodedBitmap)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
