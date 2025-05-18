package com.hwzy.app.ui.screens.discover

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executor

private const val TAG = "CameraScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    
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

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            hasCameraPermission = permissions[Manifest.permission.CAMERA] == true
            hasAudioPermission = permissions[Manifest.permission.RECORD_AUDIO] == true
        }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission || !hasAudioPermission) {
            launcher.launch(arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ))
        }
    }

    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var videoCapture: VideoCapture<Recorder>? by remember { mutableStateOf(null) }
    var recording: Recording? by remember { mutableStateOf(null) }
    var isRecording by remember { mutableStateOf(false) }
    var luminosity by remember { mutableFloatStateOf(0f) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var cameraSelector by remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }
    var showCameraSelector by remember { mutableStateOf(false) }
    
    // 获取所有可用的相机镜头
    val availableCameraSelectors = remember {
        val selectors = mutableListOf<Pair<CameraSelector, String>>()
        
        // 添加前置相机
        selectors.add(CameraSelector.DEFAULT_FRONT_CAMERA to "前置相机")
        
        // 添加后置相机
        selectors.add(CameraSelector.DEFAULT_BACK_CAMERA to "主摄")
        
        // 添加超广角相机
        try {
            val ultraWideCamera = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .addCameraFilter { cameras ->
                    cameras.filter { camera ->
                        camera.lensFacing == CameraSelector.LENS_FACING_BACK
                    }
                }
                .build()
            selectors.add(ultraWideCamera to "超广角")
        } catch (e: Exception) {
            Timber.tag(TAG).d("设备不支持超广角相机：${e.message}")
        }
        
        // 添加长焦相机
        try {
            val telephotoCamera = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .addCameraFilter { cameras ->
                    cameras.filter { camera ->
                        camera.lensFacing == CameraSelector.LENS_FACING_BACK
                    }
                }
                .build()
            selectors.add(telephotoCamera to "长焦")
        } catch (e: Exception) {
            Timber.tag(TAG).d("设备不支持长焦相机：${e.message}")
        }
        
        selectors
    }

    // 停止录制
    val stopRecording: () -> Unit = {
        try {
            if (isRecording && recording != null) {
                Timber.tag(TAG).d("正在停止录制...")
                recording?.apply {
                    stop()
                    Timber.tag(TAG).d("录制已停止")
                }
                recording = null
                isRecording = false
            } else {
                Timber.tag(TAG).d("没有正在进行的录制")
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "停止录制时发生错误")
            isRecording = false
            recording = null
        }
    }

    // 相机初始化函数
    fun initializeCamera(
        context: android.content.Context,
        lifecycleOwner: androidx.lifecycle.LifecycleOwner,
        previewView: PreviewView
    ) {
        // 如果正在录制，先停止录制
        if (isRecording) {
            stopRecording()
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                // 解绑所有用例
                cameraProvider?.unbindAll()
                
                cameraProvider = cameraProviderFuture.get()
                
                // 检查相机是否可用
                if (!cameraProvider?.hasCamera(cameraSelector)!!) {
                    Timber.tag(TAG).e("相机不可用: $cameraSelector")
                    return@addListener
                }
                
                val preview = Preview.Builder().build()
                preview.surfaceProvider = previewView.surfaceProvider

                // 设置图像捕获
                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                    .build()

                // 设置视频录制
                val recorder = Recorder.Builder()
                    .setQualitySelector(
                        QualitySelector.from(
                            Quality.HIGHEST,
                            FallbackStrategy.higherQualityOrLowerThan(Quality.SD)
                        )
                    )
                    .build()
                videoCapture = VideoCapture.withOutput(recorder)

                // 设置图像分析
                val imageAnalyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(
                            ContextCompat.getMainExecutor(context),
                            LuminosityAnalyzer { luma ->
                                luminosity = luma
                            }
                        )
                    }

                try {
                    cameraProvider?.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture,
                        videoCapture,
                        imageAnalyzer
                    )
                } catch (e: Exception) {
                    Timber.tag(TAG).e(e, "绑定相机用例时发生错误")
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "初始化相机时发生错误")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    // 开始录制
    fun startRecording(
        context: android.content.Context,
        videoCapture: VideoCapture<Recorder>,
        executor: Executor
    ) {
        if (isRecording) {
            Timber.tag(TAG).d("已经在录制中")
            return
        }

        // 检查录音权限
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Timber.tag(TAG).e("录音权限未授予")
            return
        }

        val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.CHINA)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CameraX-Video")
        }

        val mediaStoreOutputOptions = MediaStoreOutputOptions
            .Builder(context.contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()

        try {
            Timber.tag(TAG).d("开始准备录制...")
            val newRecording = videoCapture.output
                .prepareRecording(context, mediaStoreOutputOptions)
                .withAudioEnabled()
                .start(executor) { event ->
                    when (event) {
                        is VideoRecordEvent.Start -> {
                            Timber.tag(TAG).d("录制已开始")
                            isRecording = true
                        }
                        is VideoRecordEvent.Finalize -> {
                            if (event.hasError()) {
                                Timber.tag(TAG).e("录制视频时发生错误: ${event.error}")
                                isRecording = false
                                recording = null
                            } else {
                                val msg = "视频已保存: ${event.outputResults.outputUri}"
                                Timber.tag(TAG).d(msg)
                                isRecording = false
                                recording = null
                            }
                        }
                    }
                }
            recording = newRecording
            Timber.tag(TAG).d("录制对象已创建")
        } catch (e: SecurityException) {
            Timber.tag(TAG).e(e, "录音权限被拒绝")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "录制视频时发生错误")
            isRecording = false
            recording = null
        }
    }

    // 在组件销毁时停止录制
    DisposableEffect(Unit) {
        onDispose {
            if (isRecording) {
                stopRecording()
            }
            cameraProvider?.unbindAll()
        }
    }

    // 监听相机选择器变化
    LaunchedEffect(cameraSelector) {
        previewView?.let { view ->
            initializeCamera(context, lifecycleOwner, view)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("相机") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RectangleShape
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            if (hasCameraPermission) {
                AndroidView(
                    factory = { context ->
                        PreviewView(context).apply {
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                            previewView = this
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { view ->
                        previewView = view
                    }
                )

                // 底部控制栏
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.BottomCenter),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 镜头切换按钮
                    IconButton(
                        onClick = { showCameraSelector = true },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cameraswitch,
                            contentDescription = "切换镜头",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // 拍照和录像按钮
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 拍照按钮
                        Button(
                            onClick = {
                                imageCapture?.let { capture ->
                                    takePhoto(
                                        context = context,
                                        imageCapture = capture,
                                        executor = ContextCompat.getMainExecutor(context)
                                    )
                                }
                            },
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Camera,
                                contentDescription = "拍照",
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        // 录像按钮
                        Button(
                            onClick = {
                                if (isRecording) {
                                    Timber.tag(TAG).d("点击停止录制按钮")
                                    stopRecording()
                                } else {
                                    Timber.tag(TAG).d("点击开始录制按钮")
                                    videoCapture?.let { capture ->
                                        startRecording(
                                            context = context,
                                            videoCapture = capture,
                                            executor = ContextCompat.getMainExecutor(context)
                                        )
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRecording) 
                                    MaterialTheme.colorScheme.error 
                                else 
                                    MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = if (isRecording) Icons.Default.VideocamOff else Icons.Default.Videocam,
                                contentDescription = if (isRecording) "停止录像" else "开始录像",
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                // 显示亮度值
                Text(
                    text = "亮度: %.2f".format(luminosity),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                // 镜头选择对话框
                if (showCameraSelector) {
                    AlertDialog(
                        onDismissRequest = { showCameraSelector = false },
                        title = { Text("选择镜头") },
                        text = {
                            Column {
                                availableCameraSelectors.forEach { (selector, name) ->
                                    TextButton(
                                        onClick = {
                                            // 如果正在录制，先停止录制
                                            if (isRecording) {
                                                stopRecording()
                                            }
                                            cameraSelector = selector
                                            showCameraSelector = false
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(name)
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showCameraSelector = false }) {
                                Text("取消")
                            }
                        }
                    )
                }
            } else {
                Text(
                    text = "需要相机权限才能使用此功能",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

private fun takePhoto(
    context: android.content.Context,
    imageCapture: ImageCapture,
    executor: Executor
) {
    val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.CHINA)
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

    imageCapture.takePicture(
        outputOptions,
        executor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onError(exc: ImageCaptureException) {
                exc.printStackTrace()
            }

            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                val msg = "照片已保存: ${output.savedUri}"
                Timber.tag("CameraXApp").d(msg)
            }
        }
    )
}

private class LuminosityAnalyzer(private val listener: (Float) -> Unit) : ImageAnalysis.Analyzer {
    override fun analyze(image: androidx.camera.core.ImageProxy) {
        val buffer = image.planes[0].buffer
        val data = ByteArray(buffer.remaining())
        buffer.get(data)
        val pixels = data.map { it.toInt() and 0xFF }
        val luma = pixels.average()
        listener(luma.toFloat())
        image.close()
    }
}
