package com.diarymind.domain.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "perma_scores",
    foreignKeys = [
        ForeignKey(
            entity = DiaryEntry::class,
            parentColumns = ["id"],
            childColumns = ["diaryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["diaryId"])]
)
data class PermaScore(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val diaryId: Long,
    val positiveEmotion: Float,
    val engagement: Float,
    val relationships: Float,
    val meaning: Float,
    val accomplishment: Float,
    val aiReview: String,
    val suggestions: String,
    val createdAt: Long = System.currentTimeMillis()
)
