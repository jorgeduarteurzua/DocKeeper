package com.dockeeper.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dockeeper.app.data.local.entity.AttachmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttachmentDao {

    @Query("SELECT * FROM attachments WHERE itemId = :itemId ORDER BY orderIndex ASC, createdDate ASC")
    fun observeAttachments(itemId: Long): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM attachments WHERE itemId = :itemId ORDER BY orderIndex ASC, createdDate ASC")
    suspend fun getAttachments(itemId: Long): List<AttachmentEntity>

    @Query("SELECT * FROM attachments WHERE id = :id")
    suspend fun getAttachment(id: Long): AttachmentEntity?

    @Query("SELECT filePath FROM attachments WHERE itemId = :itemId AND filePath IS NOT NULL")
    suspend fun getFilePathsForItem(itemId: Long): List<String>

    @Query("SELECT filePath FROM attachments WHERE itemId IN (SELECT id FROM items WHERE categoryId = :categoryId) AND filePath IS NOT NULL")
    suspend fun getFilePathsForCategory(categoryId: Long): List<String>

    @Query("SELECT COALESCE(MAX(orderIndex), -1) FROM attachments WHERE itemId = :itemId")
    suspend fun getMaxOrderIndex(itemId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(attachment: AttachmentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(attachments: List<AttachmentEntity>)

    @Query("DELETE FROM attachments WHERE id = :id")
    suspend fun deleteById(id: Long)
}
