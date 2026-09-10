package com.dockeeper.app.domain.model

import com.dockeeper.app.data.local.entity.AttachmentType

/** Categoría con su conteo de items para mostrar en la UI. */
data class Category(
    val id: Long,
    val name: String,
    val colorTag: Int,
    val createdDate: Long,
    val itemCount: Int = 0
)

/** Item (registro) con su conteo de adjuntos. */
data class Item(
    val id: Long,
    val categoryId: Long,
    val title: String,
    val description: String,
    val createdDate: Long,
    val attachmentCount: Int = 0
)

/** Un adjunto individual dentro de un item. */
data class Attachment(
    val id: Long,
    val itemId: Long,
    val type: AttachmentType,
    val displayName: String,
    val filePath: String?,
    val linkUrl: String?,
    val mimeType: String?,
    val orderIndex: Int,
    val createdDate: Long
) {
    val isImage: Boolean
        get() = type == AttachmentType.PHOTO ||
            (mimeType?.startsWith("image/") == true)

    val isPdf: Boolean
        get() = mimeType == "application/pdf"
}

/** Item con todos sus adjuntos, para la pantalla de detalle. */
data class ItemDetail(
    val item: Item,
    val attachments: List<Attachment>
)

/** Resultado de búsqueda global. */
data class SearchHit(
    val itemId: Long,
    val categoryId: Long,
    val title: String,
    val description: String,
    val createdDate: Long,
    val categoryName: String,
    val categoryColorTag: Int,
    val attachmentCount: Int
)
