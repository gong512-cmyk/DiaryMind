package com.diarymind.data.remote

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Test

class DynamicBaseUrlInterceptorTest {

    @Test
    fun `resolveUrl preserves endpoint for root base url`() {
        val interceptor = DynamicBaseUrlInterceptor().apply {
            baseUrl = "https://api.deepseek.com/".toHttpUrl()
        }
        val original = "http://localhost/v1/chat/completions".toHttpUrl()

        val resolved = interceptor.resolveUrl(original)

        assertEquals("https://api.deepseek.com/v1/chat/completions", resolved.toString())
    }

    @Test
    fun `resolveUrl avoids duplicate version segment`() {
        val interceptor = DynamicBaseUrlInterceptor().apply {
            baseUrl = "https://api.openai.com/v1/".toHttpUrl()
        }
        val original = "http://localhost/v1/chat/completions".toHttpUrl()

        val resolved = interceptor.resolveUrl(original)

        assertEquals("https://api.openai.com/v1/chat/completions", resolved.toString())
    }

    @Test
    fun `resolveUrl keeps provider base path`() {
        val interceptor = DynamicBaseUrlInterceptor().apply {
            baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1/".toHttpUrl()
        }
        val original = "http://localhost/v1/chat/completions".toHttpUrl()

        val resolved = interceptor.resolveUrl(original)

        assertEquals(
            "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions",
            resolved.toString()
        )
    }
}
