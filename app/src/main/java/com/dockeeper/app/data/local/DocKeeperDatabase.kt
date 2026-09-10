package com.dockeeper.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.dockeeper.app.data.local.dao.AttachmentDao
import com.dockeeper.app.data.local.dao.CategoryDao
import com.dockeeper.app.data.local.dao.ItemDao
import com.dockeeper.app.data.local.entity.AttachmentEntity
import com.dockeeper.app.data.local.entity.CategoryEntity
import com.dockeeper.app.data.local.entity.ItemEntity

@Database(
    entities = [
        CategoryEntity::class,
        ItemEntity::class,
        AttachmentEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class DocKeeperDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun itemDao(): ItemDao
    abstract fun attachmentDao(): AttachmentDao

    companion object {
        const val DATABASE_NAME = "dockeeper.db"
    }
}
