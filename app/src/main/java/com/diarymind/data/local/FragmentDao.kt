package com.diarymind.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.diarymind.domain.model.Fragment
import com.diarymind.domain.model.PipelineStep
import kotlinx.coroutines.flow.Flow

@Dao
interface FragmentDao {
    @Query("SELECT * FROM fragments ORDER BY createdAt DESC")
    fun getAllFragments(): Flow<List<Fragment>>

    @Query("SELECT * FROM fragments WHERE pipelineStep = :step ORDER BY createdAt DESC")
    fun getFragmentsByStep(step: PipelineStep): Flow<List<Fragment>>

    @Query("SELECT * FROM fragments WHERE date(createdAt/1000, 'unixepoch') = :date ORDER BY createdAt ASC")
    suspend fun getFragmentsByDate(date: String): List<Fragment>

    @Query("SELECT * FROM fragments WHERE id = :id")
    suspend fun getFragmentById(id: Long): Fragment?

    @Insert
    suspend fun insert(fragment: Fragment): Long

    @Update
    suspend fun update(fragment: Fragment)

    @Delete
    suspend fun delete(fragment: Fragment)

    @Query("DELETE FROM fragments WHERE id = :id")
    suspend fun deleteById(id: Long)
}
