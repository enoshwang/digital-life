package com.hwzy.app.screens.discover

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hwzy.app.components.AppTopBar
import com.hwzy.app.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    onNavigateToDetail: (String) -> Unit
) {
    Scaffold(
        topBar = {
            AppTopBar(
                currentRoute = Screen.Discover.route,
                onSearchClick = { /* TODO: 实现搜索功能 */ }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "测试页面",
                modifier = Modifier
                    .padding(16.dp)
                    .clickable {
                        onNavigateToDetail("test")
                    }
            )
        }
    }
}
