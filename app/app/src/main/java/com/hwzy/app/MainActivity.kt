package com.hwzy.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hwzy.app.ui.theme.AppTheme
import timber.log.Timber

//  Activity 是用户界面的入口; Application 是后台运行的全局类，由系统隐式管理。
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("onCreate")
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

// 定义导航项
// 定义导航项
sealed class Screen(val route: String, val resourceId: Int, val icon: ImageVector) {
    object Home : Screen("home", R.string.home, Icons.Default.Home)
    object Discover : Screen("discover", R.string.discover, Icons.Default.Search)
    object Profile : Screen("profile", R.string.profile, Icons.Default.Person)
}

// 应用导航
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val screens = listOf(Screen.Home, Screen.Discover, Screen.Profile)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            if (currentDestination?.route in screens.map { it.route }) {
                BottomNavigationBar(
                    screens = screens,
                    currentDestination = currentDestination,
                    onNavigateToDestination = { screen ->
                        navController.navigate(screen.route) {
                            // 防止重复添加相同目的地
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(route = Screen.Home.route) {
                HomeScreen()
            }
            composable(route = Screen.Discover.route) {
                DiscoverScreen()
            }
            composable(route = Screen.Profile.route) {
                ProfileScreen()
            }
        }
    }
}

// 底部导航栏组件
@Composable
fun BottomNavigationBar(
    screens: List<Screen>,
    currentDestination: androidx.navigation.NavDestination?,
    onNavigateToDestination: (Screen) -> Unit
) {
    NavigationBar {
        screens.forEach { screen ->
            val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
            val iconTint = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }

            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = screen.icon,
                        contentDescription = stringResource(screen.resourceId),
                        tint = iconTint
                    )
                },
                label = { Text(stringResource(screen.resourceId)) },
                selected = selected,
                onClick = { onNavigateToDestination(screen) },
                alwaysShowLabel = true
            )
        }
    }
}

// 示例页面
@Composable
fun HomeScreen() {
    Text("主页")
}

@Composable
fun DiscoverScreen() {
    Text("发现")
}

@Composable
fun ProfileScreen() {
    Text("我的")
}
