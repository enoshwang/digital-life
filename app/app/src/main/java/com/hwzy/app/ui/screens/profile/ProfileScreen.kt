package com.hwzy.app.ui.screens.profile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.hwzy.app.components.ProfileTopBar
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    @Suppress("unused") onNavigateToDetail: (String) -> Unit
) {

    // 使用 LaunchedEffect 来跟踪屏幕的重新组合
    LaunchedEffect(Unit) {
        Timber.d("ProfileScreen 被重新组合")
    }

    // 使用 DisposableEffect 来跟踪屏幕的创建和销毁
    DisposableEffect(Unit) {
        Timber.d("ProfileScreen 被创建")
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
            Text(
                text = "test",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
