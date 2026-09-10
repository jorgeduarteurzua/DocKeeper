package com.dockeeper.app.data.local.relation

import androidx.room.Embedded
import com.dockeeper.app.data.local.entity.ItemEntity

/**
 * Item junto al número de adjuntos que contiene (calculado por consulta).
 */
data class ItemWithCount(
    @Embedded
    val item: ItemEntity,
    val attachmentCount: Int
)
