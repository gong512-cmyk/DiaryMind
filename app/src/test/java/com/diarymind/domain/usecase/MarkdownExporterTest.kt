package com.diarymind.domain.usecase

import android.content.Context
import com.diarymind.domain.model.DiaryEntry
import com.diarymind.domain.model.PermaScore
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class MarkdownExporterTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var exporter: MarkdownExporter

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        every { context.filesDir } returns tempFolder.root
        exporter = MarkdownExporter(context)
    }

    @Test
    fun `export creates markdown file with correct structure`() {
        val diary = DiaryEntry(
            id = 1,
            date = "2026-05-07",
            title = "2026-05-07 测试日记",
            content = "今天过得不错。",
            wordCount = 7
        )
        val perma = PermaScore(
            id = 1,
            diaryId = 1,
            positiveEmotion = 8f,
            engagement = 7f,
            relationships = 6f,
            meaning = 9f,
            accomplishment = 5f,
            aiReview = "总体不错",
            suggestions = "早点休息"
        )

        val file = exporter.export(diary, perma)

        assertNotNull(file)
        assertTrue(file!!.exists())
        val content = file.readText()
        assertTrue(content.contains("---"))
        assertTrue(content.contains("date: 2026-05-07"))
        assertTrue(content.contains("tags: [diary, perma]"))
        assertTrue(content.contains("mood_score: 7.0"))
        assertTrue(content.contains("# 2026-05-07 测试日记"))
        assertTrue(content.contains("## 日记正文"))
        assertTrue(content.contains("今天过得不错。"))
        assertTrue(content.contains("## PERMA 评估"))
        assertTrue(content.contains("积极情绪 | 8.0/10"))
        assertTrue(content.contains("## AI 评价"))
        assertTrue(content.contains("总体不错"))
        assertTrue(content.contains("## 明日建议"))
        assertTrue(content.contains("早点休息"))
    }

    @Test
    fun `export without permaScore omits perma section`() {
        val diary = DiaryEntry(
            id = 2,
            date = "2026-05-08",
            title = "2026-05-08 无评估",
            content = "普通的一天。",
            wordCount = 5
        )

        val file = exporter.export(diary, null)

        assertNotNull(file)
        val content = file!!.readText()
        assertTrue(content.contains("# 2026-05-08 无评估"))
        assertTrue(content.contains("普通的一天。"))
        assertTrue(content.contains("mood_score").not())
        assertTrue(content.contains("PERMA 评估").not())
    }

    @Test
    fun `fileName sanitizes special characters`() {
        val diary = DiaryEntry(
            id = 3,
            date = "2026-05-09",
            title = "标题/有\\特殊!字符",
            content = "内容",
            wordCount = 2
        )

        val file = exporter.export(diary, null)

        assertNotNull(file)
        val name = file!!.name
        assertTrue(name.endsWith(".md"))
        assertTrue(name.startsWith("2026-05-09-"))
        assertEquals("2026-05-09-标题有特殊字符.md", name)
    }
}
