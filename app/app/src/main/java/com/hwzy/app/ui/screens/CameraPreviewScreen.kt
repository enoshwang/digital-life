package com.hwzy.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import timber.log.Timber

private const val TAG = "CameraPreviewScreen"

@Composable
fun CameraPreviewScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // 权限状态
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    
    // 控制按钮显示/隐藏
    var showControls by remember { mutableStateOf(true) }
    
    // 权限请求
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }
    
    // 请求权限
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    
    // 相机相关状态
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    
    // 初始化相机
    fun initializeCamera(previewView: PreviewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider?.unbindAll()
                cameraProvider = cameraProviderFuture.get()
                
                val preview = Preview.Builder().build()
                preview.surfaceProvider = previewView.surfaceProvider
                
                cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview
                )
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "初始化相机失败")
            }
        }, ContextCompat.getMainExecutor(context))
    }
    
    // 清理相机资源
    DisposableEffect(Unit) {
        onDispose {
            cameraProvider?.unbindAll()
        }
    }
    
    // 监听预览视图变化
    LaunchedEffect(previewView) {
        previewView?.let { view ->
            initializeCamera(view)
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { showControls = !showControls }
    ) {
        // 摄像头预览
        if (hasCameraPermission) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        previewView = this
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { view ->
                    previewView = view
                }
            )
        }
        
        // 控制按钮（点击屏幕显示/隐藏）
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        ) {
            IconButton(
                onClick = { /* 拍照功能可以在这里添加 */ },
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.3f))
            ) {
                Icon(
                    imageVector = Icons.Default.Camera,
                    contentDescription = "拍照",
                    modifier = Modifier.size(36.dp),
                    tint = Color.White
                )
            }
        }
    }
}

