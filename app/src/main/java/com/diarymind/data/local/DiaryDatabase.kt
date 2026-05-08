package com.diarymind.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.diarymind.domain.model.DiaryEntry
import com.diarymind.domain.model.Fragment
import com.diarymind.domain.model.FragmentDiaryCrossRef
import com.diarymind.domain.model.PermaScore

@Database(
    entities = [Fragment::class, DiaryEntry::class, PermaScore::class, FragmentDiaryCrossRef::class],
    version = 1,
    exportSchema = false
)
abstract class DiaryDatabase : RoomDatabase() {
    abstract fun fragmentDao(): FragmentDao
    abstract fun diaryDao(): DiaryDao
    abstract fun permaDao(): PermaDao
    abstract fun crossRefDao(): FragmentDiaryCrossRefDao

    companion object {
        @Volatile
        private var INSTANCE: DiaryDatabase? = null

        fun getDatabase(context: Context): DiaryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DiaryDatabase::class.java,
                    "diary_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
