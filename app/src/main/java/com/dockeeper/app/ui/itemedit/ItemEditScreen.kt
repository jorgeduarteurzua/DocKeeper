package com.dockeeper.app.ui.itemedit

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.dockeeper.app.data.local.entity.AttachmentType
import com.dockeeper.app.domain.model.Attachment
import com.dockeeper.app.ui.common.colorForType
import com.dockeeper.app.ui.common.iconForType
import com.dockeeper.app.ui.common.labelForType
import com.dockeeper.app.util.ExternalActions
import androidx.compose.ui.graphics.Color
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemEditScreen(
    onDone: () -> Unit,
    onScanQr: () -> Unit,
    qrResult: String?,
    onQrConsumed: () -> Unit,
    viewModel: ItemEditViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showAddSheet by remember { mutableStateOf(false) }
    var showLinkDialog by remember { mutableStateOf(false) }
    var pendingCameraFile by remember { mutableStateOf<File?>(null) }
    // Solicitud de tomar foto pendiente: se dispara tras cerrar el sheet y tener permiso.
    var cameraRequested by remember { mutableStateOf(false) }

    // Resultado del QR recibido desde el escáner.
    LaunchedEffect(qrResult) {
        if (!qrResult.isNullOrBlank()) {
            viewModel.addLink(qrResult, "Código QR", AttachmentType.QR)
            onQrConsumed()
        }
    }

    // Selector de imágenes: usa el Photo Picker moderno, que ofrece
    // multiselección nativa y clara. Permite elegir una o varias fotos.
    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        uris.forEach { uri ->
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            viewModel.addFileAttachment(uri, AttachmentType.PHOTO)
        }
    }

    // Selector de documentos (permite elegir uno o varios).
    val pickDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        uris.forEach { uri ->
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            viewModel.addFileAttachment(uri, AttachmentType.DOCUMENT)
        }
    }

    // Cámara: toma foto en un archivo destino que preparamos.
    val takePicture = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        val file = pendingCameraFile
        if (success && file != null) {
            viewModel.addCameraPhoto(file.absolutePath)
        }
        pendingCameraFile = null
    }

    // Lanza el intent de cámara: crea el archivo destino y abre la cámara.
    fun launchCamera() {
        val target = viewModel.createCameraTarget()
        pendingCameraFile = target.file
        takePicture.launch(target.uri)
    }

    // Solicita permiso de cámara; si se concede, marca la petición pendiente.
    val requestCameraPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted: Boolean ->
        if (granted) {
            cameraRequested = true
        } else {
            ExternalActions.toast(context, "Se necesita permiso de cámara para tomar fotos")
        }
    }

    // Cuando hay una petición pendiente y el sheet ya está cerrado, abrimos la cámara.
    LaunchedEffect(cameraRequested, showAddSheet) {
        if (cameraRequested && !showAddSheet) {
            cameraRequested = false
            launchCamera()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isNew) "Nuevo registro" else "Editar registro", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.discardIfEmpty()
                        onDone()
                    }) {
                        Icon(Icons.Filled.Close, contentDescription = "Cerrar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                actions = {
                    TextButton(onClick = { viewModel.save(onDone) }) {
                        Text("Guardar", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = state.title,
                onValueChange = viewModel::onTitleChange,
                label = { Text("Título") },
                placeholder = { Text("Ej: Examen de Sangre - Marzo 2026") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.description,
                onValueChange = viewModel::onDescriptionChange,
                label = { Text("Descripción (opcional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Adjuntos (${state.attachments.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(onClick = { showAddSheet = true }) {
                    Text("Agregar")
                }
            }
            Spacer(Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.attachments, key = { it.id }) { attachment ->
                    AttachmentRow(
                        attachment = attachment,
                        onRemove = { viewModel.deleteAttachment(attachment.id) }
                    )
                }
            }
        }
    }

    if (showAddSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showAddSheet = false },
            sheetState = sheetState
        ) {
            AddAttachmentSheet(
                onPickPhoto = {
                    showAddSheet = false
                    pickImage.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onTakePhoto = {
                    showAddSheet = false
                    val hasPermission = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED
                    if (hasPermission) {
                        // El LaunchedEffect abrirá la cámara al cerrarse el sheet.
                        cameraRequested = true
                    } else {
                        requestCameraPermission.launch(Manifest.permission.CAMERA)
                    }
                },
                onPickDocument = {
                    showAddSheet = false
                    pickDocument.launch(arrayOf("application/pdf", "application/*", "text/*", "image/*"))
                },
                onScanQr = {
                    showAddSheet = false
                    onScanQr()
                },
                onAddLink = {
                    showAddSheet = false
                    showLinkDialog = true
                }
            )
        }
    }

    if (showLinkDialog) {
        LinkDialog(
            onDismiss = { showLinkDialog = false },
            onConfirm = { url, name ->
                viewModel.addLink(url, name, AttachmentType.LINK)
                showLinkDialog = false
            }
        )
    }
}

@Composable
private fun AttachmentRow(
    attachment: Attachment,
    onRemove: () -> Unit
) {
    Card(shape = RoundedCornerShape(12.dp)) {
        ListItem(
            headlineContent = {
                Text(
                    attachment.displayName.ifBlank { labelForType(attachment.type) },
                    maxLines = 1,
                    fontWeight = FontWeight.Medium
                )
            },
            supportingContent = {
                Text(attachment.linkUrl ?: labelForType(attachment.type), maxLines = 1)
            },
            leadingContent = {
                if (attachment.isImage && attachment.filePath != null) {
                    AsyncImage(
                        model = File(attachment.filePath),
                        contentDescription = null,
                        modifier = Modifier
                            .size(44.dp)
                            .padding(2.dp)
                    )
                } else {
                    val typeColor = colorForType(attachment.type)
                    Surface(
                        color = typeColor.copy(alpha = 0.16f),
                        shape = CircleShape,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                iconForType(attachment.type),
                                contentDescription = null,
                                tint = typeColor
                            )
                        }
                    }
                }
            },
            trailingContent = {
                IconButton(onClick = onRemove) {
                    Icon(Icons.Filled.Close, contentDescription = "Quitar")
                }
            }
        )
    }
}

@Composable
private fun AddAttachmentSheet(
    onPickPhoto: () -> Unit,
    onTakePhoto: () -> Unit,
    onPickDocument: () -> Unit,
    onScanQr: () -> Unit,
    onAddLink: () -> Unit
) {
    Column(modifier = Modifier.padding(bottom = 24.dp)) {
        Text(
            "Agregar adjunto",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
        )
        SheetOption(Icons.Filled.PhotoCamera, "Sacar foto", Color(0xFF1E88E5), onTakePhoto)
        SheetOption(Icons.Filled.PhotoLibrary, "Subir foto de la galería", Color(0xFF1E88E5), onPickPhoto)
        SheetOption(Icons.AutoMirrored.Filled.InsertDriveFile, "Subir documento", Color(0xFFE53935), onPickDocument)
        SheetOption(Icons.Filled.QrCodeScanner, "Escanear QR", Color(0xFF8E24AA), onScanQr)
        SheetOption(Icons.Filled.Link, "Pegar enlace", Color(0xFF00897B), onAddLink)
    }
}

@Composable
private fun SheetOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(label) },
        leadingContent = {
            Surface(color = color.copy(alpha = 0.16f), shape = CircleShape) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.padding(8.dp)
                )
            }
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}
