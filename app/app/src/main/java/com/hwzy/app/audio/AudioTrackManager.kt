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

    /**
     * 获取 WAV 文件头大小 - 并打印 WAV 文件头信息
     */
    private fun getWavHeaderSize(wavFileData: ByteArray): Int {
        if (wavFileData.size < 12) {
            Timber.tag(TAG).e("WAV 文件数据太小，无法解析")
            return 0
        }
        
        var offset = 0
        
        // 解析 RIFF chunk
        val riffId = String(wavFileData, offset, 4)
        offset += 4
        if (riffId != "RIFF") {
            Timber.tag(TAG).e("不是有效的 WAV 文件，RIFF ID: $riffId")
            return 0
        }
        
        val chunkSize = bytesToInt(wavFileData, offset, 4, littleEndian = true)
        offset += 4
        
        val format = String(wavFileData, offset, 4)
        offset += 4
        if (format != "WAVE") {
            Timber.tag(TAG).e("不是有效的 WAVE 格式，Format: $format")
            return 0
        }
        
        Timber.tag(TAG).d("=== WAV 文件头信息 ===")
        Timber.tag(TAG).d("RIFF ID: $riffId")
        Timber.tag(TAG).d("Chunk Size: $chunkSize")
        Timber.tag(TAG).d("Format: $format")
        
        // 解析各个 chunk
        var numChannels = 0
        var sampleRate = 0
        var bitsPerSample = 0
        var byteRate = 0
        var blockAlign = 0
        var audioFormat = 0
        var dataSize = 0
        var dataChunkOffset = 0
        var customInfo = ""
        
        while (offset < wavFileData.size - 8) {
            val chunkId = String(wavFileData, offset, 4)
            offset += 4
            
            val chunkSize = bytesToInt(wavFileData, offset, 4, littleEndian = true)
            offset += 4
            
            when (chunkId) {
                "fmt " -> {
                    // 解析 fmt chunk
                    audioFormat = bytesToInt(wavFileData, offset, 2, littleEndian = true)
                    offset += 2
                    
                    numChannels = bytesToInt(wavFileData, offset, 2, littleEndian = true)
                    offset += 2
                    
                    sampleRate = bytesToInt(wavFileData, offset, 4, littleEndian = true)
                    offset += 4
                    
                    byteRate = bytesToInt(wavFileData, offset, 4, littleEndian = true)
                    offset += 4
                    
                    blockAlign = bytesToInt(wavFileData, offset, 2, littleEndian = true)
                    offset += 2
                    
                    bitsPerSample = bytesToInt(wavFileData, offset, 2, littleEndian = true)
                    offset += 2
                    
                    // 如果有额外数据，跳过
                    val extraDataSize = chunkSize - 16
                    if (extraDataSize > 0) {
                        offset += extraDataSize
                    }
                    
                    Timber.tag(TAG).d("--- fmt chunk ---")
                    Timber.tag(TAG).d("Audio Format: $audioFormat (1=PCM)")
                    Timber.tag(TAG).d("声道数 (Num Channels): $numChannels")
                    Timber.tag(TAG).d("采样率 (Sample Rate): $sampleRate Hz")
                    Timber.tag(TAG).d("字节率 (Byte Rate): $byteRate bytes/sec")
                    Timber.tag(TAG).d("块对齐 (Block Align): $blockAlign bytes")
                    Timber.tag(TAG).d("位深 (Bits Per Sample): $bitsPerSample bits")
                }
                "data" -> {
                    // 找到 data chunk
                    dataSize = chunkSize
                    dataChunkOffset = offset - 8 // 包含 chunk ID 和 size
                    Timber.tag(TAG).d("--- data chunk ---")
                    Timber.tag(TAG).d("数据大小 (Data Size): $dataSize bytes")
                    offset += chunkSize // 跳过数据部分
                    break // 找到 data chunk 后停止
                }
                "LIST" -> {
                    // LIST chunk 可能包含自定义信息
                    if (chunkSize > 0 && offset + chunkSize <= wavFileData.size) {
                        val listData = String(wavFileData, offset, minOf(chunkSize, 256))
                        customInfo = listData.trim()
                        Timber.tag(TAG).d("--- LIST chunk (自定义信息) ---")
                        Timber.tag(TAG).d("内容: $customInfo")
                    }
                    offset += chunkSize
                }
                else -> {
                    // 其他未知 chunk，跳过
                    Timber.tag(TAG).d("未知 chunk: $chunkId, 大小: $chunkSize")
                    offset += chunkSize
                }
            }
        }
        
        // 计算 header 总大小
        val headerSize = if (dataChunkOffset > 0) {
            dataChunkOffset + 8 // data chunk ID (4) + size (4)
        } else {
            offset // 如果没有找到 data chunk，使用当前偏移量
        }
        
        Timber.tag(TAG).d("--- 汇总信息 ---")
        Timber.tag(TAG).d("声道数: $numChannels")
        Timber.tag(TAG).d("位深: $bitsPerSample bits")
        Timber.tag(TAG).d("采样率: $sampleRate Hz")
        if (customInfo.isNotEmpty()) {
            Timber.tag(TAG).d("自定义信息: $customInfo")
        }
        Timber.tag(TAG).d("数据量大小: $dataSize bytes")
        Timber.tag(TAG).d("Header 总大小: $headerSize bytes")
        Timber.tag(TAG).d("==================")
        
        return headerSize
    }
    
    /**
     * 将字节数组转换为整数（支持大端和小端）
     */
    private fun bytesToInt(bytes: ByteArray, offset: Int, length: Int, littleEndian: Boolean = true): Int {
        if (offset + length > bytes.size) {
            return 0
        }
        
        var result = 0
        if (littleEndian) {
            // 小端序（WAV 文件使用）
            for (i in 0 until length) {
                result = result or ((bytes[offset + i].toInt() and 0xFF) shl (i * 8))
            }
        } else {
            // 大端序
            for (i in 0 until length) {
                result = (result shl 8) or (bytes[offset + i].toInt() and 0xFF)
            }
        }
        return result
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

                        getWavHeaderSize(audioData)

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
