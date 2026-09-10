package com.dockeeper.app.ui.qr

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Analiza cada frame de la cámara buscando códigos (QR, barras, etc.) con ML Kit.
 * En cuanto detecta uno, llama a [onDetected] una sola vez y deja de reportar.
 */
class QrCodeAnalyzer(
    private val onDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val scanner = BarcodeScanning.getClient()
    private val handled = AtomicBoolean(false)

    @androidx.camera.core.ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        if (handled.get()) {
            imageProxy.close()
            return
        }
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }
        val inputImage = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )
        scanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                val value = barcodes.firstOrNull()?.let { extractValue(it) }
                if (!value.isNullOrBlank() && handled.compareAndSet(false, true)) {
                    onDetected(value)
                }
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    private fun extractValue(barcode: Barcode): String? {
        return barcode.url?.url ?: barcode.rawValue ?: barcode.displayValue
    }
}
