package com.diarymind.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fragments")
data class Fragment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val content: String,
    val type: FragmentType = FragmentType.TEXT,
    val sourceApp: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val pipelineStep: PipelineStep = PipelineStep.IDLE
)

enum class FragmentType {
    TEXT, VOICE
}

enum class PipelineStep {
    IDLE,
    PREPROCESSED,
    CLUSTERED,
    ASSESSED,
    GENERATED,
    COMPLETED,
    FAILED
}
