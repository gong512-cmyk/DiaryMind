package com.diarymind.domain.usecase

import com.diarymind.data.repository.DiaryRepository
import com.diarymind.domain.model.DiaryEntry
import com.diarymind.domain.model.Fragment
import com.diarymind.domain.model.PermaScore
import com.diarymind.domain.model.PipelineStep
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PipelineOrchestrator @Inject constructor(
    private val repository: DiaryRepository,
    private val aiProcessor: DiaryAIProcessor,
    private val markdownExporter: MarkdownExporter
) {

    suspend fun executePipeline(fragments: List<Fragment>, forceOverwrite: Boolean = false): Flow<PipelineState> = flow {
        emit(PipelineState.Running("开始处理..."))
        var createdDiaryId: Long? = null

        try {
            val diaryDate = fragments.minOfOrNull { it.createdAt }
                ?.let { timestamp ->
                    Instant.ofEpochMilli(timestamp)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                }
                ?: LocalDate.now()
            val dateStr = diaryDate.format(DateTimeFormatter.ISO_DATE)

            // Step 1: Preprocess
            emit(PipelineState.Running("整理碎片..."))
            val processedFragments = aiProcessor.preprocess(fragments)
            fragments.zip(processedFragments).forEach { (original, _) ->
                repository.updateFragmentStep(original.id, PipelineStep.PREPROCESSED)
            }

            // Step 2: Assess quality
            emit(PipelineState.Running("评估素材质量..."))
            val newQuality = aiProcessor.assessQuality(processedFragments)
            val oldDiary = if (forceOverwrite) repository.getDiaryByDate(dateStr) else null
            val (finalRating, finalReason) = if (oldDiary != null && (oldDiary.rating ?: 0) >= newQuality.rating) {
                Pair(oldDiary.rating, oldDiary.ratingReason)
            } else {
                Pair(newQuality.rating.takeIf { it > 0 }, newQuality.reason)
            }

            // Step 3: Generate diary
            emit(PipelineState.Running("生成日记..."))
            val diaryContent = aiProcessor.generateDiary(processedFragments)
            val wordCount = diaryContent.length

            val title = extractTitle(diaryContent, dateStr)

            val diary = DiaryEntry(
                date = dateStr,
                title = title,
                content = diaryContent,
                wordCount = wordCount,
                isPaginated = wordCount > 5000,
                totalPages = if (wordCount > 5000) (wordCount / 5000) + 1 else 1,
                rating = finalRating,
                ratingReason = finalReason
            )

            // Step 4: Assess PERMA
            emit(PipelineState.Running("分析心理状态..."))
            val permaResult = aiProcessor.assessPERMA(diaryContent)

            val existingDiaryId = if (forceOverwrite) {
                repository.getDiaryByDate(dateStr)?.id
            } else {
                null
            }

            val diaryId = repository.addDiary(diary)
            createdDiaryId = diaryId
            existingDiaryId?.let { repository.deleteDiaryWithDependencies(it) }

            // Link fragments to diary
            fragments.forEach { fragment ->
                repository.linkFragmentToDiary(fragment.id, diaryId)
                repository.updateFragmentStep(fragment.id, PipelineStep.GENERATED)
            }

            val permaScore = PermaScore(
                diaryId = diaryId,
                positiveEmotion = permaResult.positiveEmotion,
                engagement = permaResult.engagement,
                relationships = permaResult.relationships,
                meaning = permaResult.meaning,
                accomplishment = permaResult.accomplishment,
                aiReview = permaResult.aiReview,
                suggestions = permaResult.suggestions
            )
            repository.addPermaScore(permaScore)

            // Export to Markdown
            val exportedFile = markdownExporter.export(diary, permaScore)
            exportedFile?.let {
                repository.updateDiary(diary.copy(localPath = it.absolutePath))
            }

            // Mark all fragments as completed
            fragments.forEach { fragment ->
                repository.updateFragmentStep(fragment.id, PipelineStep.COMPLETED)
            }

            emit(PipelineState.Success(diaryId))

        } catch (e: Exception) {
            createdDiaryId?.let { diaryId ->
                runCatching { repository.deleteDiaryWithDependencies(diaryId) }
            }
            fragments.forEach { fragment ->
                repository.updateFragmentStep(fragment.id, PipelineStep.FAILED)
            }
            emit(PipelineState.Error(e.message ?: "Unknown error"))
        }
    }

    internal fun extractTitle(content: String, date: String): String {
        val compactDate = try {
            LocalDate.parse(date, DateTimeFormatter.ISO_DATE)
                .format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        } catch (_: Exception) {
            date.replace("-", "")
        }
        return "$compactDate 今天日记"
    }

    sealed class PipelineState {
        data class Running(val step: String) : PipelineState()
        data class Success(val diaryId: Long) : PipelineState()
        data class Error(val message: String) : PipelineState()
    }
}
