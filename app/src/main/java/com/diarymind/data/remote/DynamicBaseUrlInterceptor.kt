package com.diarymind.data.remote

import okhttp3.Interceptor
import okhttp3.HttpUrl
import okhttp3.Response
import okhttp3.HttpUrl.Companion.toHttpUrl

class DynamicBaseUrlInterceptor : Interceptor {

    @Volatile
    var baseUrl: okhttp3.HttpUrl = "https://api.deepseek.com/".toHttpUrl()

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val newUrl = resolveUrl(originalRequest.url)
        val newRequest = originalRequest.newBuilder()
            .url(newUrl)
            .build()
        return chain.proceed(newRequest)
    }

    internal fun resolveUrl(originalUrl: HttpUrl): HttpUrl {
        val baseSegments = baseUrl.encodedPathSegments.filter { it.isNotEmpty() }
        val originalSegments = originalUrl.encodedPathSegments.filter { it.isNotEmpty() }
        val endpointSegments = if (
            baseSegments.isNotEmpty() &&
            originalSegments.isNotEmpty() &&
            baseSegments.last() == originalSegments.first()
        ) {
            originalSegments.drop(1)
        } else {
            originalSegments
        }

        return originalUrl.newBuilder()
            .scheme(baseUrl.scheme)
            .host(baseUrl.host)
            .port(baseUrl.port)
            .encodedPath("/")
            .apply {
                (baseSegments + endpointSegments).forEach { addEncodedPathSegment(it) }
            }
            .build()
    }
}
