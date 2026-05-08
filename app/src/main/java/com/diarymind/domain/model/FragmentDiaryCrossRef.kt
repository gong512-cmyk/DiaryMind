package com.diarymind.domain.model

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "fragment_diary_cross_ref",
    primaryKeys = ["fragmentId", "diaryId"],
    indices = [
        Index(value = ["diaryId"]),
        Index(value = ["fragmentId"])
    ]
)
data class FragmentDiaryCrossRef(
    val fragmentId: Long,
    val diaryId: Long
)
