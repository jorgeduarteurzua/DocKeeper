package com.dockeeper.app.data.file

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Copia, elimina y expone archivos guardados en el almacenamiento interno privado
 * de la app (files/attachments). Nada sale del dispositivo.
 */
@Singleton
class FileManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val attachmentsDir: File
        get() = File(context.filesDir, ATTACHMENTS_DIR).apply { if (!exists()) mkdirs() }

    /**
     * Copia el contenido de [uri] a un archivo interno propio de la app.
     * @return la ruta absoluta del archivo creado.
     */
    suspend fun importFromUri(uri: Uri, suggestedName: String? = null): ImportedFile =
        withContext(Dispatchers.IO) {
            val resolver = context.contentResolver
            val displayName = suggestedName ?: queryDisplayName(uri) ?: "archivo"
            val mimeType = resolver.getType(uri)
            val extension = extractExtension(displayName, mimeType)
            val targetName = "${UUID.randomUUID()}${if (extension.isNotEmpty()) ".$extension" else ""}"
            val target = File(attachmentsDir, targetName)

            resolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: throw IllegalStateException("No se pudo abrir el contenido en $uri")

            ImportedFile(
                filePath = target.absolutePath,
                displayName = displayName,
                mimeType = mimeType ?: guessMimeType(extension)
            )
        }

    /**
     * Crea un archivo vacío destino para que la cámara escriba la foto.
     * Devuelve el File y su Uri content:// para pasarlo a la cámara.
     */
    fun createImageTarget(): CameraTarget {
        val fileName = "${UUID.randomUUID()}.jpg"
        val file = File(attachmentsDir, fileName)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return CameraTarget(file = file, uri = uri)
    }

    /** Devuelve un Uri content:// para compartir/imprimir/abrir un archivo interno. */
    fun getShareableUri(filePath: String): Uri {
        val file = File(filePath)
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    suspend fun deleteFile(filePath: String?) = withContext(Dispatchers.IO) {
        if (filePath.isNullOrBlank()) return@withContext
        runCatching { File(filePath).takeIf { it.exists() }?.delete() }
    }

    suspend fun deleteFiles(filePaths: List<String>) = withContext(Dispatchers.IO) {
        filePaths.forEach { path ->
            runCatching { File(path).takeIf { it.exists() }?.delete() }
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        return runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else null
            }
        }.getOrNull()
    }

    private fun extractExtension(displayName: String, mimeType: String?): String {
        val fromName = displayName.substringAfterLast('.', "")
        if (fromName.isNotEmpty() && fromName.length <= 5) return fromName.lowercase()
        val fromMime = mimeType?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
        return fromMime ?: ""
    }

    private fun guessMimeType(extension: String): String {
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase())
            ?: "application/octet-stream"
    }

    companion object {
        const val ATTACHMENTS_DIR = "attachments"
    }
}

data class ImportedFile(
    val filePath: String,
    val displayName: String,
    val mimeType: String
)

data class CameraTarget(
    val file: File,
    val uri: Uri
)
