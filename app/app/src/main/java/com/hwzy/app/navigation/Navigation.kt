package com.hwzy.app.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hwzy.app.ui.screens.discover.CameraScreen
import com.hwzy.app.ui.screens.discover.DiscoverScreen
import com.hwzy.app.ui.screens.discover.SensorScreen
import com.hwzy.app.ui.screens.discover.TestScreen
import com.hwzy.app.ui.screens.home.HomeScreen
import com.hwzy.app.ui.screens.profile.ProfileScreen
import com.hwzy.app.ui.screens.profile.SettingsScreen
import com.hwzy.app.ui.screens.search.SearchScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                BottomNavItem.items.forEach { item ->
                    val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = stringResource(item.labelResId)) },
                        label = { Text(stringResource(item.labelResId)) },
                        selected = selected,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = false
                                }
                                launchSingleTop = true
                                restoreState = false
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(
                route = Screen.Home.route,
                enterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(200)
                    )
                },
                exitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(200)
                    )
                }
            ) {
                HomeScreen(
                    onNavigateToDetail = { id ->
                        navController.navigate(Screen.HomeDetail.createRoute(id))
                    }
                )
            }
            
            composable(
                route = Screen.Discover.route,
                enterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(200)
                    )
                },
                exitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(200)
                    )
                }
            ) {
                DiscoverScreen(
                    onNavigateToDetail = { id ->
                        navController.navigate(Screen.DiscoverDetail.createRoute(id))
                    }
                )
            }
            
            composable(
                route = Screen.Profile.route,
                enterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(200)
                    )
                },
                exitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(200)
                    )
                }
            ) {
                ProfileScreen(
                    onNavigateToDetail = { id ->
                        navController.navigate(Screen.ProfileDetail.createRoute(id))
                    }
                )
            }
            
            // 预留子页面路由
            composable(
                route = Screen.HomeDetail.route,
                arguments = listOf(navArgument("id") { type = NavType.StringType })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("id")
                when (id) {
                    else -> {
                        null
                    }
                }
            }
            
            composable(
                route = Screen.DiscoverDetail.route,
                arguments = listOf(navArgument("id") { type = NavType.StringType })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("id")
                when (id) {
                    "search" -> SearchScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        onItemClick = { itemId ->
                            when (itemId) {
                                itemId -> navController.navigate(Screen.DiscoverDetail.createRoute(itemId))
                            }
                        }
                    )
                    "test" -> TestScreen(
                        onNavigateToDetail = { route ->
                            navController.navigate(route)
                        },
                        onBackClick = {
                            navController.popBackStack()
                        }
                    )
                    "camera" -> CameraScreen(
                        onBackClick = {
                            navController.popBackStack()
                        }
                    )
                    "sensor" -> SensorScreen(
                        onBackClick = {
                            navController.popBackStack()
                        }
                    )
                    else -> {
                        // 其他发现页面的子页面
                    }
                }
            }
            
            composable(
                route = Screen.ProfileDetail.route,
                arguments = listOf(navArgument("id") { type = NavType.StringType })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("id")
                when (id) {
                    "settings" -> SettingsScreen(
                        onNavigateToDetail = { route ->
                            navController.navigate(route)
                        },
                        onBackClick = {
                            navController.popBackStack()
                        }
                    )
                    else -> {
                        // 其他个人页面的子页面
                    }
                }
            }
        }
    }
}
