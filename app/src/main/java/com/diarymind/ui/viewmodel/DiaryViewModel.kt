package com.diarymind.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diarymind.data.repository.DiaryRepository
import com.diarymind.domain.model.DiaryEntry
import com.diarymind.domain.model.Fragment
import com.diarymind.domain.model.PermaScore
import com.diarymind.domain.usecase.MarkdownExporter
import com.diarymind.domain.usecase.PipelineOrchestrator
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class DiaryViewModel @Inject constructor(
    private val repository: DiaryRepository,
    private val pipelineOrchestrator: PipelineOrchestrator,
    private val markdownExporter: MarkdownExporter
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiaryUiState())
    val uiState: StateFlow<DiaryUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            repository.allDiaries.collect { diaries ->
                _uiState.update { it.copy(diaries = diaries) }
            }
        }
        viewModelScope.launch {
            repository.allFragments.collect { fragments ->
                _uiState.update { it.copy(fragments = fragments) }
            }
        }
        viewModelScope.launch {
            repository.allPermaScores.collect { scores ->
                _uiState.update { it.copy(permaScores = scores.associateBy { score -> score.diaryId }) }
            }
        }
    }

    fun addFragment(content: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                repository.addFragment(content)
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun generateDiary() {
        viewModelScope.launch {
            val today = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
            val todayFragments = repository.getFragmentsByDate(today)
                .filter { it.pipelineStep != com.diarymind.domain.model.PipelineStep.COMPLETED }

            if (todayFragments.isEmpty()) {
                _uiState.update { it.copy(error = "今天还没有记录碎片") }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, pipelineStep = "开始生成日记...", error = null) }

            pipelineOrchestrator.executePipeline(todayFragments).collect { state ->
                when (state) {
                    is PipelineOrchestrator.PipelineState.Running -> {
                        _uiState.update { it.copy(pipelineStep = state.step) }
                    }
                    is PipelineOrchestrator.PipelineState.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                pipelineStep = "日记生成完成！",
                                generatedDiaryId = state.diaryId
                            )
                        }
                    }
                    is PipelineOrchestrator.PipelineState.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                pipelineStep = "生成失败",
                                error = state.message
                            )
                        }
                    }
                }
            }
        }
    }

    fun deleteFragment(fragment: Fragment) {
        viewModelScope.launch {
            repository.deleteFragment(fragment)
        }
    }

    fun deleteDiary(diary: DiaryEntry) {
        viewModelScope.launch {
            repository.deleteDiary(diary)
        }
    }

    fun showError(message: String) {
        _uiState.update { it.copy(error = message) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun exportDiary(diary: DiaryEntry, permaScore: PermaScore?): File? {
        return markdownExporter.export(diary, permaScore)
    }

    data class DiaryUiState(
        val fragments: List<Fragment> = emptyList(),
        val diaries: List<DiaryEntry> = emptyList(),
        val permaScores: Map<Long, PermaScore> = emptyMap(),
        val isLoading: Boolean = false,
        val pipelineStep: String = "",
        val generatedDiaryId: Long? = null,
        val error: String? = null
    )
}
