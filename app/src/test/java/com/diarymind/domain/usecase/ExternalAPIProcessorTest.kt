package com.diarymind.domain.usecase

import android.content.Context
import com.diarymind.data.remote.ChatCompletionResponse
import com.diarymind.data.remote.Choice
import com.diarymind.data.remote.DeepSeekApi
import com.diarymind.data.remote.DynamicBaseUrlInterceptor
import com.diarymind.data.remote.Message
import com.diarymind.domain.model.Fragment
import com.diarymind.util.LlmConfig
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException

@OptIn(ExperimentalCoroutinesApi::class)
class ExternalAPIProcessorTest {

    private lateinit var deepSeekApi: DeepSeekApi
    private lateinit var baseUrlInterceptor: DynamicBaseUrlInterceptor
    private lateinit var context: Context
    private lateinit var processor: ExternalAPIProcessor

    @Before
    fun setUp() {
        deepSeekApi = mockk(relaxed = true)
        baseUrlInterceptor = mockk(relaxed = true)
        context = mockk(relaxed = true)
        processor = ExternalAPIProcessor(deepSeekApi, baseUrlInterceptor, context)
    }

    @Test
    fun `preprocess cleans filler words and extracts keywords`() = runTest {
        val fragments = listOf(
            Fragment(id = 1, content = "嗯啊今天 去了 公园，公园 很漂亮，公园 很大"),
            Fragment(id = 2, content = "和 朋友 一起 吃饭...")
        )

        val result = processor.preprocess(fragments)

        assertEquals(2, result.size)
        assertEquals(1, result[0].originalId)
        assertTrue(result[0].content.contains("今天"))
        assertTrue(result[0].keywords.contains("公园"))
        assertTrue(result[0].sentiment == "neutral")

        assertEquals(2, result[1].originalId)
        assertTrue(result[1].keywords.contains("朋友"))
    }

    @Test
    fun `preprocess handles empty content`() = runTest {
        val fragments = listOf(
            Fragment(id = 1, content = "")
        )

        val result = processor.preprocess(fragments)

        assertEquals(1, result.size)
        assertEquals("", result[0].content)
        assertTrue(result[0].keywords.isEmpty())
    }

    @Test
    fun `generateDiary returns content from API`() = runTest {
        val spyProcessor = spyk(processor, recordPrivateCalls = true)
        every { spyProcessor.applyConfig() } returns LlmConfig(apiKey = "fake_api_key")

        val fragments = listOf(
            ProcessedFragment(1, "早上跑步", listOf("跑步"), "neutral")
        )
        coEvery { deepSeekApi.chatCompletion(any(), any(), any()) } returns ChatCompletionResponse(
            choices = listOf(Choice(Message(role = "assistant", content = "今天早起跑步了。")))
        )

        val result = spyProcessor.generateDiary(fragments)

        assertEquals("今天早起跑步了。", result)
    }

    @Test(expected = IllegalStateException::class)
    fun `generateDiary throws when API key missing`() = runTest {
        val spyProcessor = spyk(processor, recordPrivateCalls = true)
        every { spyProcessor.applyConfig() } returns LlmConfig(apiKey = "")

        val fragments = listOf(
            ProcessedFragment(1, "content", emptyList(), "neutral")
        )

        spyProcessor.generateDiary(fragments)
    }

    @Test(expected = IllegalStateException::class)
    fun `generateDiary throws on 401`() = runTest {
        val spyProcessor = spyk(processor, recordPrivateCalls = true)
        every { spyProcessor.applyConfig() } returns LlmConfig(apiKey = "fake_key")

        val httpException = mockk<HttpException>()
        every { httpException.code() } returns 401
        coEvery { deepSeekApi.chatCompletion(any(), any(), any()) } throws httpException

        spyProcessor.generateDiary(listOf(ProcessedFragment(1, "c", emptyList(), "neutral")))
    }

