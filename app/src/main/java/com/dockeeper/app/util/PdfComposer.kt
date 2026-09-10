package com.dockeeper.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.dockeeper.app.domain.model.Attachment
import com.dockeeper.app.domain.model.ItemDetail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min

/**
 * Combina todos los adjuntos de un Item en un único PDF (Opción A):
 * - Imágenes: una por página, escaladas y centradas.
 * - PDFs adjuntos: cada página se rasteriza (PdfRenderer) y se añade.
 * - Enlaces / QR: una página de texto con el contenido.
 * - Documentos no compatibles (Word, Excel, etc.): una página-marcador que
 *   indica que el archivo existe pero no puede incrustarse.
 *
 * Todo es API nativa de Android, sin librerías externas.
 */
object PdfComposer {

    // Tamaño de página A4 a 72 dpi (en puntos).
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 36

    /**
     * Genera el PDF del item y lo guarda en [outputDir]. Devuelve el archivo creado.
     */
    suspend fun buildItemPdf(
        context: Context,
        detail: ItemDetail,
        outputDir: File
    ): File = withContext(Dispatchers.IO) {
        val document = PdfDocument()
        var pageNumber = 1

        try {
            // Portada con el título y datos del item.
            drawCoverPage(document, detail, pageNumber++)

            detail.attachments
                .sortedWith(compareBy({ it.orderIndex }, { it.createdDate }))
                .forEach { attachment ->
                    pageNumber = addAttachment(document, attachment, pageNumber)
                }

            if (!outputDir.exists()) outputDir.mkdirs()
            val safeName = sanitizeFileName(detail.item.title.ifBlank { "registro" })
            val outFile = File(outputDir, "${safeName}_${System.currentTimeMillis()}.pdf")
            outFile.outputStream().use { document.writeTo(it) }
            outFile
        } finally {
            document.close()
        }
    }

    // ---- Páginas por tipo de adjunto ----

    private fun addAttachment(
        document: PdfDocument,
        attachment: Attachment,
        startPage: Int
    ): Int {
        var page = startPage
        when {
            attachment.isImage && attachment.filePath != null -> {
                drawImagePage(document, attachment.filePath, page++)
            }
            attachment.isPdf && attachment.filePath != null -> {
                page = drawPdfPages(document, attachment.filePath, page)
            }
            attachment.linkUrl != null -> {
                drawTextPage(
                    document = document,
                    pageNumber = page++,
                    heading = if (attachment.type.name == "QR") "Código QR" else "Enlace",
                    lines = listOf(
                        attachment.displayName.ifBlank { "(sin nombre)" },
                        "",
                        attachment.linkUrl
                    )
                )
            }
            else -> {
                // Documento no compatible (Word, Excel, etc.): página-marcador.
                drawTextPage(
                    document = document,
                    pageNumber = page++,
                    heading = "Documento adjunto",
                    lines = listOf(
                        attachment.displayName.ifBlank { "(sin nombre)" },
                        attachment.mimeType ?: "Tipo desconocido",
                        "",
                        "Este documento no puede incrustarse en el PDF,",
                        "pero forma parte de este registro."
                    )
                )
            }
        }
        return page
    }

    private fun drawImagePage(document: PdfDocument, filePath: String, pageNumber: Int) {
        val bitmap = decodeScaledBitmap(filePath, PAGE_WIDTH, PAGE_HEIGHT) ?: return
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        val page = document.startPage(pageInfo)
        drawBitmapCentered(page.canvas, bitmap)
        document.finishPage(page)
        bitmap.recycle()
    }

