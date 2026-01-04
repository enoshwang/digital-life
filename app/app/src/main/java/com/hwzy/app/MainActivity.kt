package com.hwzy.app

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.hwzy.app.permission.PermissionManager
import com.hwzy.app.ui.screens.CameraPreviewScreen
import com.hwzy.app.ui.theme.AppTheme
import net.surina.soundtouch.SoundTouch
import timber.log.Timber

//  Activity 是用户界面的入口; Application 是后台运行的全局类，由系统隐式管理。
class MainActivity : ComponentActivity() {
    private val tag = "MainActivity"
    private lateinit var permissionManager: PermissionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.tag(tag).d("onCreate")

        super.onCreate(savedInstanceState)

        permissionManager = PermissionManager(this)

        // 在主线程中初始化 OpenMP（如果启用）- 确保 SoundTouch 可以正确地运行, 同时打印版本信息
        val soundTouchVersion = SoundTouch.getVersionString()
        Timber.tag(tag).d("SoundTouch version: $soundTouchVersion")

        // 启用全屏沉浸式模式（包含状态栏）
        enableEdgeToEdge()
        
        // 使用 WindowInsetsController 隐藏系统UI（状态栏和导航栏），实现全屏沉浸式体验
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.apply {
            // 隐藏状态栏和导航栏
            hide(WindowInsetsCompat.Type.systemBars())
            // 设置沉浸式粘性模式（系统UI不会自动显示）
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        setContent {
            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PermissionCheck()
                }
            }
        }
    }

    @Composable
    private fun PermissionCheck() {
        val context = LocalContext.current
        var permissionsGranted by remember { mutableStateOf(false) }

        // 保持全屏模式
        DisposableEffect(Unit) {
            val activity = context as? ComponentActivity
            val windowInsetsController = activity?.let {
                WindowCompat.getInsetsController(it.window, it.window.decorView)
            }
            
            windowInsetsController?.apply {
                // 隐藏状态栏和导航栏
                hide(WindowInsetsCompat.Type.systemBars())
                // 设置沉浸式粘性模式
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
            
            onDispose { }
        }

        if (!permissionsGranted) {
            permissionManager.RequestPermissions(
                activity = this,
                onPermissionsGranted = {
                    permissionsGranted = true
                },
                onPermissionsDenied = {
                    finish()
                }
            )
        } else {
            CameraPreviewScreen()
        }
    }

    override fun onResume() {
        Timber.tag(tag).d("onResume")
        super.onResume()
    }

    override fun onPause() {
        Timber.tag(tag).d("onPause")
        super.onPause()
    }

    override fun onStart() {
        Timber.tag(tag).d("onStart")
        super.onStart()
    }

    override fun onStop() {
        Timber.tag(tag).d("onStop")
        super.onStop()
    }

    override fun onDestroy() {
        Timber.tag(tag).d("onDestroy")
        super.onDestroy()
    }

    override fun onRestart() {
        Timber.tag(tag).d("onRestart")
        super.onRestart()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        Timber.tag(tag).d("onKeyDown,  keyCode: $keyCode, event: $event")
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        Timber.tag(tag).d("onKeyDown,  keyCode: $keyCode, event: $event")
        return super.onKeyUp(keyCode, event)
    }
}
