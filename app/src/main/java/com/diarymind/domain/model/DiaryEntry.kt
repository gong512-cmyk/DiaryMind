package com.diarymind.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diary_entries")
data class DiaryEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String,
    val title: String,
    val content: String,
    val wordCount: Int = 0,
    val isPaginated: Boolean = false,
    val totalPages: Int = 1,
    val localPath: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
