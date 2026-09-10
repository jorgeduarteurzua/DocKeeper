package com.dockeeper.app.data.repository

import com.dockeeper.app.data.file.FileManager
import com.dockeeper.app.data.local.dao.AttachmentDao
import com.dockeeper.app.data.local.dao.CategoryDao
import com.dockeeper.app.data.local.entity.CategoryEntity
import com.dockeeper.app.data.mapper.toDomain
import com.dockeeper.app.domain.model.Category
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao,
    private val attachmentDao: AttachmentDao,
    private val fileManager: FileManager
) {

    fun observeCategories(): Flow<List<Category>> =
        categoryDao.observeCategoriesWithCount().map { list -> list.map { it.toDomain() } }

    fun observeCategory(id: Long): Flow<Category?> =
        categoryDao.observeCategory(id).map { it?.toDomain() }

    suspend fun createCategory(name: String, colorTag: Int): Long =
        categoryDao.insert(CategoryEntity(name = name.trim(), colorTag = colorTag))

    suspend fun renameCategory(id: Long, name: String, colorTag: Int) {
        val existing = categoryDao.getCategory(id) ?: return
        categoryDao.update(existing.copy(name = name.trim(), colorTag = colorTag))
    }

    /**
     * Elimina la categoría, sus items y adjuntos (cascade en DB) y borra los archivos físicos.
     */
    suspend fun deleteCategory(id: Long) {
        val filePaths = attachmentDao.getFilePathsForCategory(id)
        categoryDao.deleteById(id)
        fileManager.deleteFiles(filePaths)
    }
}
