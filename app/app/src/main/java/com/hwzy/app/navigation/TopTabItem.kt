package com.hwzy.app.navigation

import android.os.Parcelable
import com.hwzy.app.R
import kotlinx.parcelize.Parcelize

@Parcelize
sealed class TopTabItem(
    @Suppress("unused") val route: String,
    val title: Int
) : Parcelable {
    object News : TopTabItem(
        route = "news",
        title = R.string.news
    )
    
    object HuiWen : TopTabItem(
        route = "huiwen",
        title = R.string.huiwen
    )
    
    object AI : TopTabItem(
        route = "ai",
        title = R.string.ai
    )

    companion object {
        val items: List<TopTabItem> by lazy {
            listOf(News, HuiWen, AI)
        }
    }
}
