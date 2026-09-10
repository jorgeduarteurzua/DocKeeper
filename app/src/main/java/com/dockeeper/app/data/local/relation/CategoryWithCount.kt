package com.dockeeper.app.data.local.relation

import androidx.room.Embedded
import com.dockeeper.app.data.local.entity.CategoryEntity

/**
 * Categoría junto al número de items que contiene (calculado por consulta).
 */
data class CategoryWithCount(
    @Embedded
    val category: CategoryEntity,
    val itemCount: Int
)
