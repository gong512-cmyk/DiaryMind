package com.diarymind.domain.usecase

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.diarymind.data.remote.ChatCompletionRequest
import com.diarymind.data.remote.DeepSeekApi
import com.diarymind.data.remote.Message
import com.diarymind.domain.model.Fragment
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExternalAPIProcessor @Inject constructor(
    private val deepSeekApi: DeepSeekApi,
    @ApplicationContext private val context: Context
) : DiaryAIProcessor {

    private val gson = Gson()

    private val masterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val encryptedPrefs by lazy {
        EncryptedSharedPreferences.create(
            context,
            "diarymind_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun getApiKey(): String? {
        return encryptedPrefs.getString("deepseek_api_key", null)
    }

    override suspend fun preprocess(fragments: List<Fragment>): List<ProcessedFragment> =
        withContext(Dispatchers.IO) {
            fragments.map { fragment ->
                val cleaned = fragment.content
                    .replace(Regex("[嗯啊哦哈哼嘛呢吧了呗]|(\\.{2,})|(！{2,})|(？{2,})"), "")
                    .trim()
                ProcessedFragment(
                    originalId = fragment.id,
                    content = cleaned,
                    keywords = extractKeywords(cleaned),
                    sentiment = "neutral"
                )
            }
        }

    private fun extractKeywords(text: String): List<String> {
        val stopWords = setOf("的", "了", "是", "我", "在", "和", "就", "都", "要", "会", "能", "很", "有", "到", "说", "去", "可以", "这", "那", "个", "上", "下", "他", "她", "它", "们", "与", "为", "而", "但", "也", "或", "之", "以", "及", "被", "让", "给", "把", "从", "向", "比", "对", "因", "于", "已")
        return text.split(Regex("[^\\u4e00-\\u9fa5a-zA-Z0-9]+"))
            .filter { it.length >= 2 }
            .filterNot { stopWords.contains(it) }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(5)
            .map { it.key }
    }

    override suspend fun assessPERMA(text: String): PermaScoreResult =
        withContext(Dispatchers.IO) {
            val apiKey = getApiKey() ?: throw IllegalStateException("API key not configured")

            val prompt = """
                请对以下日记内容进行 PERMA 积极心理学五维评估，并给出建设性反馈。
                请严格按照以下 JSON 格式返回，不要包含任何其他内容：
                {
                  "positiveEmotion": 0-10,
                  "engagement": 0-10,
                  "relationships": 0-10,
                  "meaning": 0-10,
                  "accomplishment": 0-10,
                  "aiReview": "200字以内的综合评语，用温暖鼓励的语气",
                  "suggestions": "200字以内的明日建议"
                }

                日记内容：
                $text
            """.trimIndent()

            val request = ChatCompletionRequest(
                messages = listOf(
                    Message(role = "system", content = "你是一位专业的积极心理学教练，擅长从日常记录中发现亮点并提供温暖而诚实的反馈。"),
                    Message(role = "user", content = prompt)
                )
            )

            val response = try {
                deepSeekApi.chatCompletion(
                    authorization = "Bearer $apiKey",
                    request = request
                )
            } catch (e: HttpException) {
                throw when (e.code()) {
                    401 -> IllegalStateException("API Key 无效或已过期，请检查设置")
                    429 -> IllegalStateException("请求过于频繁，请稍后再试")
                    in 500..599 -> IllegalStateException("DeepSeek 服务器暂时不可用，请稍后再试")
                    else -> IllegalStateException("API 请求失败: ${e.code()}")
                }
            } catch (e: SocketTimeoutException) {
                throw IllegalStateException("连接超时，请检查网络后重试")
            } catch (e: IOException) {
                throw IllegalStateException("网络连接失败，请检查网络后重试")
            }

            val content = response.choices.firstOrNull()?.message?.content
                ?: throw IllegalStateException("API 返回为空")

            parsePermaResponse(content)
        }

    override suspend fun generateDiary(fragments: List<ProcessedFragment>): String =
        withContext(Dispatchers.IO) {
            val apiKey = getApiKey() ?: throw IllegalStateException("API key not configured")

            val fragmentsText = fragments.joinToString("\n\n") {
                "[${it.originalId}] ${it.content}"
            }

            val prompt = """
                请基于以下碎片记录，生成一篇连贯、温暖、真诚的日记。
                要求：
                1. 保留原始记录的情感色彩和关键信息
                2. 去除重复和冗余，但不过度改写
                3. 使用第一人称，语气自然亲切
                4. 适当添加过渡语句使文章流畅
                5. 总字数控制在 300-2000 字之间
                6. 不要编造碎片中没有的信息

                碎片记录：
                $fragmentsText
            """.trimIndent()

            val request = ChatCompletionRequest(
                messages = listOf(
                    Message(role = "system", content = "你是一位善于倾听和表达的朋友，擅长将零散的想法整理成温暖真诚的文字。"),
                    Message(role = "user", content = prompt)
                )
            )

            val response = try {
                deepSeekApi.chatCompletion(
                    authorization = "Bearer $apiKey",
                    request = request
                )
            } catch (e: HttpException) {
                throw when (e.code()) {
                    401 -> IllegalStateException("API Key 无效或已过期，请检查设置")
                    429 -> IllegalStateException("请求过于频繁，请稍后再试")
                    in 500..599 -> IllegalStateException("DeepSeek 服务器暂时不可用，请稍后再试")
                    else -> IllegalStateException("API 请求失败: ${e.code()}")
                }
            } catch (e: SocketTimeoutException) {
                throw IllegalStateException("连接超时，请检查网络后重试")
            } catch (e: IOException) {
                throw IllegalStateException("网络连接失败，请检查网络后重试")
            }

            response.choices.firstOrNull()?.message?.content
                ?: throw IllegalStateException("API 返回为空")
        }

    override suspend fun generateReview(permaScore: PermaScoreResult): ReviewResult {
        return ReviewResult(
            review = permaScore.aiReview,
            suggestions = permaScore.suggestions.split("\n").filter { it.isNotBlank() }.take(3)
        )
    }

    private fun parsePermaResponse(content: String): PermaScoreResult {
        val json = content.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        return try {
            gson.fromJson(json, PermaScoreResult::class.java)
        } catch (e: Exception) {
            PermaScoreResult(
                positiveEmotion = 5f,
                engagement = 5f,
                relationships = 5f,
                meaning = 5f,
                accomplishment = 5f,
                aiReview = "评估过程中遇到了一点问题，但这不影响你今天的记录很珍贵。",
                suggestions = "明天继续记录，每一小步都值得被看见。"
            )
        }
    }
}
