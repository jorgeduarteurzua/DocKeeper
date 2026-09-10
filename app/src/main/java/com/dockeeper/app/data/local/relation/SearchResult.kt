package com.dockeeper.app.data.local.relation

/**
 * Resultado de búsqueda: un item con datos de su categoría y su conteo de adjuntos.
 */
data class SearchResult(
    val itemId: Long,
    val categoryId: Long,
    val title: String,
    val description: String,
    val createdDate: Long,
    val categoryName: String,
    val categoryColorTag: Int,
    val attachmentCount: Int
)
