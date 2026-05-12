package com.diarymind.domain.usecase

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.diarymind.data.remote.ChatCompletionRequest
import com.diarymind.data.remote.DeepSeekApi
import com.diarymind.data.remote.DynamicBaseUrlInterceptor
import com.diarymind.data.remote.Message
import com.diarymind.domain.model.Fragment
import com.diarymind.domain.model.EditLevel
import com.diarymind.domain.model.ReviewDepth
import com.diarymind.util.LlmConfig
import com.diarymind.util.getCustomPrompt
import com.diarymind.util.getEditLevel
import com.diarymind.util.getReviewDepth
import com.diarymind.util.getLlmConfig
import com.google.gson.Gson
import okhttp3.HttpUrl.Companion.toHttpUrl
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
    private val baseUrlInterceptor: DynamicBaseUrlInterceptor,
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

    internal fun applyConfig(): LlmConfig {
        val config = try { getLlmConfig(context) } catch (_: Exception) { LlmConfig() }
        baseUrlInterceptor.baseUrl = config.baseUrl.toHttpUrl()
        return config
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
            val config = applyConfig()
            val apiKey = config.apiKey.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("API key not configured")

            val depth = try { getReviewDepth(context) } catch (_: Exception) { ReviewDepth.STANDARD }

            val prompt = """
                请对以下日记内容进行 PERMA 积极心理学五维评估，并给出反馈。
                请严格按照以下 JSON 格式返回，不要包含任何其他内容：
                {
                  "positiveEmotion": 0-10,
                  "engagement": 0-10,
                  "relationships": 0-10,
                  "meaning": 0-10,
                  "accomplishment": 0-10,
                  "aiReview": "${depth.reviewPrompt}",
                  "suggestions": "${depth.maxReviewChars}字以内的明日建议"
                }

                日记内容：
                $text
            """.trimIndent()

            val request = ChatCompletionRequest(
                model = config.model,
                messages = listOf(
                    Message(role = "system", content = depth.systemPrompt),
                    Message(role = "user", content = prompt)
                ),
                temperature = config.temperature,
                max_tokens = config.maxTokens
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
                    in 500..599 -> IllegalStateException("AI 服务器暂时不可用，请稍后再试")
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
            val config = applyConfig()
            val apiKey = config.apiKey.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("API key not configured")

            val fragmentsText = fragments.joinToString("\n\n") {
                "[${it.originalId}] ${it.content}"
            }

            val editLevel = try { getEditLevel(context) } catch (_: Exception) { EditLevel.TYPO_ONLY }
            val customPrompt = try { getCustomPrompt(context) } catch (_: Exception) { null }

            val systemPrompt = editLevel.systemPrompt
            val userPrompt = if (!customPrompt.isNullOrBlank()) {
                customPrompt.replace("{fragments}", fragmentsText)
            } else {
                "${editLevel.userPromptPrefix}\n\n碎片记录：\n$fragmentsText"
            }

            val request = ChatCompletionRequest(
                model = config.model,
                messages = listOf(
                    Message(role = "system", content = systemPrompt),
                    Message(role = "user", content = userPrompt)
                ),
                temperature = config.temperature,
                max_tokens = config.maxTokens
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
                    in 500..599 -> IllegalStateException("AI 服务器暂时不可用，请稍后再试")
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

    override suspend fun assessQuality(fragments: List<ProcessedFragment>): Int =
        withContext(Dispatchers.IO) {
            val config = applyConfig()
            val apiKey = config.apiKey.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("API key not configured")

            val fragmentsText = fragments.joinToString("\n") { "- ${it.content}" }

            // Check if there's a previous rating to enforce "only up" rule
            // Note: the caller (PipelineOrchestrator) handles max(old, new)
            val prompt = """
                请对以下当日碎片记录进行质量评级（1-5星），仅返回JSON：
                {
                  "rating": 1-5,
                  "reason": "一句话说明为什么给这个等级"
                }

                评级标准：
                1星 — 全是日常记录，无任何思考或感悟
                2星 — 有有意义的事，但缺乏深入思考
                3星 — 有明显的感悟、反思或想法
                4星 — 出现重要洞见、关键思路、有价值的观察
                5星 — 改变认知的反思 + 有可操作的实践经验总结

                当日碎片：
                $fragmentsText
            """.trimIndent()

            val request = ChatCompletionRequest(
                model = config.model,
                messages = listOf(
                    Message(role = "system", content = "你是一位专业的内容质量评估助手，只返回JSON格式的评级结果。"),
                    Message(role = "user", content = prompt)
                ),
                temperature = 0.3f,
                max_tokens = 256
            )

            val response = try {
                deepSeekApi.chatCompletion(
                    authorization = "Bearer $apiKey",
                    request = request
                )
            } catch (e: HttpException) {
                throw when (e.code()) {
                    401 -> IllegalStateException("API Key 无效或已过期")
                    429 -> IllegalStateException("请求过于频繁，请稍后再试")
                    in 500..599 -> IllegalStateException("AI 服务器暂时不可用")
                    else -> IllegalStateException("API 请求失败: ${e.code()}")
                }
            } catch (e: SocketTimeoutException) {
                throw IllegalStateException("连接超时")
            } catch (e: IOException) {
                throw IllegalStateException("网络连接失败")
            }

            val content = response.choices.firstOrNull()?.message?.content
                ?: throw IllegalStateException("API 返回为空")

            parseQualityResponse(content)
        }

    private fun parseQualityResponse(content: String): Int {
        val json = content.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        return try {
            val obj = gson.fromJson(json, Map::class.java)
            val rating = obj["rating"]?.toString()?.toFloatOrNull()?.toInt() ?: 0
            rating.coerceIn(1, 5)
        } catch (_: Exception) {
            0
        }
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
