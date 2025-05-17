package com.hwzy.app.permission

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import timber.log.Timber

class PermissionManager(private val context: Context) {
    private val tag = "PermissionManager"
    
    // 定义所有需要的权限
    private val requiredPermissions = arrayOf(
        Manifest.permission.CAMERA
    )

    // 检查是否所有权限都已授予
    fun hasAllPermissions(): Boolean {
        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    // 获取未授予的权限列表
    @Suppress("unused")
    fun getDeniedPermissions(): List<String> {
        return requiredPermissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
    }

    // 打开应用设置页面
    fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    @Composable
    fun RequestPermissions(
        @Suppress("unused") activity: ComponentActivity,
        onPermissionsGranted: () -> Unit,
        onPermissionsDenied: () -> Unit
    ) {
        var showRationaleDialog by remember { mutableStateOf(false) }
        var deniedPermissions by remember { mutableStateOf(emptyList<String>()) }

        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val allGranted = permissions.entries.all { it.value }
            if (allGranted) {
                Timber.tag(tag).d("所有权限已授予")
                onPermissionsGranted()
            } else {
                deniedPermissions = permissions.entries
                    .filter { !it.value }
                    .map { it.key }
                showRationaleDialog = true
            }
        }

        LaunchedEffect(Unit) {
            if (hasAllPermissions()) {
                onPermissionsGranted()
            } else {
                permissionLauncher.launch(requiredPermissions)
            }
        }

        if (showRationaleDialog) {
            AlertDialog(
                onDismissRequest = { onPermissionsDenied() },
                title = { Text("需要权限") },
                text = { 
                    Text("应用需要以下权限才能正常运行：\n" +
                        deniedPermissions.joinToString("\n") { 
                            when(it) {
                                Manifest.permission.CAMERA -> "相机权限"
                                else -> it
                            }
                        }
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        showRationaleDialog = false
                        openAppSettings()
                        onPermissionsDenied()
                    }) {
                        Text("去设置")
                    }
                },
                dismissButton = {
                    Button(onClick = {
                        showRationaleDialog = false
                        onPermissionsDenied()
                    }) {
                        Text("退出应用")
                    }
                }
            )
        }
    }
}
