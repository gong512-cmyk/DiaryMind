package com.diarymind.ui.viewmodel

import com.diarymind.data.repository.DiaryRepository
import com.diarymind.domain.model.DiaryEntry
import com.diarymind.domain.model.Fragment
import com.diarymind.domain.model.PermaScore
import com.diarymind.domain.model.PipelineStep
import com.diarymind.domain.usecase.MarkdownExporter
import com.diarymind.domain.usecase.PipelineOrchestrator
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DiaryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: DiaryRepository
    private lateinit var pipelineOrchestrator: PipelineOrchestrator
    private lateinit var markdownExporter: MarkdownExporter
    private lateinit var viewModel: DiaryViewModel

    private val diariesFlow = MutableStateFlow<List<DiaryEntry>>(emptyList())
    private val fragmentsFlow = MutableStateFlow<List<Fragment>>(emptyList())
    private val permaScoresFlow = MutableStateFlow<List<PermaScore>>(emptyList())

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        pipelineOrchestrator = mockk(relaxed = true)
        markdownExporter = mockk(relaxed = true)

        every { repository.allDiaries } returns diariesFlow
        every { repository.todayFragments } returns fragmentsFlow
        every { repository.allPermaScores } returns permaScoresFlow

        viewModel = DiaryViewModel(repository, pipelineOrchestrator, markdownExporter)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init loads diaries fragments and perma scores`() = runTest {
        val diary = DiaryEntry(id = 1, date = "2026-05-07", title = "Test", content = "Content", wordCount = 1)
        val fragment = Fragment(id = 1, content = "fragment")
        val perma = PermaScore(id = 1, diaryId = 1, positiveEmotion = 8f, engagement = 7f, relationships = 6f, meaning = 9f, accomplishment = 5f, aiReview = "", suggestions = "")

        diariesFlow.emit(listOf(diary))
        fragmentsFlow.emit(listOf(fragment))
        permaScoresFlow.emit(listOf(perma))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf(diary), state.diaries)
        assertEquals(listOf(fragment), state.fragments)
        assertEquals(mapOf(1L to perma), state.permaScores)
    }

    @Test
    fun `addFragment updates loading and error states`() = runTest {
        coEvery { repository.addFragment("new fragment") } returns 1L

        viewModel.addFragment("new fragment")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        coVerify { repository.addFragment("new fragment") }
    }

    @Test
    fun `addFragment sets error on failure`() = runTest {
        val errorMessage = "Database error"
        coEvery { repository.addFragment(any()) } throws RuntimeException(errorMessage)

        viewModel.addFragment("new fragment")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(errorMessage, state.error)
    }

    @Test
    fun `generateDiary shows error when absolutely no incomplete fragments`() = runTest {
        coEvery { repository.getFragmentsByDate(any()) } returns emptyList()
        coEvery { repository.getAllIncompleteFragments() } returns emptyList()
        coEvery { repository.getDiaryByDate(any()) } returns null

        viewModel.generateDiary()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("没有可生成日记的碎片", state.error)
        assertFalse(state.isLoading)
    }

    @Test
    fun `generateDiary falls back to incomplete fragments when no today fragments`() = runTest {
        val fallbackFragments = listOf(
            Fragment(id = 2, content = "yesterday", pipelineStep = PipelineStep.IDLE)
        )
        coEvery { repository.getFragmentsByDate(any()) } returns emptyList()
        coEvery { repository.getAllIncompleteFragments() } returns fallbackFragments
        coEvery { repository.getDiaryByDate(any()) } returns null
        coEvery { pipelineOrchestrator.executePipeline(fallbackFragments, false) } returns flowOf(
            PipelineOrchestrator.PipelineState.Success(99L)
        )

        viewModel.generateDiary()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("日记生成完成！", state.pipelineStep)
        assertEquals(99L, state.generatedDiaryId)
        coVerify { pipelineOrchestrator.executePipeline(fallbackFragments, false) }
    }

    @Test
    fun `generateDiary collects pipeline states and sets success`() = runTest {
        val fragments = listOf(
            Fragment(id = 1, content = "morning", pipelineStep = PipelineStep.IDLE)
        )
        coEvery { repository.getFragmentsByDate(any()) } returns fragments
        coEvery { repository.getDiaryByDate(any()) } returns null
        coEvery { pipelineOrchestrator.executePipeline(fragments, false) } returns flowOf(
            PipelineOrchestrator.PipelineState.Running("开始处理..."),
            PipelineOrchestrator.PipelineState.Running("生成日记..."),
            PipelineOrchestrator.PipelineState.Success(42L)
        )

        viewModel.generateDiary()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("日记生成完成！", state.pipelineStep)
        assertEquals(42L, state.generatedDiaryId)
        assertNull(state.error)
    }

    @Test
    fun `generateDiary collects pipeline error state`() = runTest {
        val fragments = listOf(
            Fragment(id = 1, content = "morning", pipelineStep = PipelineStep.IDLE)
        )
        coEvery { repository.getFragmentsByDate(any()) } returns fragments
        coEvery { repository.getDiaryByDate(any()) } returns null
        coEvery { pipelineOrchestrator.executePipeline(fragments, false) } returns flowOf(
            PipelineOrchestrator.PipelineState.Running("开始处理..."),
            PipelineOrchestrator.PipelineState.Error("AI failed")
        )

        viewModel.generateDiary()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("生成失败", state.pipelineStep)
        assertEquals("AI failed", state.error)
    }

    @Test
    fun `generateDiary filters out completed fragments`() = runTest {
        val fragments = listOf(
            Fragment(id = 1, content = "done", pipelineStep = PipelineStep.COMPLETED),
            Fragment(id = 2, content = "pending", pipelineStep = PipelineStep.IDLE)
        )
        coEvery { repository.getFragmentsByDate(any()) } returns fragments
        coEvery { repository.getDiaryByDate(any()) } returns null
        coEvery { pipelineOrchestrator.executePipeline(listOf(fragments[1]), false) } returns flowOf(
            PipelineOrchestrator.PipelineState.Success(1L)
        )

        viewModel.generateDiary()
        advanceUntilIdle()

        coVerify { pipelineOrchestrator.executePipeline(listOf(fragments[1]), false) }
    }

    @Test
    fun `deleteFragment calls repository`() = runTest {
        val fragment = Fragment(id = 1, content = "to delete")

        viewModel.deleteFragment(fragment)
        advanceUntilIdle()

        coVerify { repository.deleteFragment(fragment) }
    }

    @Test
    fun `deleteDiary calls repository`() = runTest {
        val diary = DiaryEntry(id = 1, date = "2026-05-07", title = "T", content = "C", wordCount = 1)

        viewModel.deleteDiary(diary)
        advanceUntilIdle()

        coVerify { repository.deleteDiary(diary) }
    }

    @Test
    fun `showError sets error message`() {
        viewModel.showError("error msg")
        assertEquals("error msg", viewModel.uiState.value.error)
    }

    @Test
    fun `clearError clears error message`() {
        viewModel.showError("error")
        viewModel.clearError()
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `exportDiary calls markdownExporter`() {
        val diary = DiaryEntry(id = 1, date = "2026-05-07", title = "T", content = "C", wordCount = 1)
        val perma = PermaScore(id = 1, diaryId = 1, positiveEmotion = 8f, engagement = 7f, relationships = 6f, meaning = 9f, accomplishment = 5f, aiReview = "", suggestions = "")
        every { markdownExporter.export(diary, perma) } returns mockk(relaxed = true)

        viewModel.exportDiary(diary, perma)

        verify { markdownExporter.export(diary, perma) }
    }
}
