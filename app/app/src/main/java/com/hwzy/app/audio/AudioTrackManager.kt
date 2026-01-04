package com.hwzy.app.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.IOException

object AudioTrackManager {
    private const val TAG = "AudioTrackManager"

    // wav 文件头
    private const val PCM_HEADER_SIZE = 78

    // 初始化相关
    @Volatile
    private var isInitialized = false
    private lateinit var appContext: Context

    // 固定 AudioTrack 对象 - 低延迟
    private val audioTrackMap = mutableMapOf<String, AudioTrack>()
    // 对应文件的帧数（用于 marker 回调，单位：frame）
    private val audioTrackFrameCount = mutableMapOf<String, Int>()

    // 如果在播放音频时，发现文件都不存在，则尝试再加载一次
    private var loadAgainCount = 2

    // 音频数据缓存
    private val audioDataCache = HashMap<String, ByteArray>()
    // 存储动态 AudioTrack 对象
    private val dynamicAudioTrackMap = mutableMapOf<String, AudioTrack>()
    // 记录 AudioTrack 最后使用时间
    private val audioTrackLastUsedTime = mutableMapOf<String, Long>()
    // 最大缓存数量
    private const val MAX_CACHE_SIZE = 5
    // 缓存过期时间（毫秒） - 1分钟
    private const val CACHE_EXPIRE_TIME = 1 * 60 * 1000L
    // 定期清理任务
    private var cleanupJob: Job? = null

    fun initialize(context: Context) {
        if (isInitialized) return
        appContext = context.applicationContext
        Timber.tag(TAG).d("on AudioTrackManager initialize")
        
        CoroutineScope(Dispatchers.IO).launch {
            preloadAudioFiles()
        }
        startCleanupJob()
        isInitialized = true
        Timber.tag("MyApp").d("AliyunApikeyManager初始化$isInitialized")

    }

    /**
     * 定期清理未使用的 AudioTrack
     */
    private fun startCleanupJob() {
        cleanupJob?.cancel()
        cleanupJob = CoroutineScope(Dispatchers.IO).launch {
            Timber.tag(TAG).d("on cleanup job: start")
            while (true) {
                try {
                    cleanupUnusedAudioTracks()
                    delay(60 * 1000L)
                } catch (e: Exception) {
                    Timber.tag(TAG).w("on cleanup job: error: ${e.message}")
                }
            }
        }
    }

    //  获取所有 WAV 文件
    private fun getAllWavFiles(directory: File): List<File> {
        val wavFiles = mutableListOf<File>()

        // 检查目录是否存在且可读
        if (!directory.exists() || !directory.isDirectory || !directory.canRead()) {
            Timber.tag(TAG).w("on getAllWavFiles: directory does not exist or is not readable. directory=${directory.path}")
            return wavFiles
        }

        // 遍历目录下的所有内容
        directory.listFiles()?.forEach { file ->
            when {
                // 如果是 .wav 文件，添加到结果列表
                file.isFile && file.name.lowercase().endsWith(".wav") -> {
                    // 如果列表存在相同文件名，则忽略这个文件
                    if (wavFiles.any { it.name == file.name }) {
                        Timber.tag(TAG).w("on getAllWavFiles: duplicate file name found: filePath=${file.path}")
                    } else {
                        wavFiles.add(file)
                    }
                }
                // 如果是目录，递归搜索
                file.isDirectory -> {
                    wavFiles.addAll(getAllWavFiles(file))
                }
            }
        }

        return wavFiles
    }

    // 预加载所有 PCM 文件 - 从 assert/audio 目录
    private fun preloadAudioFiles() {
        val assetManager = appContext.assets
        try {
            // 列出 assets/audio/ 下的所有文件
            val fileNames = assetManager.list("audio") ?: emptyArray()

            // 过滤出 .wav 文件（不区分大小写）
            val wavFiles = fileNames.filter { it.endsWith(".wav", ignoreCase = true) }

            for (fileName in wavFiles) {
                val assetPath = "audio/$fileName"
                try {
                    // 从 assets 读取文件为 ByteArray
                    val pcmData = assetManager.open(assetPath).use { it.readBytes() }

                    // 缓存原始数据
                    audioDataCache.put(fileName, pcmData)
                } catch (e: Exception) {
                    Timber.tag(TAG).w("on preloadAudioFiles: error loading asset: $assetPath, ${e.message}")
                }
            }

            Timber.tag(TAG).d("on preloadAudioFiles: find ${wavFiles.size} audio files, audioTrackMap keys=${audioTrackMap.keys}")
            Timber.tag(TAG).d("on preloadAudioFiles: cache size: ${audioDataCache.size}, keys=${audioDataCache.keys}")

        } catch (e: IOException) {
            Timber.tag(TAG).e(e, "Failed to list assets in audio/")
        }
    }

