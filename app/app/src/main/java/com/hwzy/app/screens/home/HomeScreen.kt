package com.hwzy.app.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hwzy.app.components.AppTopBar
import com.hwzy.app.navigation.Screen
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToDetail: (String) -> Unit,
    viewModel: HomeViewModel = viewModel(
        viewModelStoreOwner = LocalContext.current as androidx.lifecycle.ViewModelStoreOwner
    )
) {
    // 使用 remember 保存状态（在配置更改时会重置）
    var rememberCounter by remember { mutableStateOf(0) }
    
    // 使用 rememberSaveable 保存状态（在配置更改时会保持）
    var rememberSaveableCounter by rememberSaveable { mutableStateOf(0) }
    
    // 使用 ViewModel 保存状态（在配置更改时会保持，在进程被杀死时会重置）
    val viewModelCounter by viewModel.counter.collectAsState()

    // 使用 LaunchedEffect 来跟踪屏幕的重新组合
    LaunchedEffect(Unit) {
        Timber.d("HomeScreen 被重新组合，当前计数器值: $viewModelCounter")
    }

    // 使用 DisposableEffect 来跟踪屏幕的创建和销毁
    DisposableEffect(Unit) {
        Timber.d("HomeScreen 被创建，ViewModel 实例: $viewModel")
        onDispose {
            Timber.d("HomeScreen 被销毁")
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                currentRoute = Screen.Home.route,
                onSearchClick = { /* TODO: 实现搜索功能 */ }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "状态保存测试",
                style = MaterialTheme.typography.headlineMedium
            )
            
            // Remember 计数器
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Remember 计数器",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "值: $rememberCounter",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Button(
                        onClick = { 
                            rememberCounter++
                            Timber.d("HomeScreen Remember 计数器增加，当前值: $rememberCounter")
                        }
                    ) {
                        Text("增加")
                    }
                    Text(
                        text = "在配置更改时会重置",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // RememberSaveable 计数器
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "RememberSaveable 计数器",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "值: $rememberSaveableCounter",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Button(
                        onClick = { 
                            rememberSaveableCounter++
                            Timber.d("HomeScreen RememberSaveable 计数器增加，当前值: $rememberSaveableCounter")
                        }
                    ) {
                        Text("增加")
                    }
                    Text(
                        text = "在配置更改时会保持",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // ViewModel 计数器
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "ViewModel 计数器 (共享)",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "值: $viewModelCounter",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Button(
                        onClick = { 
                            viewModel.incrementCounter()
                            Timber.d("HomeScreen ViewModel 计数器增加，当前值: $viewModelCounter")
                        }
                    ) {
                        Text("增加")
                    }
                    Text(
                        text = "使用共享的 ViewModel，状态会在屏幕间保持",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
