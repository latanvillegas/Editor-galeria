package com.hypereditor.nativegallery.data

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream

object MediaStoreSaver {

    suspend fun saveBitmapToGallery(
        context: Context,
        bitmap: Bitmap,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
        quality: Int = 95,
        fileNamePrefix: String = "HyperEdit_"
    ): Result<Uri> = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val filename = "$fileNamePrefix${System.currentTimeMillis()}.${if (format == Bitmap.CompressFormat.PNG) "png" else "jpg"}"
        val mimeType = if (format == Bitmap.CompressFormat.PNG) "image/png" else "image/jpeg"

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/HyperEditor")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val collectionUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        try {
            val itemUri = resolver.insert(collectionUri, contentValues)
                ?: return@withContext Result.failure(Exception("No se pudo registrar en MediaStore"))

            val outputStream: OutputStream = resolver.openOutputStream(itemUri)
                ?: return@withContext Result.failure(Exception("No se pudo abrir OutputStream"))

            outputStream.use { stream ->
                if (!bitmap.compress(format, quality, stream)) {
                    return@withContext Result.failure(Exception("Fallo al comprimir imagen"))
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(itemUri, contentValues, null, null)
            }

            Result.success(itemUri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
