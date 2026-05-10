package com.diarymind.di

import android.content.Context
import com.diarymind.data.local.DiaryDatabase
import com.diarymind.data.remote.DeepSeekApi
import com.diarymind.data.remote.DeepSeekRetrofitClient
import com.diarymind.domain.usecase.DiaryAIProcessor
import com.diarymind.domain.usecase.ExternalAPIProcessor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): DiaryDatabase {
        return DiaryDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideFragmentDao(database: DiaryDatabase) = database.fragmentDao()

    @Provides
    @Singleton
    fun provideDiaryDao(database: DiaryDatabase) = database.diaryDao()

    @Provides
    @Singleton
    fun providePermaDao(database: DiaryDatabase) = database.permaDao()

    @Provides
    @Singleton
    fun provideCrossRefDao(database: DiaryDatabase) = database.crossRefDao()

    @Provides
    @Singleton
    fun provideDynamicBaseUrlInterceptor(): com.diarymind.data.remote.DynamicBaseUrlInterceptor {
        return com.diarymind.data.remote.DynamicBaseUrlInterceptor()
    }

    @Provides
    @Singleton
    fun provideDeepSeekApi(interceptor: com.diarymind.data.remote.DynamicBaseUrlInterceptor): DeepSeekApi {
        return com.diarymind.data.remote.DeepSeekRetrofitClient(interceptor).api
    }

    @Provides
    @Singleton
    fun provideAIProcessor(processor: ExternalAPIProcessor): DiaryAIProcessor = processor
}
