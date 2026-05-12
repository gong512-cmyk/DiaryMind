package com.diarymind.domain.usecase

import com.diarymind.data.repository.DiaryRepository
import com.diarymind.domain.model.DiaryEntry
import com.diarymind.domain.model.Fragment
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PipelineOrchestratorTest {

    private lateinit var repository: DiaryRepository
    private lateinit var aiProcessor: DiaryAIProcessor
    private lateinit var markdownExporter: MarkdownExporter
    private lateinit var orchestrator: PipelineOrchestrator

    @Before
    fun setUp() {
        repository = mockk(relaxed = true)
        aiProcessor = mockk(relaxed = true)
        markdownExporter = mockk(relaxed = true)
        orchestrator = PipelineOrchestrator(repository, aiProcessor, markdownExporter)
    }

    @Test
    fun `executePipeline emits running states then success`() = runTest {
        val fragments = listOf(
            Fragment(id = 1, content = "早上吃了早餐"),
            Fragment(id = 2, content = "下午去公园散步")
        )
        val processed = listOf(
            ProcessedFragment(1, "早上吃了早餐", emptyList(), "neutral"),
            ProcessedFragment(2, "下午去公园散步", emptyList(), "neutral")
        )
        val diaryContent = "今天是美好的一天。"
        val permaResult = PermaScoreResult(
            positiveEmotion = 8f,
            engagement = 7f,
            relationships = 6f,
            meaning = 9f,
            accomplishment = 5f,
            aiReview = "很好",
            suggestions = "继续保持"
        )

        coEvery { aiProcessor.preprocess(fragments) } returns processed
        coEvery { aiProcessor.assessQuality(processed) } returns 3
        coEvery { aiProcessor.generateDiary(processed) } returns diaryContent
        coEvery { repository.addDiary(any()) } returns 100L
        coEvery { aiProcessor.assessPERMA(diaryContent) } returns permaResult
        coEvery { markdownExporter.export(any(), any()) } returns null

        val states = orchestrator.executePipeline(fragments).toList()

        assertTrue(states.first() is PipelineOrchestrator.PipelineState.Running)
        assertTrue(states.last() is PipelineOrchestrator.PipelineState.Success)
        val success = states.last() as PipelineOrchestrator.PipelineState.Success
        assertEquals(100L, success.diaryId)

        coVerify { aiProcessor.preprocess(fragments) }
        coVerify { aiProcessor.generateDiary(processed) }
        coVerify { aiProcessor.assessPERMA(diaryContent) }
        coVerify { repository.addDiary(any()) }
    }

    @Test
    fun `executePipeline emits error when AI fails`() = runTest {
        val fragments = listOf(Fragment(id = 1, content = "test"))
        val errorMessage = "Network timeout"

        coEvery { aiProcessor.preprocess(any()) } throws RuntimeException(errorMessage)

        val states = orchestrator.executePipeline(fragments).toList()

        assertTrue(states.last() is PipelineOrchestrator.PipelineState.Error)
        val error = states.last() as PipelineOrchestrator.PipelineState.Error
        assertEquals(errorMessage, error.message)

        coVerify { repository.updateFragmentStep(1, com.diarymind.domain.model.PipelineStep.FAILED) }
    }

    @Test
    fun `executePipeline keeps existing diary when overwrite AI assessment fails`() = runTest {
        val fragments = listOf(Fragment(id = 1, content = "test"))
        val processed = listOf(ProcessedFragment(1, "test", emptyList(), "neutral"))
        val errorMessage = "PERMA failed"

        coEvery { aiProcessor.preprocess(fragments) } returns processed
        coEvery { aiProcessor.generateDiary(processed) } returns "generated diary"
        coEvery { aiProcessor.assessPERMA("generated diary") } throws RuntimeException(errorMessage)

        val states = orchestrator.executePipeline(fragments, forceOverwrite = true).toList()

        assertTrue(states.last() is PipelineOrchestrator.PipelineState.Error)
        val error = states.last() as PipelineOrchestrator.PipelineState.Error
        assertEquals(errorMessage, error.message)
        coVerify(exactly = 1) { repository.getDiaryByDate(any()) }
        coVerify(exactly = 0) { repository.deleteDiaryWithDependencies(any()) }
        coVerify(exactly = 0) { repository.addDiary(any()) }
    }

    @Test
    fun `executePipeline rolls back created diary when later write fails`() = runTest {
        val fragments = listOf(Fragment(id = 1, content = "test"))
        val processed = listOf(ProcessedFragment(1, "test", emptyList(), "neutral"))
        val permaResult = PermaScoreResult(
            positiveEmotion = 8f,
            engagement = 7f,
            relationships = 6f,
            meaning = 9f,
            accomplishment = 5f,
            aiReview = "很好",
            suggestions = "继续保持"
        )

        coEvery { aiProcessor.preprocess(fragments) } returns processed
        coEvery { aiProcessor.generateDiary(processed) } returns "generated diary"
        coEvery { aiProcessor.assessPERMA("generated diary") } returns permaResult
        coEvery { repository.getDiaryByDate(any()) } returns DiaryEntry(
            id = 10,
            date = "2026-05-07",
            title = "old",
            content = "old"
        )
        coEvery { repository.addDiary(any()) } returns 100L
        coEvery { repository.addPermaScore(any()) } throws RuntimeException("DB write failed")

        val states = orchestrator.executePipeline(fragments, forceOverwrite = true).toList()

        assertTrue(states.last() is PipelineOrchestrator.PipelineState.Error)
        coVerify { repository.deleteDiaryWithDependencies(10L) }
        coVerify { repository.deleteDiaryWithDependencies(100L) }
        coVerify { repository.updateFragmentStep(1, com.diarymind.domain.model.PipelineStep.FAILED) }
    }

    @Test
    fun `executePipeline keeps existing diary when replacement insert fails`() = runTest {
        val fragments = listOf(Fragment(id = 1, content = "test"))
        val processed = listOf(ProcessedFragment(1, "test", emptyList(), "neutral"))
        val permaResult = PermaScoreResult(
            positiveEmotion = 8f,
            engagement = 7f,
            relationships = 6f,
            meaning = 9f,
            accomplishment = 5f,
            aiReview = "很好",
            suggestions = "继续保持"
        )

        coEvery { aiProcessor.preprocess(fragments) } returns processed
        coEvery { aiProcessor.generateDiary(processed) } returns "generated diary"
        coEvery { aiProcessor.assessPERMA("generated diary") } returns permaResult
        coEvery { repository.getDiaryByDate(any()) } returns DiaryEntry(
            id = 10,
            date = "2026-05-07",
            title = "old",
            content = "old"
        )
        coEvery { repository.addDiary(any()) } throws RuntimeException("insert failed")

        val states = orchestrator.executePipeline(fragments, forceOverwrite = true).toList()

        assertTrue(states.last() is PipelineOrchestrator.PipelineState.Error)
        coVerify(exactly = 0) { repository.deleteDiaryWithDependencies(10L) }
    }

    @Test
    fun `extractTitle returns fixed format yyyyMMdd 今天日记`() {
        val title = orchestrator.extractTitle("ignored content", "2026-05-07")
        assertEquals("20260507 今天日记", title)
    }

    @Test
    fun `extractTitle handles malformed date gracefully`() {
        val title = orchestrator.extractTitle("ignored", "invalid")
        assertEquals("invalid 今天日记", title)
    }
}
