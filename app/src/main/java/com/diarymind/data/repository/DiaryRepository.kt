package com.diarymind.data.repository

import com.diarymind.data.local.DiaryDao
import com.diarymind.data.local.FragmentDao
import com.diarymind.data.local.FragmentDiaryCrossRefDao
import com.diarymind.data.local.PermaDao
import com.diarymind.domain.model.DiaryEntry
import com.diarymind.domain.model.Fragment
import com.diarymind.domain.model.FragmentDiaryCrossRef
import com.diarymind.domain.model.PermaScore
import com.diarymind.domain.model.PipelineStep
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiaryRepository @Inject constructor(
    private val fragmentDao: FragmentDao,
    private val diaryDao: DiaryDao,
    private val permaDao: PermaDao,
    private val crossRefDao: FragmentDiaryCrossRefDao
) {
    // Fragment operations
    val allFragments: Flow<List<Fragment>> = fragmentDao.getAllFragments()

    val todayFragments: Flow<List<Fragment>>
        get() {
            val zoneId = ZoneId.systemDefault()
            val today = LocalDate.now()
            val startOfDay = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
            val endOfDay = today.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
            return fragmentDao.getTodayFragments(startOfDay, endOfDay)
        }

    suspend fun addFragment(content: String): Long {
        return fragmentDao.insert(Fragment(content = content))
    }

    suspend fun updateFragment(fragment: Fragment) {
        fragmentDao.update(fragment)
    }

    suspend fun deleteFragment(fragment: Fragment) {
        fragmentDao.delete(fragment)
    }

    suspend fun getFragmentsByDate(date: String): List<Fragment> {
        val formatter = DateTimeFormatter.ISO_DATE
        val localDate = LocalDate.parse(date, formatter)
        val zoneId = ZoneId.systemDefault()
        val startOfDay = localDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endOfDay = localDate.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        return fragmentDao.getFragmentsByDateRange(startOfDay, endOfDay)
    }

    suspend fun getFragmentById(id: Long): Fragment? {
        return fragmentDao.getFragmentById(id)
    }

    suspend fun getAllIncompleteFragments(): List<Fragment> {
        return fragmentDao.getAllFragmentsList()
            .filter { it.pipelineStep != PipelineStep.COMPLETED }
    }

    suspend fun getFragmentsForDiary(diaryId: Long): List<Fragment> {
        val crossRefs = crossRefDao.getCrossRefsForDiary(diaryId)
        return crossRefs.mapNotNull { fragmentDao.getFragmentById(it.fragmentId) }
    }

    // Diary operations
    val allDiaries: Flow<List<DiaryEntry>> = diaryDao.getAllDiaries()

    suspend fun addDiary(diary: DiaryEntry): Long {
        return diaryDao.insert(diary)
    }

    suspend fun getDiaryByDate(date: String): DiaryEntry? {
        return diaryDao.getDiaryByDate(date)
    }

    suspend fun getDiaryById(id: Long): DiaryEntry? {
        return diaryDao.getDiaryById(id)
    }

    suspend fun updateDiary(diary: DiaryEntry) {
        diaryDao.update(diary)
    }

    suspend fun deleteDiary(diary: DiaryEntry) {
        diaryDao.delete(diary)
    }

    suspend fun deleteDiaryWithDependencies(diaryId: Long) {
        crossRefDao.deleteCrossRefsForDiary(diaryId)
        permaDao.deleteByDiaryId(diaryId)
        diaryDao.deleteById(diaryId)
    }

    // PERMA operations
    val allPermaScores: Flow<List<PermaScore>> = permaDao.getAllPermaScores()

    suspend fun addPermaScore(permaScore: PermaScore): Long {
        return permaDao.insert(permaScore)
    }

    suspend fun getPermaScoreByDiaryId(diaryId: Long): PermaScore? {
        return permaDao.getPermaScoreByDiaryId(diaryId)
    }

    // Cross reference operations
    suspend fun linkFragmentToDiary(fragmentId: Long, diaryId: Long) {
        crossRefDao.insert(FragmentDiaryCrossRef(fragmentId, diaryId))
    }

    suspend fun updateFragmentStep(fragmentId: Long, step: PipelineStep) {
        fragmentDao.getFragmentById(fragmentId)?.let { fragment ->
            fragmentDao.update(fragment.copy(pipelineStep = step))
        }
    }
}
