package com.dockeeper.app.data.mapper

import com.dockeeper.app.data.local.entity.AttachmentEntity
import com.dockeeper.app.data.local.entity.CategoryEntity
import com.dockeeper.app.data.local.entity.ItemEntity
import com.dockeeper.app.data.local.relation.CategoryWithCount
import com.dockeeper.app.data.local.relation.ItemWithAttachments
import com.dockeeper.app.data.local.relation.ItemWithCount
import com.dockeeper.app.data.local.relation.SearchResult
import com.dockeeper.app.domain.model.Attachment
import com.dockeeper.app.domain.model.Category
import com.dockeeper.app.domain.model.Item
import com.dockeeper.app.domain.model.ItemDetail
import com.dockeeper.app.domain.model.SearchHit

fun CategoryWithCount.toDomain(): Category = Category(
    id = category.id,
    name = category.name,
    colorTag = category.colorTag,
    createdDate = category.createdDate,
    itemCount = itemCount
)

fun CategoryEntity.toDomain(itemCount: Int = 0): Category = Category(
    id = id,
    name = name,
    colorTag = colorTag,
    createdDate = createdDate,
    itemCount = itemCount
)

fun ItemWithCount.toDomain(): Item = Item(
    id = item.id,
    categoryId = item.categoryId,
    title = item.title,
    description = item.description,
    createdDate = item.createdDate,
    attachmentCount = attachmentCount
)

fun ItemEntity.toDomain(attachmentCount: Int = 0): Item = Item(
    id = id,
    categoryId = categoryId,
    title = title,
    description = description,
    createdDate = createdDate,
    attachmentCount = attachmentCount
)

fun AttachmentEntity.toDomain(): Attachment = Attachment(
    id = id,
    itemId = itemId,
    type = type,
    displayName = displayName,
    filePath = filePath,
    linkUrl = linkUrl,
    mimeType = mimeType,
    orderIndex = orderIndex,
    createdDate = createdDate
)

fun ItemWithAttachments.toDomain(): ItemDetail = ItemDetail(
    item = item.toDomain(attachmentCount = attachments.size),
    attachments = attachments
        .sortedWith(compareBy({ it.orderIndex }, { it.createdDate }))
        .map { it.toDomain() }
)

fun SearchResult.toDomain(): SearchHit = SearchHit(
    itemId = itemId,
    categoryId = categoryId,
    title = title,
    description = description,
    createdDate = createdDate,
    categoryName = categoryName,
    categoryColorTag = categoryColorTag,
    attachmentCount = attachmentCount
)
