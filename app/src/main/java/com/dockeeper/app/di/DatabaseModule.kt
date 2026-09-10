package com.dockeeper.app.di

import android.content.Context
import androidx.room.Room
import com.dockeeper.app.data.local.DocKeeperDatabase
import com.dockeeper.app.data.local.dao.AttachmentDao
import com.dockeeper.app.data.local.dao.CategoryDao
import com.dockeeper.app.data.local.dao.ItemDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): DocKeeperDatabase =
        Room.databaseBuilder(
            context,
            DocKeeperDatabase::class.java,
            DocKeeperDatabase.DATABASE_NAME
        ).build()

    @Provides
    fun provideCategoryDao(db: DocKeeperDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideItemDao(db: DocKeeperDatabase): ItemDao = db.itemDao()

    @Provides
    fun provideAttachmentDao(db: DocKeeperDatabase): AttachmentDao = db.attachmentDao()
}
