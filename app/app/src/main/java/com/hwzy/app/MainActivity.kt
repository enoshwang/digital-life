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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.hwzy.app.navigation.AppNavigation
import com.hwzy.app.permission.PermissionManager
import com.hwzy.app.ui.theme.AppTheme
import timber.log.Timber

//  Activity 是用户界面的入口; Application 是后台运行的全局类，由系统隐式管理。
class MainActivity : ComponentActivity() {
    private val tag = "MainActivity"
    private lateinit var permissionManager: PermissionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.tag(tag).d("onCreate")
        super.onCreate(savedInstanceState)
        permissionManager = PermissionManager(this)

        enableEdgeToEdge()
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
        var permissionsGranted by remember { mutableStateOf(false) }

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
            AppNavigation()
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
