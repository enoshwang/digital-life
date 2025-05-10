package com.hwzy.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector
import com.hwzy.app.R

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Discover : Screen("discover")
    object Profile : Screen("profile")
    
    // 子页面路由
    object HomeDetail : Screen("home/{id}") {
        fun createRoute(id: String) = "home/$id"
    }
    
    object DiscoverDetail : Screen("discover/{id}") {
        fun createRoute(id: String) = "discover/$id"
    }
    
    object ProfileDetail : Screen("profile/{id}") {
        fun createRoute(id: String) = "profile/$id"
    }
}

sealed class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val labelResId: Int
) {
    object Home : BottomNavItem(
        route = Screen.Home.route,
        icon = Icons.Default.Home,
        labelResId = R.string.home
    )
    
    object Discover : BottomNavItem(
        route = Screen.Discover.route,
        icon = Icons.Default.Search,
        labelResId = R.string.discover
    )
    
    object Profile : BottomNavItem(
        route = Screen.Profile.route,
        icon = Icons.Default.Person,
        labelResId = R.string.profile
    )
    
    companion object {
        val items = listOf(Home, Discover, Profile)
    }
}
