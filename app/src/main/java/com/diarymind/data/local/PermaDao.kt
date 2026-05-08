package com.diarymind.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.diarymind.domain.model.PermaScore
import kotlinx.coroutines.flow.Flow

@Dao
interface PermaDao {
    @Query("SELECT * FROM perma_scores WHERE diaryId = :diaryId")
    suspend fun getPermaScoreByDiaryId(diaryId: Long): PermaScore?

    @Query("SELECT * FROM perma_scores ORDER BY createdAt DESC")
    fun getAllPermaScores(): Flow<List<PermaScore>>

    @Insert
    suspend fun insert(permaScore: PermaScore): Long

    @Update
    suspend fun update(permaScore: PermaScore)

    @Query("DELETE FROM perma_scores WHERE diaryId = :diaryId")
    suspend fun deleteByDiaryId(diaryId: Long)
}
