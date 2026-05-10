package com.diarymind.domain.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "fragment_diary_cross_ref",
    primaryKeys = ["fragmentId", "diaryId"],
    foreignKeys = [
        ForeignKey(
            entity = Fragment::class,
            parentColumns = ["id"],
            childColumns = ["fragmentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = DiaryEntry::class,
            parentColumns = ["id"],
            childColumns = ["diaryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["diaryId"]),
        Index(value = ["fragmentId"])
    ]
)
data class FragmentDiaryCrossRef(
    val fragmentId: Long,
    val diaryId: Long
)
