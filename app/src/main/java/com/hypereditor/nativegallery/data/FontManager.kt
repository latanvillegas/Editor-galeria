package com.hypereditor.nativegallery.data

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream

data class CustomFont(
    val name: String,
    val file: File
)

object FontManager {
    private const val FONTS_DIR = "custom_fonts"

    fun getFontsDir(context: Context): File {
        val dir = File(context.filesDir, FONTS_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Loads all persistent custom fonts stored in app internal storage.
     */
    fun loadSavedFonts(context: Context): List<CustomFont> {
        val dir = getFontsDir(context)
        val files = dir.listFiles { f ->
            val ext = f.extension.lowercase()
            ext == "ttf" || ext == "otf"
        } ?: emptyArray()

        return files.mapNotNull { file ->
            try {
                val tf = Typeface.createFromFile(file)
                if (tf != null) {
                    CustomFont(file.nameWithoutExtension, file)
                } else null
            } catch (_: Exception) {
                null
            }
        }.sortedBy { it.name.lowercase() }
    }

    /**
     * Imports a font from a Storage Access Framework Uri, copies to internal storage,
     * validates that it parses as a Typeface, and returns the CustomFont.
     * Returns Result.failure with human-readable error if invalid.
     */
    fun importFontFromUri(context: Context, uri: Uri): Result<CustomFont> {
        return runCatching {
            val contentResolver = context.contentResolver
            var displayName = "font_${System.currentTimeMillis()}.ttf"

            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        val retrieved = cursor.getString(nameIndex)
                        if (!retrieved.isNullOrBlank()) {
                            displayName = retrieved
                        }
                    }
                }
            }

            val lowerName = displayName.lowercase()
            if (!lowerName.endsWith(".ttf") && !lowerName.endsWith(".otf")) {
                throw IllegalArgumentException("Formato no compatible: el archivo debe tener extensión .ttf o .otf ($displayName)")
            }

            val sanitizedName = displayName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val fontsDir = getFontsDir(context)
            val destFile = File(fontsDir, sanitizedName)

            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            } ?: throw IllegalStateException("No se pudo leer el archivo seleccionado")

            // Validar que el motor tipográfico de Android pueda interpretar la fuente
            val typeface = try {
                Typeface.createFromFile(destFile)
            } catch (e: Exception) {
                destFile.delete()
                throw IllegalArgumentException("El archivo no es una tipografía TTF/OTF válida o está dañado (${e.localizedMessage})")
            }

            if (typeface == null) {
                destFile.delete()
                throw IllegalArgumentException("El sistema no pudo interpretar la fuente tipográfica seleccionada.")
            }

            CustomFont(destFile.nameWithoutExtension, destFile)
        }
    }
}
