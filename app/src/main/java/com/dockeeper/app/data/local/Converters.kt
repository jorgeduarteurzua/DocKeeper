package com.dockeeper.app.data.local

import androidx.room.TypeConverter
import com.dockeeper.app.data.local.entity.AttachmentType

class Converters {
    @TypeConverter
    fun fromAttachmentType(type: AttachmentType): String = type.name

    @TypeConverter
    fun toAttachmentType(value: String): AttachmentType = AttachmentType.valueOf(value)
}
