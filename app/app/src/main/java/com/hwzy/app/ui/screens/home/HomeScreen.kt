package com.hwzy.app.ui.screens.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hwzy.app.navigation.TopTabItem
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    @Suppress("unused") onNavigateToDetail: (String) -> Unit
) {
    // 当前选中的标签
    var selectedTab by rememberSaveable { mutableStateOf<TopTabItem>(TopTabItem.HuiWen) }
    
    // 创建 PagerState
    val pagerState = rememberPagerState(
        initialPage = TopTabItem.items.indexOf(selectedTab)
    ) { TopTabItem.items.size }

    // 添加调试日志
    LaunchedEffect(Unit) {
        Timber.d("HomeScreen 被创建")
    }

    // 监听页面切换
    LaunchedEffect(pagerState.currentPage) {
        selectedTab = TopTabItem.items[pagerState.currentPage]
    }

    // 监听标签切换
    LaunchedEffect(selectedTab) {
        val targetPage = TopTabItem.items.indexOf(selectedTab)
        if (targetPage != pagerState.currentPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 顶部标签栏
        TabRow(
            selectedTabIndex = TopTabItem.items.indexOf(selectedTab),
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
                            text = stringResource(id = tab.title),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                )
            }
        }

        // 内容区域
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) { page ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                when (TopTabItem.items[page]) {
                    TopTabItem.News -> NewsContent()
                    TopTabItem.HuiWen -> HomeContent()
                    TopTabItem.AI -> AIContent()
                }
            }
        }
    }
}
