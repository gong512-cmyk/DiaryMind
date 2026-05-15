package com.diarymind.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.diarymind.domain.model.DiaryEntry
import com.diarymind.domain.model.Fragment
import com.diarymind.domain.model.FragmentDiaryCrossRef
import com.diarymind.domain.model.PermaScore

@Database(
    entities = [Fragment::class, DiaryEntry::class, PermaScore::class, FragmentDiaryCrossRef::class],
    version = 5,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class DiaryDatabase : RoomDatabase() {
    abstract fun fragmentDao(): FragmentDao
    abstract fun diaryDao(): DiaryDao
    abstract fun permaDao(): PermaDao
    abstract fun crossRefDao(): FragmentDiaryCrossRefDao

    companion object {
        @Volatile
        private var INSTANCE: DiaryDatabase? = null

        internal val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS fragment_diary_cross_ref_new (
                        fragmentId INTEGER NOT NULL,
                        diaryId INTEGER NOT NULL,
                        PRIMARY KEY(fragmentId, diaryId),
                        FOREIGN KEY(fragmentId) REFERENCES fragments(id) ON DELETE CASCADE,
                        FOREIGN KEY(diaryId) REFERENCES diary_entries(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO fragment_diary_cross_ref_new(fragmentId, diaryId)
                    SELECT c.fragmentId, c.diaryId
                    FROM fragment_diary_cross_ref c
                    INNER JOIN fragments f ON f.id = c.fragmentId
                    INNER JOIN diary_entries d ON d.id = c.diaryId
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE fragment_diary_cross_ref")
                db.execSQL("ALTER TABLE fragment_diary_cross_ref_new RENAME TO fragment_diary_cross_ref")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_fragment_diary_cross_ref_diaryId ON fragment_diary_cross_ref(diaryId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_fragment_diary_cross_ref_fragmentId ON fragment_diary_cross_ref(fragmentId)")
            }
        }

        internal val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE fragments ADD COLUMN imagePaths TEXT DEFAULT NULL")
            }
        }

        internal val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE diary_entries ADD COLUMN rating INTEGER DEFAULT NULL")
            }
        }

        internal val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE diary_entries ADD COLUMN ratingReason TEXT DEFAULT NULL")
            }
        }

        fun getDatabase(context: Context): DiaryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DiaryDatabase::class.java,
                    "diary_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
