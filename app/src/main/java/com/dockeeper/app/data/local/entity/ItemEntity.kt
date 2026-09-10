package com.dockeeper.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Un item (o "registro") pertenece a una categoría y agrupa uno o varios adjuntos.
 * Ej: "Examen de Sangre - Marzo 2026".
 */
@Entity(
    tableName = "items",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("categoryId")]
)
data class ItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val categoryId: Long,
    val title: String,
    val description: String = "",
    val createdDate: Long = System.currentTimeMillis()
)
