package com.diarymind

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.diarymind.data.repository.DiaryRepository
import com.diarymind.domain.model.DiaryEntry
import com.diarymind.domain.model.PermaScore
import com.diarymind.ui.screens.CaptureScreen
import com.diarymind.ui.screens.CalendarScreen
import com.diarymind.ui.screens.DiaryDetailScreen
import com.diarymind.ui.screens.DiaryListScreen
import com.diarymind.ui.screens.HomeScreen
import com.diarymind.ui.screens.SettingsScreen
import com.diarymind.ui.screens.StatsScreen
import com.diarymind.ui.theme.DiaryMindTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Home : Screen("home", "首页", Icons.Default.Home)
    data object Calendar : Screen("calendar", "日历", Icons.Default.CalendarMonth)
    data object Stats : Screen("stats", "统计", Icons.Default.PieChart)
    data object Settings : Screen("settings", "设置", Icons.Default.Settings)
}

val bottomNavItems = listOf(Screen.Home, Screen.Calendar, Screen.Stats, Screen.Settings)

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
                    MainScreen()
                }
            }
        }
    }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = bottomNavItems.any { it.route == currentDestination?.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Home.route) { HomeScreen(navController) }
            composable(Screen.Calendar.route) { CalendarScreen(navController) }
            composable(Screen.Stats.route) { StatsScreen(navController) }
            composable(Screen.Settings.route) { SettingsScreen(navController) }
            composable("capture") { CaptureScreen(navController) }
            composable(
                "capture/{fragmentId}",
                arguments = listOf(
                    navArgument("fragmentId") {
                        type = androidx.navigation.NavType.LongType
                    }
                )
            ) { backStackEntry ->
                val fragmentId = backStackEntry.arguments?.getLong("fragmentId")
                CaptureScreen(navController, fragmentId)
            }
            composable("diaryList") { DiaryListScreen(navController) }
            composable(
                "diaryDetail/{diaryId}",
                deepLinks = listOf(navDeepLink { uriPattern = "diarymind://diaryDetail/{diaryId}" })
            ) { backStackEntry ->
                val diaryId = backStackEntry.arguments?.getString("diaryId")?.toLongOrNull() ?: 0L
                DiaryDetailScreen(navController, diaryId)
            }
        }
    }
}
