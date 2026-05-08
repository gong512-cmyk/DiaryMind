package com.diarymind.domain.usecase

import com.diarymind.domain.model.DiaryEntry
import com.diarymind.domain.model.Fragment
import com.diarymind.domain.model.PermaScore

interface DiaryAIProcessor {
    suspend fun preprocess(fragments: List<Fragment>): List<ProcessedFragment>
    suspend fun assessPERMA(text: String): PermaScoreResult
    suspend fun generateDiary(fragments: List<ProcessedFragment>): String
    suspend fun generateReview(permaScore: PermaScoreResult): ReviewResult
}

data class ProcessedFragment(
    val originalId: Long,
    val content: String,
    val keywords: List<String>,
    val sentiment: String
)

data class PermaScoreResult(
    val positiveEmotion: Float,
    val engagement: Float,
    val relationships: Float,
    val meaning: Float,
    val accomplishment: Float,
    val aiReview: String,
    val suggestions: String
)

data class ReviewResult(
    val review: String,
    val suggestions: List<String>
)
