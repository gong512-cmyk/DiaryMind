package com.diarymind.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.diarymind.domain.model.DiaryEntry
import com.diarymind.domain.model.displayTitle
import com.diarymind.ui.viewmodel.DiaryViewModel
import com.diarymind.util.getLlmConfig
import com.diarymind.util.hasPrivacyConsent
import com.diarymind.util.setPrivacyConsent
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: DiaryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showPrivacyDialog by remember { mutableStateOf(false) }
    val today = LocalDate.now()
    val todayStr = today.format(DateTimeFormatter.ISO_DATE)
    val dateFormatter = DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.CHINESE)

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("隐私确认") },
            text = {
                Text(
                    "生成日记需要将您的记录内容发送到 AI 服务器进行处理。" +
                    "内容会被加密传输，但会离开您的设备。是否继续？"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        setPrivacyConsent(context, true)
                        showPrivacyDialog = false
                        viewModel.generateDiary()
                    }
                ) {
                    Text("同意并继续")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (uiState.needsOverwriteConfirmation) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelOverwrite() },
            title = { Text("日记已存在") },
            text = {
                Text("${uiState.overwriteDiaryDate} 已有日记，重新生成将覆盖旧内容，是否继续？")
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmOverwrite() }) {
                    Text("覆盖")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelOverwrite() }) {
                    Text("取消")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("DiaryMind") },
                actions = {
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("capture") }) {
                Icon(Icons.Default.Add, contentDescription = "记录碎片")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            item {
                Text(
                    text = today.format(dateFormatter),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 16.dp, bottom = 16.dp)
                )
            }

            val todayDiary = uiState.diaries.find { it.date == todayStr }

            if (todayDiary != null) {
                item {
                    TodayDiaryCard(
                        diary = todayDiary,
                        onClick = { navController.navigate("diaryDetail/${todayDiary.id}") }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            item {
                TodayFragmentsSection(
                    uiState = uiState,
                    onGenerateClick = {
                        when {
                            !hasPrivacyConsent(context) -> showPrivacyDialog = true
                            getLlmConfig(context).apiKey.isBlank() -> viewModel.showError("请先在设置中配置 API Key")
                            !isNetworkAvailable(context) -> viewModel.showError("当前无网络连接，请检查网络后重试")
                            else -> viewModel.generateDiary()
                        }
                    },
                    onFragmentClick = { fragmentId ->
                        navController.navigate("capture/$fragmentId")
                    }
                )
            }

            val historyDiaries = uiState.diaries.filter { it.date != todayStr }
            if (historyDiaries.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 32.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "最近动态",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "时间轴 →",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                itemsIndexed(historyDiaries) { index, diary ->
                    TimelineDiaryItem(
                        diary = diary,
                        isFirst = index == 0,
                        isLast = index == historyDiaries.lastIndex,
                        onClick = { navController.navigate("diaryDetail/${diary.id}") }
                    )
                }
            }

            if (uiState.error != null) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    ErrorCard(
                        message = uiState.error!!,
                        onDismiss = { viewModel.clearError() }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun TodayDiaryCard(diary: DiaryEntry, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = diary.displayTitle(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = firstLinePreview(diary.content),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${diary.wordCount} 字",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun TodayFragmentsSection(
    uiState: DiaryViewModel.DiaryUiState,
    onGenerateClick: () -> Unit,
    onFragmentClick: (Long) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "今日碎片",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${uiState.fragments.size} 条记录",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = uiState.pipelineStep,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Button(
                    onClick = onGenerateClick,
                    enabled = uiState.fragments.isNotEmpty()
                ) {
                    Text("生成今日日记")
                }
            }

            if (uiState.fragments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                uiState.fragments.take(3).forEach { fragment ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onFragmentClick(fragment.id) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = fragment.content,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (uiState.fragments.size > 3) {
                    Text(
                        text = "还有 ${uiState.fragments.size - 3} 条...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineDiaryItem(
    diary: DiaryEntry,
    isFirst: Boolean,
    isLast: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clickable(onClick = onClick)
    ) {
        // Timeline axis — seamless single line drawn via Canvas
        val lineColor = MaterialTheme.colorScheme.outline
        val dotColor = MaterialTheme.colorScheme.primary
        Box(
            modifier = Modifier
                .width(32.dp)
                .fillMaxHeight()
                .drawBehind {
                    val centerX = size.width / 2
                    val centerY = size.height / 2
                    val dotRadius = 5.dp.toPx()
                    val strokeWidth = 2.dp.toPx()

                    if (!isFirst) {
                        drawLine(
                            color = lineColor,
                            start = androidx.compose.ui.geometry.Offset(centerX, 0f),
                            end = androidx.compose.ui.geometry.Offset(centerX, centerY - dotRadius),
                            strokeWidth = strokeWidth
                        )
                    }
                    if (!isLast) {
                        drawLine(
                            color = lineColor,
                            start = androidx.compose.ui.geometry.Offset(centerX, centerY + dotRadius),
                            end = androidx.compose.ui.geometry.Offset(centerX, size.height),
                            strokeWidth = strokeWidth
                        )
                    }
                    drawCircle(
                        color = dotColor,
                        radius = dotRadius,
                        center = androidx.compose.ui.geometry.Offset(centerX, centerY)
                    )
                }
        )

        // Content
        Column(
            modifier = Modifier
                .padding(start = 8.dp, bottom = if (isLast) 0.dp else 16.dp)
                .weight(1f)
        ) {
            Text(
                text = diary.date,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = diary.displayTitle(),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = firstLinePreview(diary.content),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ErrorCard(message: String, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭",
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

private fun firstLinePreview(content: String): String {
    return content.lineSequence().firstOrNull()?.trim()?.replace(Regex("^#{1,6}\\s*"), "") ?: ""
}

private fun isNetworkAvailable(context: android.content.Context): Boolean {
    val connectivityManager = context.getSystemService(android.net.ConnectivityManager::class.java)
    val network = connectivityManager?.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
}
