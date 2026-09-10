package com.dockeeper.app.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

/**
 * Acciones que involucran apps externas del sistema: abrir un archivo/link,
 * compartir, etc. La impresión vive en PrintUtil.
 */
object ExternalActions {

    private fun uriFor(context: Context, filePath: String): Uri =
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            File(filePath)
        )

    /** Abre un archivo interno con la app apropiada del sistema. */
    fun openFile(context: Context, filePath: String, mimeType: String?) {
        val uri = uriFor(context, filePath)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType ?: "*/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(Intent.createChooser(intent, "Abrir con"))
        } catch (e: ActivityNotFoundException) {
            toast(context, "No hay una app para abrir este archivo")
        }
    }

    /** Abre una URL en el navegador. */
    fun openUrl(context: Context, url: String) {
        val normalized = if (url.startsWith("http://") || url.startsWith("https://")) {
            url
        } else {
            "https://$url"
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(normalized))
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            toast(context, "No se pudo abrir el enlace")
        }
    }

    fun shareFile(context: Context, filePath: String, mimeType: String?) {
        val uri = uriFor(context, filePath)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType ?: "*/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartir"))
    }

    fun shareText(context: Context, text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Compartir"))
    }

    fun copyToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE)
                as android.content.ClipboardManager
        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("DocKeeper", text))
        toast(context, "Copiado")
    }

    fun toast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
