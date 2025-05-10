package com.hwzy.app.screens.profile

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
import com.hwzy.app.components.ProfileTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateToDetail: (String) -> Unit
) {
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
                text = "设置",
                modifier = Modifier
                    .padding(16.dp)
                    .clickable {
                        onNavigateToDetail("settings")
                    }
            )
        }
    }
}