    @Test
    fun `assessPERMA parses JSON response correctly`() = runTest {
        val spyProcessor = spyk(processor, recordPrivateCalls = true)
        every { spyProcessor.applyConfig() } returns LlmConfig(apiKey = "fake_key")

        val jsonResponse = """
            {
              "positiveEmotion": 8.0,
              "engagement": 7.5,
              "relationships": 6.0,
              "meaning": 9.0,
              "accomplishment": 5.5,
              "aiReview": "总体不错",
              "suggestions": "继续保持"
            }
        """.trimIndent()

        coEvery { deepSeekApi.chatCompletion(any(), any(), any()) } returns ChatCompletionResponse(
            choices = listOf(Choice(Message(role = "assistant", content = jsonResponse)))
        )

        val result = spyProcessor.assessPERMA("日记内容")

        assertEquals(8.0f, result.positiveEmotion, 0.01f)
        assertEquals(7.5f, result.engagement, 0.01f)
        assertEquals(6.0f, result.relationships, 0.01f)
        assertEquals(9.0f, result.meaning, 0.01f)
        assertEquals(5.5f, result.accomplishment, 0.01f)
        assertEquals("总体不错", result.aiReview)
        assertEquals("继续保持", result.suggestions)
    }

    @Test
    fun `assessPERMA parses markdown wrapped JSON`() = runTest {
        val spyProcessor = spyk(processor, recordPrivateCalls = true)
        every { spyProcessor.applyConfig() } returns LlmConfig(apiKey = "fake_key")

        val jsonResponse = """
            ```json
            {
              "positiveEmotion": 7.0,
              "engagement": 7.0,
              "relationships": 7.0,
              "meaning": 7.0,
              "accomplishment": 7.0,
              "aiReview": "一般",
              "suggestions": "加油"
            }
            ```
        """.trimIndent()

        coEvery { deepSeekApi.chatCompletion(any(), any(), any()) } returns ChatCompletionResponse(
            choices = listOf(Choice(Message(role = "assistant", content = jsonResponse)))
        )

        val result = spyProcessor.assessPERMA("日记")

        assertEquals(7.0f, result.positiveEmotion, 0.01f)
        assertEquals("一般", result.aiReview)
    }

    @Test
    fun `assessPERMA falls back to defaults on malformed JSON`() = runTest {
        val spyProcessor = spyk(processor, recordPrivateCalls = true)
        every { spyProcessor.applyConfig() } returns LlmConfig(apiKey = "fake_key")

        coEvery { deepSeekApi.chatCompletion(any(), any(), any()) } returns ChatCompletionResponse(
            choices = listOf(Choice(Message(role = "assistant", content = "这不是 JSON")))
        )

        val result = spyProcessor.assessPERMA("日记")

        assertEquals(5.0f, result.positiveEmotion, 0.01f)
        assertEquals(5.0f, result.engagement, 0.01f)
        assertTrue(result.aiReview.contains("遇到了一点问题"))
    }

    @Test(expected = IllegalStateException::class)
    fun `assessPERMA throws on empty API response`() = runTest {
        val spyProcessor = spyk(processor, recordPrivateCalls = true)
        every { spyProcessor.applyConfig() } returns LlmConfig(apiKey = "fake_key")

        coEvery { deepSeekApi.chatCompletion(any(), any(), any()) } returns ChatCompletionResponse(
            choices = emptyList()
        )

        spyProcessor.assessPERMA("日记")
    }

    @Test
    fun `generateReview splits suggestions into list`() = runTest {
        val perma = PermaScoreResult(
            positiveEmotion = 8f,
            engagement = 7f,
            relationships = 6f,
            meaning = 9f,
            accomplishment = 5f,
            aiReview = "不错",
            suggestions = "早睡\n多喝水\n运动\n阅读"
        )

        val result = processor.generateReview(perma)

        assertEquals("不错", result.review)
        assertEquals(listOf("早睡", "多喝水", "运动"), result.suggestions)
    }
}
