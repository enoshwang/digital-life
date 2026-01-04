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
import java.io.IOException

object AudioTrackManager {
    private const val TAG = "AudioTrackManager"

    // wav 文件头
    private const val PCM_HEADER_SIZE = 78

    // 初始化相关
    @Volatile
    private var isInitialized = false
    private lateinit var appContext: Context

    // 对应文件的帧数（用于 marker 回调，单位：frame）
    private val audioTrackFrameCount = mutableMapOf<String, Int>()

    // 如果在播放音频时，发现文件都不存在，则尝试再加载一次
    private var loadAgainCount = 1

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

    /**
     * 初始化 AudioTrackManager
     */
    fun initialize(context: Context) {
        if (isInitialized) return
        isInitialized = true
        appContext = context.applicationContext
        
        CoroutineScope(Dispatchers.IO).launch {
            preloadAudioFiles()
        }
        startCleanupJob()
        Timber.tag(TAG).d("on initialize for AudioTrackManager")
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

            Timber.tag(TAG).d("on preloadAudioFiles: find ${wavFiles.size} audio files, cache size: ${audioDataCache.size}, keys=${audioDataCache.keys}")
        } catch (e: IOException) {
            Timber.tag(TAG).e(e, "Failed to list assets in audio")
        }
    }

    /**
     * 创建 AudioTrack 对象
     */
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

    // 停止播放指定音频
    fun stopAudioByBaseName(baseName: String) {
        try {
            Timber.tag(TAG).d("on stopAudioByBaseName: baseName=$baseName")

            val fileName = "$baseName.wav"
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

    // 停止播放所有音频
    private fun stopAudio() {
        Timber.tag(TAG).d("on stopAudio")

        dynamicAudioTrackMap.forEach { (_, audioTrack) ->
            audioTrack.let {
                if (it.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    it.stop()
                }
            }
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
                if(audioDataCache.isEmpty() && loadAgainCount > 0) {
                    Timber.tag(TAG).d("on playAudioFileWithListener: audio file list is null , try load again")
                    --loadAgainCount
                    preloadAudioFiles()
                }

                // 在 audioTrackMap 中查找对应的音频文件
                var fileName = audioDataCache.keys.find { it == "${baseName}.wav" }
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
}
