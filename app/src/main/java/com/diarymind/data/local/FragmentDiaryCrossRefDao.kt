package com.diarymind.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.diarymind.domain.model.FragmentDiaryCrossRef

@Dao
interface FragmentDiaryCrossRefDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(crossRef: FragmentDiaryCrossRef)

    @Query("SELECT * FROM fragment_diary_cross_ref WHERE diaryId = :diaryId")
    suspend fun getCrossRefsForDiary(diaryId: Long): List<FragmentDiaryCrossRef>

    @Query("DELETE FROM fragment_diary_cross_ref WHERE diaryId = :diaryId")
    suspend fun deleteCrossRefsForDiary(diaryId: Long)
}
