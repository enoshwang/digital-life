package com.hwzy.app.ui.screens.discover

import android.Manifest
import android.annotation.SuppressLint
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
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { previewView ->
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
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
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    imageCapture,
                                    videoCapture,
                                    imageAnalyzer
                                )
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }, ContextCompat.getMainExecutor(context))
                    }
                )

                // 底部控制栏
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.BottomCenter),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // 拍照按钮
                    IconButton(
                        onClick = {
                            imageCapture?.let { capture ->
                                takePhoto(
                                    context = context,
                                    imageCapture = capture,
                                    executor = ContextCompat.getMainExecutor(context)
                                )
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Camera,
                            contentDescription = "拍照",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // 录像按钮
                    IconButton(
                        onClick = {
                            if (isRecording) {
                                recording?.stop()
                                recording = null
                                isRecording = false
                            } else {
                                videoCapture?.let { capture ->
                                    startRecording(
                                        context = context,
                                        videoCapture = capture,
                                        executor = ContextCompat.getMainExecutor(context)
                                    ) { newRecording ->
                                        recording = newRecording
                                        isRecording = true
                                    }
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isRecording) Icons.Default.VideocamOff else Icons.Default.Videocam,
                            contentDescription = if (isRecording) "停止录像" else "开始录像",
                            tint = MaterialTheme.colorScheme.primary
                        )
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

@SuppressLint("CheckResult")
private fun startRecording(
    context: android.content.Context,
    videoCapture: VideoCapture<Recorder>,
    executor: Executor,
    @Suppress("unused") onRecordingStarted: (Recording) -> Unit
) {
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
        videoCapture.output
            .prepareRecording(context, mediaStoreOutputOptions)
            .withAudioEnabled()
            .start(executor) { event ->
                when (event) {
                    is VideoRecordEvent.Start -> {
                        // 录制已经开始，不需要额外的处理
                        Timber.tag(TAG).d("开始录制视频")
                    }
                    is VideoRecordEvent.Finalize -> {
                        if (event.hasError()) {
                            Timber.tag(TAG).e("录制视频时发生错误: ${event.error}")
                        } else {
                            val msg = "视频已保存: ${event.outputResults.outputUri}"
                            Timber.tag(TAG).d("msg:$msg")
                        }
                    }
                }
            }
    } catch (e: SecurityException) {
        Timber.tag(TAG).e(e, "录音权限被拒绝")
    } catch (e: Exception) {
        Timber.tag(TAG).e(e, "录制视频时发生错误")
    }
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
