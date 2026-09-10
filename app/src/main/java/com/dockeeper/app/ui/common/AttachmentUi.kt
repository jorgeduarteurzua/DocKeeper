package com.dockeeper.app.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.dockeeper.app.data.local.entity.AttachmentType

fun iconForType(type: AttachmentType): ImageVector = when (type) {
    AttachmentType.DOCUMENT -> Icons.Filled.Description
    AttachmentType.PHOTO -> Icons.Filled.Image
    AttachmentType.LINK -> Icons.Filled.Link
    AttachmentType.QR -> Icons.Filled.QrCode
}

/** Color distintivo por tipo de adjunto, para dar vida a la UI. */
fun colorForType(type: AttachmentType): Color = when (type) {
    AttachmentType.DOCUMENT -> Color(0xFFE53935) // rojo
    AttachmentType.PHOTO -> Color(0xFF1E88E5)    // azul
    AttachmentType.LINK -> Color(0xFF00897B)     // teal
    AttachmentType.QR -> Color(0xFF8E24AA)       // púrpura
}

fun labelForType(type: AttachmentType): String = when (type) {
    AttachmentType.DOCUMENT -> "Documento"
    AttachmentType.PHOTO -> "Foto"
    AttachmentType.LINK -> "Enlace"
    AttachmentType.QR -> "QR"
}
