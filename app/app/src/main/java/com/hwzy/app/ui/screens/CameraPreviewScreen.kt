package com.hwzy.app.ui.screens

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

private const val TAG = "CameraPreviewScreen"

@Composable
fun CameraPreviewScreen() {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    
    // 权限状态
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    
    // 控制按钮显示/隐藏
    var showControls by remember { mutableStateOf(true) }
    
    // 权限请求
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasCameraPermission = permissions[Manifest.permission.CAMERA] == true
        hasAudioPermission = permissions[Manifest.permission.RECORD_AUDIO] == true
    }
    
    // 请求权限
    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf<String>()
        if (!hasCameraPermission) {
            permissionsToRequest.add(Manifest.permission.CAMERA)
        }
        if (!hasAudioPermission) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
        }
        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }
    
    // 相机相关状态
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var cameraSelector by remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    
    // 录音相关状态
    var isRecording by remember { mutableStateOf(false) }
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    
    // 录音动效
    val infiniteTransition = rememberInfiniteTransition(label = "recording")
    val recordingScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "recordingScale"
    )
    
    // 初始化相机
    fun initializeCamera(previewView: PreviewView, selector: CameraSelector) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider?.unbindAll()
                cameraProvider = cameraProviderFuture.get()
                
                val preview = Preview.Builder().build()
                preview.surfaceProvider = previewView.surfaceProvider
                
                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                    .build()
                
                cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    selector,
                    preview,
                    imageCapture
                )
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "初始化相机失败")
            }
        }, ContextCompat.getMainExecutor(context))
    }
    
    // 拍照功能
    fun takePhoto() {
        imageCapture?.let { capture ->
            val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.getDefault())
                .format(System.currentTimeMillis())
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
            }
            
            val outputOptions = ImageCapture.OutputFileOptions
                .Builder(
                    context.contentResolver,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                )
                .build()
            
            capture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onError(exception: ImageCaptureException) {
                        Timber.tag(TAG).e(exception, "拍照失败")
                    }
                    
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        Timber.tag(TAG).d("照片已保存: ${output.savedUri}")
                    }
                }
            )
        }
    }
    
    // 开始录音
    fun startRecording() {
        if (isRecording) return
        
        try {
            val recorder = MediaRecorder(context)

            val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.getDefault())
                .format(System.currentTimeMillis())
            val file = File(context.getExternalFilesDir(null), "$name.m4a")
            
            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            
            mediaRecorder = recorder
            isRecording = true
            Timber.tag(TAG).d("开始录音: ${file.absolutePath}")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "开始录音失败")
        }
    }
    
    // 停止录音
    fun stopRecording() {
        if (!isRecording) return
        
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecording = false
            Timber.tag(TAG).d("停止录音")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "停止录音失败")
            mediaRecorder = null
            isRecording = false
        }
    }
    
    // 切换摄像头
    fun switchCamera() {
        cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
    }
    
    // 清理相机和录音资源
    DisposableEffect(Unit) {
        onDispose {
            stopRecording()
            cameraProvider?.unbindAll()
        }
    }
    
    // 监听预览视图和摄像头选择器变化
    LaunchedEffect(previewView, cameraSelector) {
        previewView?.let { view ->
            initializeCamera(view, cameraSelector)
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧：拍照按钮
                IconButton(
                    onClick = { takePhoto() },
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.3f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Camera,
                        contentDescription = "拍照",
                        modifier = Modifier.size(28.dp),
                        tint = Color.White
                    )
                }
                
                // 按钮之间的间距
                Spacer(modifier = Modifier.size(24.dp))
                
                // 中间：按住说话按钮（带录音动效）
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                
                // 监听按下和释放状态
                LaunchedEffect(isPressed) {
                    if (isPressed && !isRecording && hasAudioPermission) {
                        startRecording()
                    } else if (!isPressed && isRecording) {
                        stopRecording()
                    }
                }
                
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .scale(if (isRecording) recordingScale else 1f)
                        .clip(CircleShape)
                        .background(
                            if (isRecording) Color.Red.copy(alpha = 0.7f)
                            else Color.White.copy(alpha = 0.3f)
                        )
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) { }
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "按住说话",
                        modifier = Modifier
                            .size(36.dp)
                            .align(Alignment.Center),
                        tint = Color.White
                    )
                }
                
                // 按钮之间的间距
                Spacer(modifier = Modifier.size(24.dp))
                
                // 右侧：切换摄像头按钮
                IconButton(
                    onClick = { switchCamera() },
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.3f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = "切换摄像头",
                        modifier = Modifier.size(28.dp),
                        tint = Color.White
                    )
                }
            }
        }
    }
}
