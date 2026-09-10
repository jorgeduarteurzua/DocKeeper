package com.dockeeper.app.util

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import java.io.FileOutputStream
import kotlin.math.min

/**
 * PrintDocumentAdapter que toma una lista de rutas de imágenes y las renderiza,
 * una por página, en un PDF que el sistema envía a la impresora o a "Guardar como PDF".
 */
class ImagesPrintAdapter(
    private val context: Context,
    private val imagePaths: List<String>,
    private val jobName: String
) : PrintDocumentAdapter() {

    private var pageWidth = 0
    private var pageHeight = 0

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback,
        extras: Bundle?
    ) {
        val mediaSize = newAttributes.mediaSize ?: PrintAttributes.MediaSize.ISO_A4
        // 1/1000 de pulgada -> puntos (72 dpi).
        pageWidth = (mediaSize.widthMils / 1000.0 * 72).toInt()
        pageHeight = (mediaSize.heightMils / 1000.0 * 72).toInt()

        if (cancellationSignal?.isCanceled == true) {
            callback.onLayoutCancelled()
            return
        }

        val info = PrintDocumentInfo.Builder("$jobName.pdf")
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .setPageCount(imagePaths.size)
            .build()
        callback.onLayoutFinished(info, true)
    }

    override fun onWrite(
        pages: Array<out PageRange>?,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback
    ) {
        val pdf = PdfDocument()
        try {
            imagePaths.forEachIndexed { index, path ->
                if (cancellationSignal?.isCanceled == true) {
                    callback.onWriteCancelled()
                    pdf.close()
                    return
                }
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
                val page = pdf.startPage(pageInfo)

                val bitmap = BitmapFactory.decodeFile(path)
                if (bitmap != null) {
                    // Escalar manteniendo proporción y centrar con margen.
                    val margin = 24
                    val availW = pageWidth - margin * 2
                    val availH = pageHeight - margin * 2
                    val scale = min(
                        availW.toFloat() / bitmap.width,
                        availH.toFloat() / bitmap.height
                    ).coerceAtMost(1f)
                    val drawW = (bitmap.width * scale).toInt()
                    val drawH = (bitmap.height * scale).toInt()
                    val left = (pageWidth - drawW) / 2
                    val top = (pageHeight - drawH) / 2
                    val destRect = Rect(left, top, left + drawW, top + drawH)
                    page.canvas.drawBitmap(bitmap, null, destRect, null)
                    bitmap.recycle()
                }
                pdf.finishPage(page)
            }

            FileOutputStream(destination.fileDescriptor).use { out ->
                pdf.writeTo(out)
            }
            callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (e: Exception) {
            callback.onWriteFailed(e.message)
        } finally {
            pdf.close()
        }
    }
}
