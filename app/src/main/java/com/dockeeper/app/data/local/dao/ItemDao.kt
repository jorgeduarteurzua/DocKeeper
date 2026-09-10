package com.dockeeper.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.dockeeper.app.data.local.entity.ItemEntity
import com.dockeeper.app.data.local.relation.ItemWithAttachments
import com.dockeeper.app.data.local.relation.ItemWithCount
import com.dockeeper.app.data.local.relation.SearchResult
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {

    @Query(
        """
        SELECT i.*, (
            SELECT COUNT(*) FROM attachments a WHERE a.itemId = i.id
        ) AS attachmentCount
        FROM items i
        WHERE i.categoryId = :categoryId
        ORDER BY i.createdDate DESC
        """
    )
    fun observeItemsWithCount(categoryId: Long): Flow<List<ItemWithCount>>

    @Transaction
    @Query("SELECT * FROM items WHERE id = :id")
    fun observeItemWithAttachments(id: Long): Flow<ItemWithAttachments?>

    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun getItem(id: Long): ItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ItemEntity): Long

    @Update
    suspend fun update(item: ItemEntity)

    @Query("DELETE FROM items WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * Búsqueda global por título/descripción de item o nombre de categoría.
     */
    @Query(
        """
        SELECT
            i.id AS itemId,
            i.categoryId AS categoryId,
            i.title AS title,
            i.description AS description,
            i.createdDate AS createdDate,
            c.name AS categoryName,
            c.colorTag AS categoryColorTag,
            (SELECT COUNT(*) FROM attachments a WHERE a.itemId = i.id) AS attachmentCount
        FROM items i
        INNER JOIN categories c ON c.id = i.categoryId
        WHERE i.title LIKE '%' || :query || '%'
           OR i.description LIKE '%' || :query || '%'
           OR c.name LIKE '%' || :query || '%'
        ORDER BY i.createdDate DESC
        """
    )
    fun searchItems(query: String): Flow<List<SearchResult>>
}
