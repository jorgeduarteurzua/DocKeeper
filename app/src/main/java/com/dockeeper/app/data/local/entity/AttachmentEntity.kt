package com.dockeeper.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Un adjunto es cada archivo/hoja/link individual dentro de un item.
 * - Para DOCUMENT/PHOTO: [filePath] apunta al archivo en almacenamiento interno.
 * - Para LINK/QR: [linkUrl] guarda el contenido.
 */
@Entity(
    tableName = "attachments",
    foreignKeys = [
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("itemId")]
)
data class AttachmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val itemId: Long,
    val type: AttachmentType,
    /** Nombre visible del adjunto (nombre de archivo o título dado por el usuario). */
    val displayName: String = "",
    /** Ruta absoluta al archivo local. Null para LINK/QR. */
    val filePath: String? = null,
    /** Contenido de link o QR. Null para DOCUMENT/PHOTO. */
    val linkUrl: String? = null,
    /** MIME type del archivo (ej: image/jpeg, application/pdf). Null para LINK/QR. */
    val mimeType: String? = null,
    /** Orden dentro del item (hoja 1, hoja 2, ...). */
    val orderIndex: Int = 0,
    val createdDate: Long = System.currentTimeMillis()
)
