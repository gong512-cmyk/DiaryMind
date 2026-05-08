package com.diarymind.domain.usecase

import com.diarymind.data.repository.DiaryRepository
import com.diarymind.domain.model.DiaryEntry
import com.diarymind.domain.model.Fragment
import com.diarymind.domain.model.FragmentDiaryCrossRef
import com.diarymind.domain.model.PermaScore
import com.diarymind.domain.model.PipelineStep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PipelineOrchestrator @Inject constructor(
    private val repository: DiaryRepository,
    private val aiProcessor: DiaryAIProcessor,
    private val markdownExporter: MarkdownExporter
) {

    suspend fun executePipeline(fragments: List<Fragment>): Flow<PipelineState> = flow {
        emit(PipelineState.Running("开始处理..."))

        try {
            // Step 1: Preprocess
            emit(PipelineState.Running("整理碎片..."))
            val processedFragments = aiProcessor.preprocess(fragments)
            fragments.zip(processedFragments).forEach { (original, _) ->
                repository.updateFragmentStep(original.id, PipelineStep.PREPROCESSED)
            }

            // Step 2: Generate diary
            emit(PipelineState.Running("生成日记..."))
            val diaryContent = aiProcessor.generateDiary(processedFragments)
            val wordCount = diaryContent.length

            val today = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
            val title = extractTitle(diaryContent, today)

            val diary = DiaryEntry(
                date = today,
                title = title,
                content = diaryContent,
                wordCount = wordCount,
                isPaginated = wordCount > 5000,
                totalPages = if (wordCount > 5000) (wordCount / 5000) + 1 else 1
            )
            val diaryId = repository.addDiary(diary)

            // Link fragments to diary
            fragments.forEach { fragment ->
                repository.linkFragmentToDiary(fragment.id, diaryId)
                repository.updateFragmentStep(fragment.id, PipelineStep.GENERATED)
            }

            // Step 3: Assess PERMA
            emit(PipelineState.Running("分析心理状态..."))
            val permaResult = aiProcessor.assessPERMA(diaryContent)
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
            fragments.forEach { fragment ->
                repository.updateFragmentStep(fragment.id, PipelineStep.FAILED)
            }
            emit(PipelineState.Error(e.message ?: "Unknown error"))
        }
    }

    internal fun extractTitle(content: String, date: String): String {
        val firstLine = content.lineSequence().firstOrNull()?.trim() ?: ""
        val summary = if (firstLine.length > 20) {
            firstLine.take(20) + "..."
        } else {
            firstLine.ifEmpty { "今日记录" }
        }
        return "$date $summary"
    }

    sealed class PipelineState {
        data class Running(val step: String) : PipelineState()
        data class Success(val diaryId: Long) : PipelineState()
        data class Error(val message: String) : PipelineState()
    }
}
