package com.hwzy.app.navigation

import timber.log.Timber

sealed class TopTabItem(
    val route: String,
    val title: String
) {
    object News : TopTabItem(
        route = "news",
        title = "资讯"
    )
    
    object HuiWen : TopTabItem(
        route = "huiwen",
        title = "慧文"
    )
    
    object AI : TopTabItem(
        route = "ai",
        title = "AI"
    )
    
    companion object {
        val items:  List<TopTabItem> by lazy {
            listOf(News, HuiWen, AI)
        }
    }
}
