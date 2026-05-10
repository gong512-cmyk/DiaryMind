package com.diarymind.data.remote

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface DeepSeekApi {
    @POST("v1/chat/completions")
    suspend fun chatCompletion(
        @Header("Authorization") authorization: String,
        @Header("Content-Type") contentType: String = "application/json",
        @Body request: ChatCompletionRequest
    ): ChatCompletionResponse
}

data class ChatCompletionRequest(
    val model: String,
    val messages: List<Message>,
    val temperature: Float,
    val max_tokens: Int
)

data class Message(
    val role: String,
    val content: String
)

data class ChatCompletionResponse(
    val choices: List<Choice>,
    val usage: Usage? = null
)

data class Choice(
    val message: Message,
    val finish_reason: String? = null
)

data class Usage(
    val prompt_tokens: Int,
    val completion_tokens: Int,
    val total_tokens: Int
)
