package com.dockeeper.app.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.dockeeper.app.data.local.entity.AttachmentEntity
import com.dockeeper.app.data.local.entity.ItemEntity

/**
 * Item con todos sus adjuntos cargados.
 */
data class ItemWithAttachments(
    @Embedded
    val item: ItemEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "itemId"
    )
    val attachments: List<AttachmentEntity>
)
