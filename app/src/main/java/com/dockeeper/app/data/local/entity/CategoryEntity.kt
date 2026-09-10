package com.dockeeper.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Una categoría agrupa items (registros). Ej: "Documentos Médicos".
 */
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    /** Índice del color en la paleta predefinida (ver CategoryColors). */
    val colorTag: Int = 0,
    /** Fecha de creación en milisegundos epoch. */
    val createdDate: Long = System.currentTimeMillis()
)
