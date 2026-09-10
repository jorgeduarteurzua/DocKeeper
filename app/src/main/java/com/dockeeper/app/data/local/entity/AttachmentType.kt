package com.dockeeper.app.data.local.entity

/**
 * Tipo de un adjunto dentro de un Item.
 * - DOCUMENT: archivo (PDF, Word, etc.) copiado al almacenamiento interno.
 * - PHOTO: imagen (de galería o cámara) copiada al almacenamiento interno.
 * - LINK: una URL pegada manualmente por el usuario.
 * - QR: contenido rescatado de un código QR (puede ser link, texto, etc.).
 */
enum class AttachmentType {
    DOCUMENT,
    PHOTO,
    LINK,
    QR
}
