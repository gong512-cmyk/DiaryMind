package com.diarymind.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.diarymind.ui.viewmodel.DiaryViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun StatsScreen(
    navController: NavController,
    viewModel: DiaryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val totalDiaries = uiState.diaries.size
    val totalWords = uiState.diaries.sumOf { it.wordCount }
    val totalFragments = uiState.fragments.size

    val avgPerma = remember(uiState.permaScores) {
        if (uiState.permaScores.isEmpty()) null else {
            val scores = uiState.permaScores.values
            PermaAverages(
                positiveEmotion = scores.map { it.positiveEmotion }.average().toFloat(),
                engagement = scores.map { it.engagement }.average().toFloat(),
                relationships = scores.map { it.relationships }.average().toFloat(),
                meaning = scores.map { it.meaning }.average().toFloat(),
                accomplishment = scores.map { it.accomplishment }.average().toFloat()
            )
        }
    }

    val recent7Days = remember(uiState.diaries, uiState.permaScores) {
        val today = LocalDate.now()
        (0..6).map { offset ->
            val date = today.minusDays(offset.toLong())
            val dateStr = date.format(DateTimeFormatter.ISO_DATE)
            val diary = uiState.diaries.find { it.date == dateStr }
            val perma = diary?.let { uiState.permaScores[it.id] }
            DayPerma(dateStr, perma?.positiveEmotion ?: 0f)
        }.reversed()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "统计",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Summary cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SummaryCard(
                value = totalDiaries.toString(),
                label = "日记篇数",
                modifier = Modifier.weight(1f)
            )
            SummaryCard(
                value = totalWords.toString(),
                label = "总字数",
                modifier = Modifier.weight(1f)
            )
            SummaryCard(
                value = totalFragments.toString(),
                label = "碎片条数",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // PERMA bar chart
        if (avgPerma != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "PERMA 五维平均分",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    PermaBarChart(
                        averages = avgPerma
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 7-day mood curve
        if (recent7Days.any { it.score > 0 }) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "近7天积极情绪",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    MoodLineChart(
                        data = recent7Days
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SummaryCard(value: String, label: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

data class PermaAverages(
    val positiveEmotion: Float,
    val engagement: Float,
    val relationships: Float,
    val meaning: Float,
    val accomplishment: Float
)

data class DayPerma(val date: String, val score: Float)

@Composable
private fun PermaBarChart(averages: PermaAverages) {
    val labels = listOf("积极情绪", "投入", "关系", "意义", "成就")
    val values = listOf(
        averages.positiveEmotion,
        averages.engagement,
        averages.relationships,
        averages.meaning,
        averages.accomplishment
    )
    val maxValue = 10f
    val colors = listOf(
        Color(0xFFE74C3C),
        Color(0xFFE8A87C),
        Color(0xFFF1C40F),
        Color(0xFF2ECC71),
        Color(0xFF3498DB)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        labels.forEachIndexed { index, label ->
            val value = values[index]
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "%.1f".format(value),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .height((value / maxValue * 120).dp)
                        .background(colors[index], RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MoodLineChart(data: List<DayPerma>) {
    val primaryColor = MaterialTheme.colorScheme.primary
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val padding = 32.dp.toPx()
                val chartWidth = size.width - padding * 2
                val chartHeight = size.height - padding
                val maxValue = 10f

                // Draw grid lines
                for (i in 0..5) {
                    val y = padding + chartHeight * (1 - i / 5f)
                    drawLine(
                        color = Color.LightGray.copy(alpha = 0.5f),
                        start = Offset(padding, y),
                        end = Offset(size.width - padding, y),
                        strokeWidth = 1f
                    )
                }

                if (data.size < 2) return@Canvas

                // Draw line
                val points = data.mapIndexed { index, dayPerma ->
                    val x = padding + (index.toFloat() / (data.size - 1)) * chartWidth
                    val y = if (dayPerma.score > 0) {
                        padding + chartHeight * (1 - dayPerma.score / maxValue)
                    } else {
                        padding + chartHeight
                    }
                    Offset(x, y)
                }

                // Draw connecting lines
                for (i in 0 until points.size - 1) {
                    if (data[i].score > 0 || data[i + 1].score > 0) {
                        drawLine(
                            color = primaryColor,
                            start = points[i],
                            end = points[i + 1],
                            strokeWidth = 2f
                        )
                    }
                }

                // Draw points
                points.forEachIndexed { index, point ->
                    if (data[index].score > 0) {
                        drawCircle(
                            color = primaryColor,
                            radius = 4.dp.toPx(),
                            center = point
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 2.dp.toPx(),
                            center = point
                        )
                    }
                }
            }
        }
        // Date labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            data.forEach { dayPerma ->
                Text(
                    text = dayPerma.date.substring(5),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
