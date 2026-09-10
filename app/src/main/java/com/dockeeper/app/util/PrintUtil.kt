package com.dockeeper.app.util

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import android.print.PrintAttributes
import android.print.PrintManager
import androidx.core.content.getSystemService
import com.dockeeper.app.domain.model.Attachment
import java.io.File

/**
 * Impresión con el PrintManager de Android.
 * - Un documento (PDF u otro archivo) usa un PrintDocumentAdapter sobre el archivo.
 * - Una o varias imágenes se combinan en un PDF multipágina y se imprimen juntas
 *   (ideal para "resultados de exámenes de varias hojas").
 */
object PrintUtil {

    /** Imprime una sola imagen. */
    fun printImage(context: Context, filePath: String, jobName: String) {
        printImages(context, listOf(filePath), jobName)
    }

    /** Imprime un archivo PDF existente conservando su contenido tal cual. */
    fun printPdfFile(context: Context, file: File, jobName: String) {
        val printManager = context.getSystemService<PrintManager>() ?: return
        val adapter = FilePrintAdapter(file, jobName)
        printManager.print(jobName, adapter, PrintAttributes.Builder().build())
    }

    /**
     * Imprime todas las imágenes de un item como un solo trabajo (una por página).
     */
    fun printImages(context: Context, filePaths: List<String>, jobName: String) {
        if (filePaths.isEmpty()) return
        val printManager = context.getSystemService<PrintManager>() ?: return
        val adapter = ImagesPrintAdapter(context, filePaths, jobName)
        printManager.print(jobName, adapter, PrintAttributes.Builder().build())
    }

    /**
     * Imprime todos los adjuntos imprimibles de un item.
     * Junta las imágenes en un PDF multipágina. (Los PDF/documentos se imprimen
     * por separado desde el visor, ya que combinar PDFs requiere más librerías.)
     */
    fun printItemImages(context: Context, attachments: List<Attachment>, jobName: String) {
        val imagePaths = attachments
            .filter { it.isImage && it.filePath != null }
            .sortedBy { it.orderIndex }
            .mapNotNull { it.filePath }
        printImages(context, imagePaths, jobName)
    }

    /** Genera un PDF multipágina desde imágenes en un archivo de cache y lo devuelve. */
    fun buildPdfFromImages(context: Context, filePaths: List<String>, name: String): File? {
        if (filePaths.isEmpty()) return null
        val doc = PdfDocument()
        try {
            filePaths.forEachIndexed { index, path ->
                val bitmap = BitmapFactory.decodeFile(path) ?: return@forEachIndexed
                val pageInfo = PdfDocument.PageInfo.Builder(
                    bitmap.width,
                    bitmap.height,
                    index + 1
                ).create()
                val page = doc.startPage(pageInfo)
                page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                doc.finishPage(page)
                bitmap.recycle()
            }
            val outFile = File(context.cacheDir, "$name.pdf")
            outFile.outputStream().use { doc.writeTo(it) }
            return outFile
        } catch (e: Exception) {
            return null
        } finally {
            doc.close()
        }
    }
}
