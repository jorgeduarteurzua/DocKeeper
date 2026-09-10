package com.dockeeper.app.data.repository

import android.net.Uri
import com.dockeeper.app.data.file.FileManager
import com.dockeeper.app.data.local.dao.AttachmentDao
import com.dockeeper.app.data.local.dao.ItemDao
import com.dockeeper.app.data.local.entity.AttachmentEntity
import com.dockeeper.app.data.local.entity.AttachmentType
import com.dockeeper.app.data.local.entity.ItemEntity
import com.dockeeper.app.data.mapper.toDomain
import com.dockeeper.app.domain.model.Item
import com.dockeeper.app.domain.model.ItemDetail
import com.dockeeper.app.domain.model.SearchHit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ItemRepository @Inject constructor(
    private val itemDao: ItemDao,
    private val attachmentDao: AttachmentDao,
    private val fileManager: FileManager
) {

    fun observeItems(categoryId: Long): Flow<List<Item>> =
        itemDao.observeItemsWithCount(categoryId).map { list -> list.map { it.toDomain() } }

    fun observeItemDetail(itemId: Long): Flow<ItemDetail?> =
        itemDao.observeItemWithAttachments(itemId).map { it?.toDomain() }

    fun search(query: String): Flow<List<SearchHit>> =
        itemDao.searchItems(query.trim()).map { list -> list.map { it.toDomain() } }

    suspend fun createItem(categoryId: Long, title: String, description: String): Long =
        itemDao.insert(
            ItemEntity(
                categoryId = categoryId,
                title = title.trim(),
                description = description.trim()
            )
        )

    suspend fun updateItem(itemId: Long, title: String, description: String) {
        val existing = itemDao.getItem(itemId) ?: return
        itemDao.update(existing.copy(title = title.trim(), description = description.trim()))
    }

    suspend fun deleteItem(itemId: Long) {
        val filePaths = attachmentDao.getFilePathsForItem(itemId)
        itemDao.deleteById(itemId)
        fileManager.deleteFiles(filePaths)
    }

    // ---- Adjuntos ----

    /** Importa un archivo (foto de galería o documento) desde un Uri y lo asocia al item. */
    suspend fun addFileAttachment(itemId: Long, uri: Uri, type: AttachmentType): Long {
        val imported = fileManager.importFromUri(uri)
        val nextOrder = attachmentDao.getMaxOrderIndex(itemId) + 1
        return attachmentDao.insert(
            AttachmentEntity(
                itemId = itemId,
                type = type,
                displayName = imported.displayName,
                filePath = imported.filePath,
                linkUrl = null,
                mimeType = imported.mimeType,
                orderIndex = nextOrder
            )
        )
    }

    /** Registra una foto ya capturada por la cámara (archivo interno ya creado). */
    suspend fun addCameraPhoto(itemId: Long, filePath: String): Long {
        val nextOrder = attachmentDao.getMaxOrderIndex(itemId) + 1
        return attachmentDao.insert(
            AttachmentEntity(
                itemId = itemId,
                type = AttachmentType.PHOTO,
                displayName = "Foto ${nextOrder + 1}",
                filePath = filePath,
                linkUrl = null,
                mimeType = "image/jpeg",
                orderIndex = nextOrder
            )
        )
    }

    /** Guarda un link (pegado a mano) o el contenido de un QR. */
    suspend fun addLinkAttachment(
        itemId: Long,
        content: String,
        displayName: String,
        type: AttachmentType
    ): Long {
        val nextOrder = attachmentDao.getMaxOrderIndex(itemId) + 1
        return attachmentDao.insert(
            AttachmentEntity(
                itemId = itemId,
                type = type,
                displayName = displayName.ifBlank { content.take(60) },
                filePath = null,
                linkUrl = content,
                mimeType = null,
                orderIndex = nextOrder
            )
        )
    }

    suspend fun deleteAttachment(attachmentId: Long) {
        val attachment = attachmentDao.getAttachment(attachmentId) ?: return
        attachmentDao.deleteById(attachmentId)
        fileManager.deleteFile(attachment.filePath)
    }

    suspend fun getAttachmentFilePaths(itemId: Long): List<String> =
        attachmentDao.getFilePathsForItem(itemId)
}
