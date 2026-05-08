package com.diarymind.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.diarymind.R
import com.diarymind.data.repository.DiaryRepository
import com.diarymind.domain.usecase.PipelineOrchestrator
import com.diarymind.util.getApiKey
import com.diarymind.util.hasPrivacyConsent
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@HiltWorker
class DiaryGenerationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: DiaryRepository,
    private val pipelineOrchestrator: PipelineOrchestrator
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "daily_diary_generation"
        private const val CHANNEL_ID = "diary_mind_channel"
        private const val NOTIFICATION_ID = 1
    }

    override suspend fun doWork(): Result {
        val context = applicationContext

        if (!hasPrivacyConsent(context) || getApiKey(context) == null) {
            return Result.failure()
        }

        val today = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
        val fragments = repository.getFragmentsByDate(today)
            .filter { it.pipelineStep != com.diarymind.domain.model.PipelineStep.COMPLETED }

        if (fragments.isEmpty()) {
            return Result.success()
        }

        return try {
            val state = pipelineOrchestrator.executePipeline(fragments).first { it !is PipelineOrchestrator.PipelineState.Running }

            when (state) {
                is PipelineOrchestrator.PipelineState.Success -> {
                    showNotification("日记已生成", "今日日记已整理完成，点击查看")
                    Result.success()
                }
                is PipelineOrchestrator.PipelineState.Error -> {
                    showNotification("日记生成失败", state.message)
                    Result.failure()
                }
                else -> Result.failure()
            }
        } catch (e: Exception) {
            showNotification("日记生成失败", e.message ?: "未知错误")
            Result.failure()
        }
    }

    private fun showNotification(title: String, message: String) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "日记生成",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "日记生成完成或失败时的通知"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
