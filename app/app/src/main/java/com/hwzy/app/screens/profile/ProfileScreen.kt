package com.hwzy.app.screens.profile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hwzy.app.components.ProfileTopBar
import com.hwzy.app.screens.home.HomeViewModel
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateToDetail: (String) -> Unit,
    viewModel: HomeViewModel = viewModel(
        viewModelStoreOwner = LocalContext.current as androidx.lifecycle.ViewModelStoreOwner
    )
) {
    // 使用 ViewModel 保存状态
    val viewModelCounter by viewModel.counter.collectAsState()

    // 使用 LaunchedEffect 来跟踪屏幕的重新组合
    LaunchedEffect(Unit) {
        Timber.d("ProfileScreen 被重新组合，当前计数器值: $viewModelCounter")
    }

    // 使用 DisposableEffect 来跟踪屏幕的创建和销毁
    DisposableEffect(Unit) {
        Timber.d("ProfileScreen 被创建，ViewModel 实例: $viewModel")
        onDispose {
            Timber.d("ProfileScreen 被销毁")
        }
    }

    Scaffold(
        topBar = {
            ProfileTopBar(
                avatarUrl = null,
                username = "游客",
                userId = "ID: 88888888"
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            // ViewModel 计数器
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "HomeViewModel 计数器 (在 ProfileScreen 中)",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "值: $viewModelCounter",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Button(
                        onClick = { 
                            viewModel.incrementCounter()
                            Timber.d("ProfileScreen 中按钮被点击，当前值: $viewModelCounter")
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
