package com.diarymind.data.remote

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeepSeekRetrofitClient @Inject constructor(
    private val baseUrlInterceptor: DynamicBaseUrlInterceptor
) {

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(baseUrlInterceptor)
        .addInterceptor(loggingInterceptor)
        .build()

    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl("http://localhost/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: DeepSeekApi = retrofit.create(DeepSeekApi::class.java)
}
