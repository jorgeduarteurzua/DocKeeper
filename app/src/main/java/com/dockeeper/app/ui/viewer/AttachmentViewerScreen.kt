package com.dockeeper.app.ui.viewer

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.dockeeper.app.data.local.entity.AttachmentType
import com.dockeeper.app.domain.model.Attachment
import com.dockeeper.app.ui.common.ConfirmDeleteDialog
import com.dockeeper.app.ui.common.iconForType
import com.dockeeper.app.util.ExternalActions
import com.dockeeper.app.util.PrintUtil
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachmentViewerScreen(
    onBack: () -> Unit,
    viewModel: AttachmentViewerViewModel = hiltViewModel()
) {
    val attachment by viewModel.attachment.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var confirmDelete by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(attachment?.displayName ?: "Adjunto", maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    val current = attachment
                    if (current != null) {
                        // Imprimir imágenes.
                        if (current.isImage && current.filePath != null) {
                            IconButton(onClick = {
                                PrintUtil.printImage(context, current.filePath, current.displayName.ifBlank { "Adjunto" })
                            }) { Icon(Icons.Filled.Print, contentDescription = "Imprimir") }
                        }
                        // Compartir archivos.
                        if (current.filePath != null) {
                            IconButton(onClick = {
                                ExternalActions.shareFile(context, current.filePath, current.mimeType)
                            }) { Icon(Icons.Filled.Share, contentDescription = "Compartir") }
                        }
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Eliminar")
                        }
                    }
                }
            )
        }
    ) { padding ->
        val current = attachment
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            when {
                current == null -> {}
                current.isImage && current.filePath != null -> {
                    ZoomableImage(
                        filePath = current.filePath,
                        contentDescription = current.displayName
                    )
                }
                current.type == AttachmentType.LINK || current.type == AttachmentType.QR -> {
                    LinkContent(attachment = current, context = context)
                }
                else -> {
                    // Documento: mostrar acción para abrir con app externa.
                    DocumentContent(attachment = current, context = context)
                }
            }
        }
    }

    if (confirmDelete) {
        ConfirmDeleteDialog(
            title = "Eliminar adjunto",
            message = "Se eliminará este adjunto de forma permanente.",
            onConfirm = {
                confirmDelete = false
                viewModel.delete(onDeleted = onBack)
            },
            onDismiss = { confirmDelete = false }
        )
    }
}

@Composable
private fun LinkContent(attachment: Attachment, context: android.content.Context) {
    val url = attachment.linkUrl.orEmpty()
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            iconForType(attachment.type),
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = url,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        val looksLikeUrl = url.startsWith("http") || url.contains(".")
        if (looksLikeUrl) {
            Button(onClick = { ExternalActions.openUrl(context, url) }) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                Text("  Abrir enlace")
            }
        }
        OutlinedButton(onClick = { ExternalActions.copyToClipboard(context, url) }) {
            Text("Copiar")
        }
        OutlinedButton(onClick = { ExternalActions.shareText(context, url) }) {
            Icon(Icons.Filled.Share, contentDescription = null)
            Text("  Compartir")
        }
    }
}

@Composable
private fun DocumentContent(attachment: Attachment, context: android.content.Context) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            iconForType(attachment.type),
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = attachment.displayName.ifBlank { "Documento" },
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Text(
            text = attachment.mimeType ?: "Archivo",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        attachment.filePath?.let { path ->
            Button(onClick = { ExternalActions.openFile(context, path, attachment.mimeType) }) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                Text("  Abrir documento")
            }
            if (attachment.isPdf) {
                OutlinedButton(onClick = {
                    ExternalActions.openFile(context, path, attachment.mimeType)
                }) {
                    Icon(Icons.Filled.Print, contentDescription = null)
                    Text("  Imprimir (abrir y usar imprimir)")
                }
            }
        }
    }
}

/**
 * Imagen con zoom: pellizco para ampliar/reducir, doble toque para alternar
 * entre 1x y 2.5x, y arrastre para desplazar cuando está ampliada.
 */
@Composable
private fun ZoomableImage(
    filePath: String,
    contentDescription: String?
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val minScale = 1f
    val maxScale = 5f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.5f
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(minScale, maxScale)
                    scale = newScale
                    offset = if (newScale > 1f) offset + pan else Offset.Zero
                }
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = File(filePath),
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
        )
    }
}
