package com.hwzy.app.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hwzy.app.components.AppTopBar
import com.hwzy.app.navigation.Screen
import com.hwzy.app.navigation.TopTabItem
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    @Suppress("unused") onNavigateToDetail: (String) -> Unit
) {
    // 当前选中的标签
    var selectedTab by remember { mutableStateOf<TopTabItem>(TopTabItem.HuiWen) }

    // 添加调试日志
    LaunchedEffect(Unit) {
        Timber.d("HomeScreen 被创建")
        Timber.d("Selected Index: ${TopTabItem.items.indexOf(selectedTab)}")
    }

    Scaffold(
        topBar = {
            AppTopBar(
                currentRoute = Screen.Home.route,
                onSearchClick = { Timber.d("搜索按钮被点击") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 顶部标签栏
            TabRow(
                selectedTabIndex =  TopTabItem.items.indexOf(selectedTab),
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                TopTabItem.items.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = {
                            selectedTab = tab
                            Timber.d("选中标签: ${tab.title}")
                        },
                        text = {
                            Text(
                                text = tab.title,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    )
                }
            }

            // 内容区域
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                when (selectedTab) {
                    TopTabItem.News -> NewsContent()
                    TopTabItem.HuiWen -> HomeContent()
                    TopTabItem.AI -> AIContent()
                }
            }
        }
    }
}

@Composable
private fun NewsContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("资讯内容")
    }
}

@Composable
private fun HomeContent() {

    Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "主页内容",
            style = MaterialTheme.typography.headlineMedium ,
            color = Color.Black
        )
    }
}

@Composable
private fun AIContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("AI 内容")
    }
}
