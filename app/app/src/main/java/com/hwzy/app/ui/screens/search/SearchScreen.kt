package com.hwzy.app.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hwzy.app.data.AppData
import com.hwzy.app.data.ItemType
import com.hwzy.app.data.SearchableItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onNavigateBack: () -> Unit,
    onItemClick: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<SearchableItem>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()

    // 监听搜索文本变化
    LaunchedEffect(searchQuery) {
        coroutineScope.launch {
            // 添加一个小延迟，避免频繁搜索
            delay(300)
            searchResults = AppData.searchItems(searchQuery)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("搜索") },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "清除")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (searchQuery.isEmpty()) {
            // 显示搜索历史或推荐
            SearchSuggestions(
                modifier = Modifier.padding(paddingValues),
                onItemClick = { searchQuery = it }
            )
        } else {
            // 显示搜索结果
            SearchResults(
                modifier = Modifier.padding(paddingValues),
                results = searchResults,
                onItemClick = onItemClick
            )
        }
    }
}

@Composable
private fun SearchSuggestions(
    modifier: Modifier = Modifier,
    onItemClick: (String) -> Unit
) {
    val suggestions = listOf("测试")
    
    LazyColumn(modifier = modifier) {
        item {
            Text(
                text = "热门搜索",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )
        }
        items(suggestions) { suggestion ->
            ListItem(
                headlineContent = { Text(suggestion) },
                leadingContent = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier.clickable { onItemClick(suggestion) }
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun SearchResults(
    modifier: Modifier = Modifier,
    results: List<SearchableItem>,
    onItemClick: (String) -> Unit
) {
    if (results.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("未找到相关结果")
        }
        return
    }

    val groupedResults = results.groupBy { it.category }
    
    LazyColumn(modifier = modifier) {
        groupedResults.forEach { (category, items) ->
            item {
                Text(
                    text = category,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(16.dp)
                )
            }
            items(items) { item ->
                SearchResultItem(
                    item = item,
                    onItemClick = { onItemClick(item.id) }
                )
            }
        }
    }
}

@Composable
private fun SearchResultItem(
    item: SearchableItem,
    onItemClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(item.title) },
        supportingContent = {
            when (item.type) {
                ItemType.ARTICLE -> {
                    val article = item as? com.hwzy.app.data.ArticleItem
                    article?.let {
                        Text("${it.author} · ${it.publishDate}")
                    }
                }
                else -> {
                    Text(item.type.name)
                }
            }
        },
        leadingContent = {
            Icon(
                item.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        modifier = Modifier.clickable(onClick = onItemClick)
    )
    HorizontalDivider()
}
