package com.hwzy.app.ui.screens.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.hwzy.app.components.AppTopBar
import com.hwzy.app.navigation.Screen

data class DiscoverItem(
    val title: String,
    val icon: ImageVector,
    val route: String,
    val category: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    onNavigateToDetail: (String) -> Unit
) {
    val discoverItems = listOf(
        // 生活服务类
        DiscoverItem("测试", Icons.Filled.WbSunny, "test", "生活服务"),
        DiscoverItem("天气", Icons.Filled.WbSunny, "weather", "生活服务"),
        DiscoverItem("日历", Icons.Filled.CalendarMonth, "calendar", "生活服务"),
        DiscoverItem("备忘录", Icons.Filled.NoteAlt, "notes", "生活服务"),
        
        // 娱乐类
        DiscoverItem("音乐", Icons.Filled.MusicNote, "music", "娱乐"),
        DiscoverItem("视频", Icons.Filled.VideoLibrary, "video", "娱乐"),
        DiscoverItem("游戏", Icons.Filled.SportsEsports, "games", "娱乐"),
        
        // 工具类
        DiscoverItem("计算器", Icons.Filled.Calculate, "calculator", "工具"),
        DiscoverItem("翻译", Icons.Filled.Translate, "translate", "工具"),
        DiscoverItem("二维码", Icons.Filled.QrCode, "qrcode", "工具")
    )

    val groupedItems = discoverItems.groupBy { it.category }

    Scaffold(
        topBar = {
            AppTopBar(
                currentRoute = Screen.Discover.route,
                onSearchClick = { /* TODO: 实现搜索功能 */ }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            groupedItems.forEach { (category, items) ->
                item {
                    CategoryHeader(category = category)
                }
                items(items) { item ->
                    DiscoverItemCard(
                        item = item,
                        onItemClick = { onNavigateToDetail(item.route) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryHeader(category: String) {
    Text(
        text = category,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp)
    )
}

@Composable
private fun DiscoverItemCard(
    item: DiscoverItem,
    onItemClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onItemClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "更多",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    HorizontalDivider()
}
