package com.diarymind.domain.usecase

import android.content.Context
import com.diarymind.domain.model.DiaryEntry
import com.diarymind.domain.model.PermaScore
import com.diarymind.domain.model.toStars
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MarkdownExporter @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun export(diary: DiaryEntry, permaScore: PermaScore?): File? {
        val fileName = buildFileName(diary)
        val markdown = buildMarkdown(diary, permaScore)

        return try {
            val file = File(context.filesDir, "diaries/$fileName")
            file.parentFile?.mkdirs()
            file.writeText(markdown, Charsets.UTF_8)
            file
        } catch (e: Exception) {
            null
        }
    }

    private fun buildFileName(diary: DiaryEntry): String {
        val titleWithoutDate = diary.title
            .removePrefix(diary.date)
            .trim()
        val safeTitle = titleWithoutDate
            .replace(" ", "-")
            .replace(Regex("[^\\w\\-\\p{IsHan}]"), "")
            .take(30)
        return "${diary.date}-${safeTitle}.md"
    }

    private fun buildMarkdown(diary: DiaryEntry, permaScore: PermaScore?): String {
        val avgScore = permaScore?.let {
            ((it.positiveEmotion + it.engagement + it.relationships + it.meaning + it.accomplishment) / 5.0)
        }

        val ratingStars = diary.rating.toStars()

        val frontMatter = buildString {
            appendLine("---")
            appendLine("date: ${diary.date}")
            appendLine("tags: [diary, perma]")
            avgScore?.let { appendLine(String.format("mood_score: %.1f", it)) }
            diary.rating?.let { appendLine("rating: $it") }
            appendLine("---")
            appendLine()
        }

        val header = "# ${diary.title}" +
                if (ratingStars.isNotEmpty()) "  $ratingStars" else "" +
                "\n\n"

        val body = "## 日记正文\n\n${diary.content}\n\n"

        val permaSection = permaScore?.let {
            """
            |## PERMA 评估
            |
            || 维度 | 评分 | 说明 |
            ||------|:----:|------|
            || 积极情绪 | ${it.positiveEmotion}/10 | |
            || 投入 | ${it.engagement}/10 | |
            || 关系 | ${it.relationships}/10 | |
            || 意义 | ${it.meaning}/10 | |
            || 成就 | ${it.accomplishment}/10 | |
            |
            |## AI 评价
            |
            |${it.aiReview}
            |
            |## 明日建议
            |
            |${it.suggestions}
            |
            """.trimMargin()
        } ?: ""

        return frontMatter + header + body + permaSection
    }
}
