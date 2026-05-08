package com.diarymind

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.diarymind.data.repository.DiaryRepository
import com.diarymind.domain.model.DiaryEntry
import com.diarymind.domain.model.PermaScore
import com.diarymind.ui.screens.CaptureScreen
import com.diarymind.ui.screens.DiaryDetailScreen
import com.diarymind.ui.screens.DiaryListScreen
import com.diarymind.ui.screens.HomeScreen
import com.diarymind.ui.screens.SettingsScreen
import com.diarymind.ui.theme.DiaryMindTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var repository: DiaryRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            if (repository.getDiaryById(2) == null) {
                repository.addDiary(
                    DiaryEntry(
                        id = 2,
                        date = "2026-05-07",
                        title = "2026-05-07 测试日记",
                        content = """# 今天过得不错

早上参加了产品评审会，讨论了新功能的交互设计。下午去健身房跑步30分钟，感觉状态很好。晚上读了一会儿书，关于积极心理学的。""",
                        wordCount = 58,
                        localPath = "/data/data/com.diarymind/files/diaries/2026-05-07-测试日记.md"
                    )
                )
                repository.addPermaScore(
                    PermaScore(
                        id = 2,
                        diaryId = 2,
                        positiveEmotion = 8f,
                        engagement = 7f,
                        relationships = 6f,
                        meaning = 9f,
                        accomplishment = 5f,
                        aiReview = "你今天在多个维度都展现了积极的状态。工作投入度高，运动习惯值得肯定。建议在人际关系方面多投入一些时间。",
                        suggestions = """1. 明天尝试和同事共进午餐
2. 继续保持运动习惯
3. 睡前做10分钟冥想"""
                    )
                )
            }
        }

        setContent {
            DiaryMindTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "home") {
                        composable("home") { HomeScreen(navController) }
                        composable("capture") { CaptureScreen(navController) }
                        composable("diaryList") { DiaryListScreen(navController) }
                        composable(
                            "diaryDetail/{diaryId}",
                            deepLinks = listOf(androidx.navigation.navDeepLink { uriPattern = "diarymind://diaryDetail/{diaryId}" })
                        ) { backStackEntry ->
                            val diaryId = backStackEntry.arguments?.getString("diaryId")?.toLongOrNull() ?: 0L
                            DiaryDetailScreen(navController, diaryId)
                        }
                        composable("settings") { SettingsScreen(navController) }
                    }
                }
            }
        }
    }
}
