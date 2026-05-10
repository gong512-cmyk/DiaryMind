package com.diarymind.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.diarymind.ui.viewmodel.DiaryViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun CalendarScreen(
    navController: NavController,
    viewModel: DiaryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }

    val diaryDates = remember(uiState.diaries) {
        uiState.diaries.map { it.date }.toSet()
    }

    val permaByDate = remember(uiState.diaries, uiState.permaScores) {
        uiState.diaries.associate { diary ->
            diary.date to uiState.permaScores[diary.id]
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Month header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "上个月")
            }
            Text(
                text = currentMonth.format(DateTimeFormatter.ofPattern("yyyy年M月", Locale.CHINESE)),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                Icon(Icons.Default.ChevronRight, contentDescription = "下个月")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Weekday headers
        Row(modifier = Modifier.fillMaxWidth()) {
            val weekdays = listOf("日", "一", "二", "三", "四", "五", "六")
            weekdays.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Calendar grid
        val firstDayOfMonth = currentMonth.atDay(1)
        val daysInMonth = currentMonth.lengthOfMonth()
        val startOffset = firstDayOfMonth.dayOfWeek.value % 7
        val totalCells = ((startOffset + daysInMonth + 6) / 7) * 7

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(totalCells) { index ->
                val dayNumber = index - startOffset + 1
                if (dayNumber in 1..daysInMonth) {
                    val date = currentMonth.atDay(dayNumber)
                    val dateStr = date.format(DateTimeFormatter.ISO_DATE)
                    val hasDiary = diaryDates.contains(dateStr)
                    val perma = permaByDate[dateStr]
                    val isToday = date == LocalDate.now()

                    CalendarDayCell(
                        day = dayNumber,
                        isToday = isToday,
                        hasDiary = hasDiary,
                        permaColor = perma?.let { getPermaEmotionColor(it.positiveEmotion) },
                        onClick = {
                            if (hasDiary) {
                                val diary = uiState.diaries.find { it.date == dateStr }
                                diary?.let {
                                    navController.navigate("diaryDetail/${it.id}")
                                }
                            }
                        }
                    )
                } else {
                    Box(modifier = Modifier.aspectRatio(1f))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Legend
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "情绪图例",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    LegendItem(color = Color(0xFFE74C3C), label = "低落")
                    LegendItem(color = Color(0xFFE8A87C), label = "一般")
                    LegendItem(color = Color(0xFF2ECC71), label = "积极")
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    day: Int,
    isToday: Boolean,
    hasDiary: Boolean,
    permaColor: Color?,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(4.dp)
            .clip(CircleShape)
            .background(
                when {
                    isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    else -> Color.Transparent
                }
            )
            .clickable(enabled = hasDiary, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = day.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = when {
                    isToday -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
            if (hasDiary) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(permaColor ?: MaterialTheme.colorScheme.primary)
                )
            } else {
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun getPermaEmotionColor(score: Float): Color {
    return when {
        score <= 4f -> Color(0xFFE74C3C)
        score <= 7f -> Color(0xFFE8A87C)
        else -> Color(0xFF2ECC71)
    }
}
