package com.diarymind.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.diarymind.domain.model.DiaryEntry
import com.diarymind.domain.model.DiaryWithFragments
import kotlinx.coroutines.flow.Flow

@Dao
interface DiaryDao {
    @Query("SELECT * FROM diary_entries ORDER BY date DESC")
    fun getAllDiaries(): Flow<List<DiaryEntry>>

    @Query("SELECT * FROM diary_entries WHERE date = :date")
    suspend fun getDiaryByDate(date: String): DiaryEntry?

    @Query("SELECT * FROM diary_entries WHERE id = :id")
    suspend fun getDiaryById(id: Long): DiaryEntry?

    @Transaction
    @Query("SELECT * FROM diary_entries WHERE id = :diaryId")
    suspend fun getDiaryWithFragments(diaryId: Long): DiaryWithFragments?

    @Insert
    suspend fun insert(diary: DiaryEntry): Long

    @Update
    suspend fun update(diary: DiaryEntry)

    @Delete
    suspend fun delete(diary: DiaryEntry)

    @Query("DELETE FROM diary_entries WHERE id = :id")
    suspend fun deleteById(id: Long)
}
