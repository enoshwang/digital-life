package com.hwzy.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.hwzy.app.navigation.AppNavigation
import com.hwzy.app.ui.theme.AppTheme
import timber.log.Timber

//  Activity 是用户界面的入口; Application 是后台运行的全局类，由系统隐式管理。
class MainActivity : ComponentActivity() {
    private val tag = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.tag(tag).d("onCreate")
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
}