    private fun createAudioTrack(pcmDataSize: Int): AudioTrack {
        // 根据 PCM 文件格式配置 AudioTrack
        val sampleRate = 16000
        val channelConfig = AudioFormat.CHANNEL_OUT_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT

        val bufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)

        return AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(audioFormat)
                    .setSampleRate(sampleRate)
                    .setChannelMask(channelConfig)
                    .build()
            )
            .setBufferSizeInBytes(pcmDataSize.coerceAtLeast(bufferSize))
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
    }

    // 加载音频文件
    private fun loadAudioFile(filePath: String): ByteArray {
        return try {
            FileInputStream(filePath).use { input ->
                val bytes = input.readBytes()
                Timber.tag(TAG).v("on loadAudioFile: read audio file: $filePath, size: ${bytes.size} bytes")
                bytes
            }
        } catch (e: IOException) {
            Timber.tag(TAG).w("on loadAudioFile: error on read audio file: ${e.message}, filePath: $filePath")
            byteArrayOf()
        }
    }

    // 打断当前播放
    private fun stopAudio() {
        Timber.tag(TAG).d("on stopAudio")
        // 打断 audioTrackMap 中的所有 AudioTrack
        for (audioTrack in audioTrackMap.values) {
            audioTrack.let {
                if (it.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    it.stop()
                }
            }
        }

        dynamicAudioTrackMap.forEach { (_, audioTrack) ->
            audioTrack.let {
                if (it.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    it.stop()
                }
            }
        }
    }

    // 定时清理缓存
    private fun cleanupUnusedAudioTracks() {
        val currentTime = System.currentTimeMillis()
        val expiredTracks = audioTrackLastUsedTime.filter { (_, lastUsedTime) ->
            currentTime - lastUsedTime > CACHE_EXPIRE_TIME
        }
        
        expiredTracks.forEach { (fileName, _) ->
            dynamicAudioTrackMap[fileName]?.let { audioTrack ->
                try {
                    if (audioTrack.playState == AudioTrack.PLAYSTATE_PLAYING) {
                        audioTrack.stop()
                    }
                    audioTrack.release()
                } catch (e: Exception) {
                    Timber.tag(TAG).w("on cleanupUnusedAudioTracks: error on release audio track: ${e.message}")
                }
            }
            dynamicAudioTrackMap.remove(fileName)
            audioTrackLastUsedTime.remove(fileName)
            Timber.tag(TAG).d("on cleanupUnusedAudioTracks: release audio track: $fileName")
        }
    }

    // 保证不超过最大缓存数量
    private fun manageCacheSize() {
        // 如果缓存数量超过限制，删除最久未使用的
        if (dynamicAudioTrackMap.size > MAX_CACHE_SIZE) {
            val oldestTracks = audioTrackLastUsedTime.entries.sortedBy { it.value }.take(dynamicAudioTrackMap.size - MAX_CACHE_SIZE)

            oldestTracks.forEach { (fileName, _) ->
                dynamicAudioTrackMap[fileName]?.let { audioTrack ->
                    try {
                        if (audioTrack.playState == AudioTrack.PLAYSTATE_PLAYING) {
                            audioTrack.stop()
                        }
                        audioTrack.release()
                    } catch (e: Exception) {
                        Timber.tag(TAG).w("on cleanupUnusedAudioTracks: error on release audio track: ${e.message}")
                    }
                }
                dynamicAudioTrackMap.remove(fileName)
                audioTrackLastUsedTime.remove(fileName)
                Timber.tag(TAG).d("on manageCacheSize: release audio track: $fileName")
            }
        }
    }

    // 动态 AudioTrack 播放
    private fun playAudioFileWithDynamicAudioTrack(fileName: String) {
        try {
            // 从缓存中获取音频数据
            val audioData = audioDataCache[fileName] ?: run {
                Timber.tag(TAG).w("on playAudioFileWithDynamicAudioTrack: audio data not found in cache: $fileName")
                return
            }

            // 获取已存在的 AudioTrack 或创建新的
            var audioTrack = dynamicAudioTrackMap[fileName]
            if (audioTrack == null) {
                val pcmDataSize = audioData.size - PCM_HEADER_SIZE
                audioTrack = createAudioTrack(pcmDataSize)
                audioTrack.write(audioData, PCM_HEADER_SIZE, pcmDataSize)
                dynamicAudioTrackMap[fileName] = audioTrack
                audioTrackFrameCount[fileName] = (pcmDataSize / 2)
                Timber.tag(TAG).d("on playAudioFileWithDynamicAudioTrack: create new audio track: $fileName")
                manageCacheSize()
            }

            // 更新最后使用时间
            audioTrackLastUsedTime[fileName] = System.currentTimeMillis()

            // 播放音频
            synchronized(audioTrack) {
                if (audioTrack.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    audioTrack.stop()
                }
                audioTrack.playbackHeadPosition = 0
                Timber.tag(TAG).d("on playAudioFileWithDynamicAudioTrack: start play audio file: $fileName")
                audioTrack.play()
            }
        } catch (e: Exception) {
            Timber.tag(TAG).w("on playAudioFileWithDynamicAudioTrack: error: ${e.message}")
        }
    }

    // 按 baseName 停止播放
    fun stopAudioByBaseName(baseName: String) {
        try {
            Timber.tag(TAG).d("on stopAudioByBaseName: baseName=$baseName")

            val fileName = "$baseName.wav"
            audioTrackMap[fileName]?.let { audioTrack ->
                synchronized(audioTrack) {
                    if (audioTrack.playState == AudioTrack.PLAYSTATE_PLAYING) {
                        audioTrack.stop()
                    }
                }
            }

            dynamicAudioTrackMap[fileName]?.let { audioTrack ->
                synchronized(audioTrack) {
                    if (audioTrack.playState == AudioTrack.PLAYSTATE_PLAYING) {
                        audioTrack.stop()
                    }
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).w("on stopAudioByBaseName: error=${e.message}")
        }
    }

    // 带完成回调的播放（建议在需要获知结束时使用）
    fun playAudioFileWithListener(baseName: String, volume: Float? = null, onPlay: (() -> Unit)? = null, onCompleted: (() -> Unit)? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Timber.tag(TAG).d("on playAudioFileWithListener: baseName=$baseName")

                // 处理打断事件
                if (baseName.isEmpty()) {
                    stopAudio()
                    return@launch
                }

                // 尝试重新加载音频文件
                if(audioTrackMap.keys.isEmpty() && audioDataCache.isEmpty() && loadAgainCount > 0) {
                    Timber.tag(TAG).d("on playAudioFileWithListener: audio file list is null , try load again")
                    --loadAgainCount
                    preloadAudioFiles()
                }

                // 在 audioTrackMap 中查找对应的音频文件
                var fileName = audioTrackMap.keys.find { it == "${baseName}.wav" }
                if (fileName == null) {
                    fileName = audioDataCache.keys.find { it == "${baseName}.wav" }
                    if (fileName == null) {
                        Timber.tag(TAG).w("on playAudioFileWithListener: audio file not found: $baseName")
                        return@launch
                    } else {
                        try {
                            // 从缓存中获取音频数据
                            val audioData = audioDataCache[fileName] ?: run {
                                Timber.tag(TAG).w("on playAudioFileWithListener: audio data not found in cache: $fileName")
                                return@launch
                            }

                            // 获取已存在的 AudioTrack 或创建新的
                            var audioTrack = dynamicAudioTrackMap[fileName]
                            if (audioTrack == null) {
                                val pcmDataSize = audioData.size - PCM_HEADER_SIZE
                                audioTrack = createAudioTrack(pcmDataSize)
                                audioTrack.write(audioData, PCM_HEADER_SIZE, pcmDataSize)
                                dynamicAudioTrackMap[fileName] = audioTrack
                                audioTrackFrameCount[fileName] = (pcmDataSize / 2)
                                Timber.tag(TAG).d("on playAudioFileWithListener: create new audio track: $fileName")
                                manageCacheSize()
                            }

                            // 更新最后使用时间
                            audioTrackLastUsedTime[fileName] = System.currentTimeMillis()

                            val frames = audioTrackFrameCount[fileName] ?: 0
                            var invoked = false

                            // 播放音频
                            synchronized(audioTrack) {
                                if (audioTrack.playState == AudioTrack.PLAYSTATE_PLAYING) {
                                    audioTrack.stop()
                                }

                                // 设置 marker（单位为帧）
                                if (frames > 0) {
                                    audioTrack.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
                                        override fun onMarkerReached(track: AudioTrack?) {
                                            if (!invoked) {
                                                invoked = true
                                                try { onCompleted?.invoke() } catch (_: Exception) {}
                                            }
                                            // 清除 marker，避免复用时重复触发
                                            try { audioTrack.setNotificationMarkerPosition(0) } catch (_: Exception) {}
                                        }
                                        override fun onPeriodicNotification(track: AudioTrack?) {}
                                    })
                                    try { audioTrack.setNotificationMarkerPosition(frames) } catch (_: Exception) {}
                                }

                                audioTrack.playbackHeadPosition = 0
                                // 设置音量（0.0f - 1.0f），为空时保持默认
                                volume?.let { v ->
                                    try {
                                        audioTrack.setVolume(v.coerceIn(0f, 1f))
                                    } catch (_: Exception) {}
                                }
                                Timber.tag(TAG).d("on playAudioFileWithListener: start play audio file: $fileName")
                                try { onPlay?.invoke() } catch (_: Exception) {}
                                audioTrack.play()
                            }
                        } catch (e: Exception) {
                            Timber.tag(TAG).w("on playAudioFileWithListener: find exception=${e.message}")
                        }
                        return@launch
                    }
                }

                // 获取预加载的 AudioTrack 并播放
                val audioTrack = audioTrackMap[fileName] ?: dynamicAudioTrackMap[fileName] ?: return@launch
                val frames = audioTrackFrameCount[fileName] ?: 0
                var invoked = false

                synchronized(audioTrack) {
                    if (audioTrack.playState == AudioTrack.PLAYSTATE_PLAYING) {
                        audioTrack.stop()
                    }

                    // 设置 marker（单位为帧）
                    if (frames > 0) {
                        audioTrack.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
                            override fun onMarkerReached(track: AudioTrack?) {
                                if (!invoked) {
                                    invoked = true
                                    try { onCompleted?.invoke() } catch (_: Exception) {}
                                }
                                // 清除 marker，避免复用时重复触发
                                try { audioTrack.setNotificationMarkerPosition(0) } catch (_: Exception) {}
                            }
                            override fun onPeriodicNotification(track: AudioTrack?) {}
                        })
                        try { audioTrack.setNotificationMarkerPosition(frames) } catch (_: Exception) {}
                    }

                    audioTrack.playbackHeadPosition = 0
                    // 设置音量（0.0f - 1.0f），为空时保持默认
                    volume?.let { v ->
                        try {
                            audioTrack.setVolume(v.coerceIn(0f, 1f))
                        } catch (_: Exception) {}
                    }
                    Timber.tag(TAG).d("on playAudioFileWithListener: start play audio file: $baseName, frames=$frames")
                    try { onPlay?.invoke() } catch (_: Exception) {}
                    audioTrack.play()
                }
            } catch (e: Exception) {
                Timber.tag(TAG).w("on playAudioFileWithListener: find exception=${e.message}")
            }
        }
    }

    /**
     * 播放音频并等待播放完成（suspend 函数，可在协程中使用）
     * @param baseName 音频文件名（不含扩展名）
     * @param volume 音量（0.0f - 1.0f），为空时使用默认音量
     */
    suspend fun playAudioAndWait(baseName: String, volume: Float? = null) {
        val audioCompleted = CompletableDeferred<Unit>()
        playAudioFileWithListener(
            baseName,
            volume = volume,
            onCompleted = {
                audioCompleted.complete(Unit)
            }
        )
        audioCompleted.await()
    }

    // 播放音频文件
    private fun playAudioFile(baseName: String, @Suppress("unused") interruptAble: Boolean = true) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Timber.tag(TAG).d("on playAudioFile: baseName=$baseName")

                // 处理打断事件
                if (baseName.isEmpty()) {
                    stopAudio()
                    return@launch
                }

                // 尝试重新加载音频文件
                if(audioTrackMap.keys.isEmpty() && audioDataCache.isEmpty() && loadAgainCount > 0)
                {
                    Timber.tag(TAG).d("on playAudioFile: audio file list is null , try load again")
                    --loadAgainCount
                    preloadAudioFiles()
                }

                // 在 audioTrackMap 中查找对应的音频文件
                var fileName = audioTrackMap.keys.find { it == "${baseName}.wav" }
                if (fileName == null) {
                    fileName = audioDataCache.keys.find { it == "${baseName}.wav" }
                    if (fileName == null) {
                        Timber.tag(TAG).w("on playAudioFile: audio file not found: $baseName")
                        return@launch
                    } else {
                        playAudioFileWithDynamicAudioTrack(fileName)
                        return@launch
                    }
                }

                // 获取预加载的 AudioTrack 并播放（仅播放预加载的）
                val audioTrack = audioTrackMap[fileName]  ?: return@launch
                synchronized(audioTrack) {
                    if (audioTrack.playState == AudioTrack.PLAYSTATE_PLAYING) {
                        Timber.tag(TAG).d("on playAudioFile: do stop")
                        audioTrack.stop()
                    }

                    if (audioTrack.playState == AudioTrack.PLAYSTATE_PLAYING) {
                        Timber.tag(TAG).w("on playAudioFile: audio file is already playing. baseName: $baseName")
                        return@launch
                    }

                    audioTrack.playbackHeadPosition = 0
                    Timber.tag(TAG).d("on playAudioFile: start play audio file: $baseName")
                    audioTrack.play()
                }
            } catch (e: Exception) {
                Timber.tag(TAG).w("on playAudioFile: error: ${e.message}")
            }
        }
    }
}
