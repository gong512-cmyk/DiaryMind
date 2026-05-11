package com.diarymind.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DiaryDatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        DiaryDatabase::class.java
    )

    @Test
    fun migrate1To2_preservesValidCrossRefsAndDropsOrphans() {
        helper.createDatabase(TEST_DB, 1).apply {
            createVersion1Schema()
            execSQL("INSERT INTO fragments(id, content, type, sourceApp, createdAt, pipelineStep) VALUES (1, 'fragment', 'TEXT', NULL, 1, 'IDLE')")
            execSQL("INSERT INTO diary_entries(id, date, title, content, wordCount, isPaginated, totalPages, localPath, createdAt) VALUES (1, '2026-05-07', 'title', 'content', 7, 0, 1, NULL, 1)")
            execSQL("INSERT INTO fragment_diary_cross_ref(fragmentId, diaryId) VALUES (1, 1)")
            execSQL("INSERT INTO fragment_diary_cross_ref(fragmentId, diaryId) VALUES (99, 1)")
            close()
        }

        val migrated = helper.runMigrationsAndValidate(
            TEST_DB,
            2,
            true,
            DiaryDatabase.MIGRATION_1_2
        )

        migrated.query("SELECT COUNT(*) FROM fragment_diary_cross_ref").use { cursor ->
            cursor.moveToFirst()
            assertEquals(1, cursor.getInt(0))
        }
        migrated.query("SELECT fragmentId, diaryId FROM fragment_diary_cross_ref").use { cursor ->
            cursor.moveToFirst()
            assertEquals(1, cursor.getLong(0))
            assertEquals(1, cursor.getLong(1))
        }
    }

    @Test
    fun migrate2To3_addsImagePathsColumn() {
        helper.createDatabase(TEST_DB, 2).apply {
            createVersion2Schema()
            execSQL("INSERT INTO fragments(id, content, type, sourceApp, createdAt, pipelineStep) VALUES (1, 'fragment', 'TEXT', NULL, 1, 'IDLE')")
            close()
        }

        val migrated = helper.runMigrationsAndValidate(
            TEST_DB,
            3,
            true,
            DiaryDatabase.MIGRATION_2_3
        )

        migrated.query("SELECT imagePaths FROM fragments WHERE id = 1").use { cursor ->
            cursor.moveToFirst()
            assertEquals(null, cursor.getString(0))
        }
    }

    private fun SupportSQLiteDatabase.createVersion1Schema() {
        execSQL(
            """
            CREATE TABLE IF NOT EXISTS fragments (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                content TEXT NOT NULL,
                type TEXT NOT NULL,
                sourceApp TEXT,
                createdAt INTEGER NOT NULL,
                pipelineStep TEXT NOT NULL
            )
            """.trimIndent()
        )
        execSQL(
            """
            CREATE TABLE IF NOT EXISTS diary_entries (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                date TEXT NOT NULL,
                title TEXT NOT NULL,
                content TEXT NOT NULL,
                wordCount INTEGER NOT NULL,
                isPaginated INTEGER NOT NULL,
                totalPages INTEGER NOT NULL,
                localPath TEXT,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
        execSQL(
            """
            CREATE TABLE IF NOT EXISTS perma_scores (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                diaryId INTEGER NOT NULL,
                positiveEmotion REAL NOT NULL,
                engagement REAL NOT NULL,
                relationships REAL NOT NULL,
                meaning REAL NOT NULL,
                accomplishment REAL NOT NULL,
                aiReview TEXT NOT NULL,
                suggestions TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                FOREIGN KEY(diaryId) REFERENCES diary_entries(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        execSQL("CREATE INDEX IF NOT EXISTS index_perma_scores_diaryId ON perma_scores(diaryId)")
        execSQL(
            """
            CREATE TABLE IF NOT EXISTS fragment_diary_cross_ref (
                fragmentId INTEGER NOT NULL,
                diaryId INTEGER NOT NULL,
                PRIMARY KEY(fragmentId, diaryId)
            )
            """.trimIndent()
        )
    }

    private fun SupportSQLiteDatabase.createVersion2Schema() {
        execSQL(
            """
            CREATE TABLE IF NOT EXISTS fragments (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                content TEXT NOT NULL,
                type TEXT NOT NULL,
                sourceApp TEXT,
                createdAt INTEGER NOT NULL,
                pipelineStep TEXT NOT NULL
            )
            """.trimIndent()
        )
        execSQL(
            """
            CREATE TABLE IF NOT EXISTS diary_entries (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                date TEXT NOT NULL,
                title TEXT NOT NULL,
                content TEXT NOT NULL,
                wordCount INTEGER NOT NULL,
                isPaginated INTEGER NOT NULL,
                totalPages INTEGER NOT NULL,
                localPath TEXT,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
        execSQL(
            """
            CREATE TABLE IF NOT EXISTS perma_scores (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                diaryId INTEGER NOT NULL,
                positiveEmotion REAL NOT NULL,
                engagement REAL NOT NULL,
                relationships REAL NOT NULL,
                meaning REAL NOT NULL,
                accomplishment REAL NOT NULL,
                aiReview TEXT NOT NULL,
                suggestions TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                FOREIGN KEY(diaryId) REFERENCES diary_entries(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        execSQL("CREATE INDEX IF NOT EXISTS index_perma_scores_diaryId ON perma_scores(diaryId)")
        execSQL(
            """
            CREATE TABLE IF NOT EXISTS fragment_diary_cross_ref (
                fragmentId INTEGER NOT NULL,
                diaryId INTEGER NOT NULL,
                PRIMARY KEY(fragmentId, diaryId),
                FOREIGN KEY(fragmentId) REFERENCES fragments(id) ON DELETE CASCADE,
                FOREIGN KEY(diaryId) REFERENCES diary_entries(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        execSQL("CREATE INDEX IF NOT EXISTS index_fragment_diary_cross_ref_diaryId ON fragment_diary_cross_ref(diaryId)")
        execSQL("CREATE INDEX IF NOT EXISTS index_fragment_diary_cross_ref_fragmentId ON fragment_diary_cross_ref(fragmentId)")
    }

    private companion object {
        const val TEST_DB = "migration-test"
    }
}
