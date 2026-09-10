package com.dockeeper.app.ui.itemdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.dockeeper.app.domain.model.Attachment
import com.dockeeper.app.ui.common.ConfirmDeleteDialog
import com.dockeeper.app.ui.common.EmptyState
import com.dockeeper.app.ui.common.colorForType
import com.dockeeper.app.ui.common.formatDate
import com.dockeeper.app.ui.common.iconForType
import com.dockeeper.app.ui.common.labelForType
import com.dockeeper.app.util.ExternalActions
import com.dockeeper.app.util.PrintUtil
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailScreen(
    onBack: () -> Unit,
    onAddMore: (categoryId: Long, itemId: Long) -> Unit,
    onAttachmentClick: (itemId: Long, attachmentId: Long) -> Unit,
    viewModel: ItemDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val pdfState by viewModel.pdfState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var confirmDeleteItem by remember { mutableStateOf(false) }

    val detail = state.detail
    val hasImages = detail?.attachments?.any { it.isImage && it.filePath != null } == true
    val hasAttachments = (detail?.attachments?.isNotEmpty()) == true

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(detail?.item?.title?.ifBlank { "Sin título" } ?: "Registro", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                actions = {
                    if (hasAttachments) {
                        IconButton(
                            onClick = { viewModel.generatePdf() },
                            enabled = pdfState !is PdfState.Generating
                        ) {
                            Icon(Icons.Filled.PictureAsPdf, contentDescription = "Generar PDF")
                        }
                    }
                    if (hasImages) {
                        IconButton(onClick = {
                            PrintUtil.printItemImages(
                                context = context,
                                attachments = detail!!.attachments,
                                jobName = detail.item.title.ifBlank { "Registro" }
                            )
                        }) {
                            Icon(Icons.Filled.Print, contentDescription = "Imprimir todo")
                        }
                    }
                    IconButton(onClick = { confirmDeleteItem = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Eliminar registro")
                    }
                }
            )
        },
        floatingActionButton = {
            if (detail != null) {
                ExtendedFloatingActionButton(
                    onClick = { onAddMore(detail.item.categoryId, detail.item.id) },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Agregar adjunto") }
                )
            }
        }
    ) { padding ->
        if (detail == null) {
            Box(Modifier.fillMaxSize().padding(padding))
            return@Scaffold
        }
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (detail.item.description.isNotBlank()) {
                Text(
                    text = detail.item.description,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            Text(
                text = "Creado el ${formatDate(detail.item.createdDate)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            if (detail.attachments.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.Add,
                    title = "Sin adjuntos",
                    subtitle = "Usa el botón + para agregar fotos, documentos o enlaces."
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 140.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(detail.attachments, key = { it.id }) { attachment ->
                        AttachmentTile(
                            attachment = attachment,
                            onClick = { onAttachmentClick(detail.item.id, attachment.id) }
                        )
                    }
                }
            }
        }
    }

    if (confirmDeleteItem) {
        ConfirmDeleteDialog(
            title = "Eliminar registro",
            message = "Se eliminará este registro y todos sus adjuntos.",
            onConfirm = {
                confirmDeleteItem = false
                viewModel.deleteItem(onDeleted = onBack)
            },
            onDismiss = { confirmDeleteItem = false }
        )
    }

    // Diálogos del flujo de generación de PDF.
    when (val s = pdfState) {
        is PdfState.Generating -> {
            AlertDialog(
                onDismissRequest = { },
                confirmButton = {},
                icon = { CircularProgressIndicator() },
                title = { Text("Generando PDF") },
                text = { Text("Combinando los adjuntos del registro en un solo documento…") }
            )
        }
        is PdfState.Success -> {
            AlertDialog(
                onDismissRequest = { viewModel.consumePdfState() },
                icon = { Icon(Icons.Filled.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                title = { Text("PDF generado") },
                text = { Text("Se creó un PDF con todo el contenido de este registro.") },
                confirmButton = {
                    Row {
                        TextButton(onClick = {
                            PrintUtil.printPdfFile(
                                context,
                                s.file,
                                detail?.item?.title?.ifBlank { "Registro" } ?: "Registro"
                            )
                            viewModel.consumePdfState()
                        }) { Text("Imprimir") }
                        TextButton(onClick = {
                            ExternalActions.shareFile(context, s.file.absolutePath, "application/pdf")
                            viewModel.consumePdfState()
                        }) { Text("Compartir") }
                        TextButton(onClick = {
                            ExternalActions.openFile(context, s.file.absolutePath, "application/pdf")
                            viewModel.consumePdfState()
                        }) { Text("Abrir") }
                    }
                }
            )
        }
        is PdfState.Error -> {
            AlertDialog(
                onDismissRequest = { viewModel.consumePdfState() },
                confirmButton = {
                    TextButton(onClick = { viewModel.consumePdfState() }) { Text("Entendido") }
                },
                title = { Text("No se pudo generar") },
                text = { Text(s.message) }
            )
        }
        PdfState.Idle -> {}
    }
}

@Composable
private fun AttachmentTile(
    attachment: Attachment,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (attachment.isImage && attachment.filePath != null) {
                    AsyncImage(
                        model = File(attachment.filePath),
                        contentDescription = attachment.displayName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    val typeColor = colorForType(attachment.type)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(typeColor.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(color = typeColor, shape = CircleShape) {
                            Icon(
                                iconForType(attachment.type),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }
            Text(
                text = attachment.displayName.ifBlank { labelForType(attachment.type) },
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
            )
        }
    }
}