    /** Rasteriza cada página del PDF adjunto en una página del PDF final. */
    private fun drawPdfPages(document: PdfDocument, filePath: String, startPage: Int): Int {
        var page = startPage
        val file = File(filePath)
        if (!file.exists()) return page

        var descriptor: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        try {
            descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(descriptor)
            for (i in 0 until renderer.pageCount) {
                renderer.openPage(i).use { pdfPage ->
                    // Renderizar a un bitmap del tamaño de la página A4.
                    val scale = min(
                        PAGE_WIDTH.toFloat() / pdfPage.width,
                        PAGE_HEIGHT.toFloat() / pdfPage.height
                    )
                    val w = (pdfPage.width * scale).toInt().coerceAtLeast(1)
                    val h = (pdfPage.height * scale).toInt().coerceAtLeast(1)
                    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(Color.WHITE)
                    pdfPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                    val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, page++).create()
                    val outPage = document.startPage(pageInfo)
                    drawBitmapCentered(outPage.canvas, bitmap)
                    document.finishPage(outPage)
                    bitmap.recycle()
                }
            }
        } catch (e: Exception) {
            // Si el PDF está protegido o corrupto, dejamos una página-marcador.
            drawTextPage(
                document = document,
                pageNumber = page++,
                heading = "Documento PDF",
                lines = listOf(
                    file.name,
                    "",
                    "No se pudo leer este PDF para incluirlo."
                )
            )
        } finally {
            renderer?.close()
            descriptor?.close()
        }
        return page
    }

    private fun drawCoverPage(document: PdfDocument, detail: ItemDetail, pageNumber: Int) {
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.parseColor("#3F51B5")
            textSize = 26f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val bodyPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 14f
            isAntiAlias = true
        }
        val labelPaint = Paint().apply {
            color = Color.parseColor("#009688")
            textSize = 12f
            isAntiAlias = true
        }

        var y = MARGIN + 40f
        canvas.drawText("DocKeeper", MARGIN.toFloat(), y, labelPaint)
        y += 40f
        for (line in wrapText(detail.item.title.ifBlank { "Sin título" }, titlePaint, PAGE_WIDTH - MARGIN * 2)) {
            canvas.drawText(line, MARGIN.toFloat(), y, titlePaint)
            y += 32f
        }
        y += 8f
        val dateStr = SimpleDateFormat("dd MMM yyyy", Locale("es")).format(Date(detail.item.createdDate))
        canvas.drawText("Creado el $dateStr", MARGIN.toFloat(), y, bodyPaint)
        y += 22f
        canvas.drawText("${detail.attachments.size} adjunto(s)", MARGIN.toFloat(), y, bodyPaint)
        y += 30f
        if (detail.item.description.isNotBlank()) {
            for (line in wrapText(detail.item.description, bodyPaint, PAGE_WIDTH - MARGIN * 2)) {
                canvas.drawText(line, MARGIN.toFloat(), y, bodyPaint)
                y += 20f
            }
        }

        document.finishPage(page)
    }

    private fun drawTextPage(
        document: PdfDocument,
        pageNumber: Int,
        heading: String,
        lines: List<String>
    ) {
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val headingPaint = Paint().apply {
            color = Color.parseColor("#3F51B5")
            textSize = 20f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val bodyPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 14f
            isAntiAlias = true
        }

        var y = MARGIN + 40f
        canvas.drawText(heading, MARGIN.toFloat(), y, headingPaint)
        y += 34f
        for (raw in lines) {
            for (line in wrapText(raw, bodyPaint, PAGE_WIDTH - MARGIN * 2)) {
                canvas.drawText(line, MARGIN.toFloat(), y, bodyPaint)
                y += 20f
            }
        }
        document.finishPage(page)
    }

    // ---- Helpers ----

    private fun drawBitmapCentered(canvas: Canvas, bitmap: Bitmap) {
        val availW = PAGE_WIDTH - MARGIN * 2
        val availH = PAGE_HEIGHT - MARGIN * 2
        val scale = min(
            availW.toFloat() / bitmap.width,
            availH.toFloat() / bitmap.height
        ).coerceAtMost(1f)
        val drawW = (bitmap.width * scale).toInt()
        val drawH = (bitmap.height * scale).toInt()
        val left = (PAGE_WIDTH - drawW) / 2
        val top = (PAGE_HEIGHT - drawH) / 2
        canvas.drawBitmap(bitmap, null, Rect(left, top, left + drawW, top + drawH), null)
    }

    /** Decodifica un bitmap reduciéndolo si es enorme, para no agotar memoria. */
    private fun decodeScaledBitmap(filePath: String, reqW: Int, reqH: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(filePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        // Renderizamos a ~2x el tamaño de página para buena nitidez sin excedernos.
        val targetW = reqW * 2
        val targetH = reqH * 2
        while (bounds.outWidth / sample > targetW || bounds.outHeight / sample > targetH) {
            sample *= 2
        }
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeFile(filePath, opts)
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Int): List<String> {
        if (text.isEmpty()) return listOf("")
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var current = StringBuilder()
        for (word in words) {
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (paint.measureText(candidate) <= maxWidth) {
                current = StringBuilder(candidate)
            } else {
                if (current.isNotEmpty()) lines.add(current.toString())
                // Palabra sola más ancha que la línea: la partimos por caracteres.
                if (paint.measureText(word) > maxWidth) {
                    var chunk = StringBuilder()
                    for (ch in word) {
                        if (paint.measureText(chunk.toString() + ch) <= maxWidth) {
                            chunk.append(ch)
                        } else {
                            lines.add(chunk.toString())
                            chunk = StringBuilder(ch.toString())
                        }
                    }
                    current = chunk
                } else {
                    current = StringBuilder(word)
                }
            }
        }
        if (current.isNotEmpty()) lines.add(current.toString())
        return lines
    }

    private fun sanitizeFileName(name: String): String =
        name.replace(Regex("[^a-zA-Z0-9-_ ]"), "").trim().replace(" ", "_").ifBlank { "registro" }
}
