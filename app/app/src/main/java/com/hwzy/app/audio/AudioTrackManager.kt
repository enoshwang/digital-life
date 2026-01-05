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
import java.nio.ByteBuffer
import java.nio.ByteOrder
import net.surina.soundtouch.SoundTouch

object AudioTrackManager {
    private const val TAG = "AudioTrackManager"

    // wav 文件头
    private const val PCM_HEADER_SIZE = 78

    /**
     * WAV 文件信息数据类
     */
    data class WavFileInfo(
        val headerSize: Int,              // 文件头大小（字节）
        val numChannels: Int,             // 声道数（1=单声道, 2=立体声）
        val sampleRate: Int,              // 采样率（Hz）
        val bitsPerSample: Int,           // 位深（bits）
        val byteRate: Int,                // 字节率（bytes/sec）
        val blockAlign: Int,              // 块对齐（bytes）
        val audioFormat: Int,             // 音频格式（1=PCM）
        val dataSize: Int,                // PCM 数据大小（字节）
        val dataChunkOffset: Int,         // data chunk 偏移位置
        val customInfo: String = ""       // 自定义信息（如果有）
    )

    // 初始化相关
    @Volatile
    private var isInitialized = false
    private lateinit var appContext: Context

    // 对应文件的帧数（用于 marker 回调，单位：frame）
    private val audioTrackFrameCount = mutableMapOf<String, Int>()

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
     * 解析 WAV 文件头信息
     * @param wavFileData WAV 文件数据
     * @return WavFileInfo 对象，如果解析失败返回 null
     */
    private fun parseWavFileInfo(wavFileData: ByteArray): WavFileInfo? {
        if (wavFileData.size < 12) {
            Timber.tag(TAG).e("WAV 文件数据太小，无法解析")
            return null
        }
        
        var offset = 0
        
        // 解析 RIFF chunk
        val riffId = String(wavFileData, offset, 4)
        offset += 4
        if (riffId != "RIFF") {
            Timber.tag(TAG).e("不是有效的 WAV 文件，RIFF ID: $riffId")
            return null
        }
        
        val chunkSize = bytesToInt(wavFileData, offset, 4, littleEndian = true)
        offset += 4
        
        val format = String(wavFileData, offset, 4)
        offset += 4
        if (format != "WAVE") {
            Timber.tag(TAG).e("不是有效的 WAVE 格式，Format: $format")
            return null
        }

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
                }
                "data" -> {
                    // 找到 data chunk
                    dataSize = chunkSize
                    dataChunkOffset = offset - 8 // 包含 chunk ID 和 size
                    offset += chunkSize // 跳过数据部分
                    break // 找到 data chunk 后停止
                }
                "LIST" -> {
                    // LIST chunk 可能包含自定义信息
                    if (chunkSize > 0 && offset + chunkSize <= wavFileData.size) {
                        val listData = String(wavFileData, offset, minOf(chunkSize, 256))
                        customInfo = listData.trim()
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
        
        Timber.tag(TAG).d("--- 音频信息 ---")
        Timber.tag(TAG).d("声道数: $numChannels")
        Timber.tag(TAG).d("位深: $bitsPerSample bits")
        Timber.tag(TAG).d("采样率: $sampleRate Hz")
        if (customInfo.isNotEmpty()) {
            Timber.tag(TAG).d("自定义信息: $customInfo")
        }
        Timber.tag(TAG).d("数据量大小: $dataSize bytes")
        Timber.tag(TAG).d("Header 总大小: $headerSize bytes")
        Timber.tag(TAG).d("==================")
        
        return WavFileInfo(
            headerSize = headerSize,
            numChannels = numChannels,
            sampleRate = sampleRate,
            bitsPerSample = bitsPerSample,
            byteRate = byteRate,
            blockAlign = blockAlign,
            audioFormat = audioFormat,
            dataSize = dataSize,
            dataChunkOffset = dataChunkOffset,
            customInfo = customInfo
        )
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
    fun playAudioFileWithListener(baseName: String, volume: Float? = null, speed: Float = 1.0f, onPlay: (() -> Unit)? = null, onCompleted: (() -> Unit)? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Timber.tag(TAG).d("on playAudioFileWithListener: baseName=$baseName, speed=$speed, volume=$volume, onPlay=$onPlay, onCompleted=$onCompleted")
                if (baseName.isEmpty()) {
                    return@launch
                }

                // 在 audioTrackMap 中查找对应的音频文件
                var fileName = audioDataCache.keys.find { it == "${baseName}.wav" }
                if (fileName == null) {
                    Timber.tag(TAG).w("on playAudioFileWithListener: audio file not found, baseName=$baseName")
                    return@launch
                } else {
                    // 使用文件名和速度作为缓存键，因为不同速度需要不同的 AudioTrack
                    val cacheKey = if (speed != 1.0f) "${fileName}_speed_$speed" else fileName

                    // 先检查 AudioTrack 是否已缓存
                    var audioTrack = dynamicAudioTrackMap[cacheKey]
                    if (audioTrack == null) {
                        // 从缓存中获取音频数据
                        val audioData = audioDataCache[fileName] ?: run {
                            Timber.tag(TAG).w("on playAudioFileWithListener: audio data not found in cache: $fileName")
                            return@launch
                        }

                        val wavInfo = parseWavFileInfo(audioData)
                        val headerSize = wavInfo?.headerSize ?: PCM_HEADER_SIZE

                        // 如果缓存不存在，才进行音频处理
                        val processedAudioData = if (speed != 1.0f) {
                            Timber.tag(TAG).d("on playAudioFileWithListener: processing audio with speed=$speed")
                            processAudioWithSpeed(audioData, speed, wavInfo)
                        } else {
                            audioData
                        }

                        // 创建新的 AudioTrack
                        // 对于处理后的音频，需要重新解析 headerSize（因为 processAudioWithSpeed 可能改变了文件头）
                        val processedWavInfo = if (speed != 1.0f) parseWavFileInfo(processedAudioData) else wavInfo
                        val processedHeaderSize = processedWavInfo?.headerSize ?: PCM_HEADER_SIZE
                        val pcmDataSize = processedAudioData.size - processedHeaderSize
                        audioTrack = createAudioTrack(pcmDataSize)
                        audioTrack.write(processedAudioData, processedHeaderSize, pcmDataSize)
                        dynamicAudioTrackMap[cacheKey] = audioTrack
                        audioTrackFrameCount[cacheKey] = (pcmDataSize / 2)
                        Timber.tag(TAG).d("on playAudioFileWithListener: create new audio track: $cacheKey")
                        manageCacheSize()
                    }

                    // 更新最后使用时间
                    audioTrackLastUsedTime[cacheKey] = System.currentTimeMillis()

                    val frames = audioTrackFrameCount[cacheKey] ?: 0
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
                }
            } catch (e: Exception) {
                Timber.tag(TAG).w("on playAudioFileWithListener: find exception=${e.message}")
            }
        }
    }

    /**
     * 使用 SoundTouch 实时处理音频数据的速度
     * @param wavData 原始 WAV 文件数据（包含文件头）
     * @param speed 播放速度，1.0 = 原始速度
     * @param wavInfo WAV 文件信息，如果为 null 则使用默认的 PCM_HEADER_SIZE
     * @return 处理后的 WAV 文件数据（包含文件头）
     */
    private fun processAudioWithSpeed(wavData: ByteArray, speed: Float, wavInfo: WavFileInfo? = null): ByteArray {
        val headerSize = wavInfo?.headerSize ?: PCM_HEADER_SIZE
        if (wavData.size <= headerSize) {
            Timber.tag(TAG).w("on processAudioWithSpeed: audio data too small")
            return wavData
        }

        // 提取 PCM 数据（跳过文件头）
        val pcmData = ByteArray(wavData.size - headerSize)
        System.arraycopy(wavData, headerSize, pcmData, 0, pcmData.size)

        // 将 16-bit PCM 数据转换为 Float 数组
        val samples = ShortArray(pcmData.size / 2)
        ByteBuffer.wrap(pcmData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(samples)
        val floatSamples = FloatArray(samples.size) { samples[it] / 32768.0f }

        // 使用 SoundTouch 处理
        val soundTouch = SoundTouch.create()
        try {
            // 配置 SoundTouch（根据 WAV 文件格式，这里使用固定值，实际可以从文件头解析）
            soundTouch.setSampleRate(16000)
            soundTouch.setChannels(1) // 单声道
            soundTouch.setTempo(speed)

            // 处理音频数据
            val chunkSize = 4096 // 每次处理的样本数
            val processedSamples = mutableListOf<Float>()
            
            // 分块输入数据
            var offset = 0
            while (offset < floatSamples.size) {
                val chunk = floatSamples.copyOfRange(offset, minOf(offset + chunkSize, floatSamples.size))
                soundTouch.putSamples(chunk, chunk.size)
                offset += chunk.size

                // 接收处理后的数据
                val outputBuffer = FloatArray(chunkSize)
                var received = soundTouch.receiveSamples(outputBuffer, chunkSize)
                while (received > 0) {
                    processedSamples.addAll(outputBuffer.take(received))
                    received = soundTouch.receiveSamples(outputBuffer, chunkSize)
                }
            }

            // 刷新处理管道，获取剩余数据
            soundTouch.flush()
            val outputBuffer = FloatArray(chunkSize)
            var received = soundTouch.receiveSamples(outputBuffer, chunkSize)
            while (received > 0) {
                processedSamples.addAll(outputBuffer.take(received))
                received = soundTouch.receiveSamples(outputBuffer, chunkSize)
            }

            // 将 Float 数组转换回 16-bit PCM
            val processedShorts = processedSamples.map { (it.coerceIn(-1f, 1f) * 32767f).toInt().coerceIn(-32768, 32767).toShort() }.toShortArray()
            val processedPcmData = ByteArray(processedShorts.size * 2)
            ByteBuffer.wrap(processedPcmData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(processedShorts)

            // 重新构建 WAV 文件（更新文件头中的 data chunk 大小）
            val newDataSize = processedPcmData.size
            val newWavData = ByteArray(headerSize + newDataSize)
            
            // 复制原始文件头
            System.arraycopy(wavData, 0, newWavData, 0, headerSize)
            
            // 更新 RIFF chunk size（总文件大小 - 8）
            val riffSize = newWavData.size - 8
            newWavData[4] = (riffSize and 0xFF).toByte()
            newWavData[5] = ((riffSize shr 8) and 0xFF).toByte()
            newWavData[6] = ((riffSize shr 16) and 0xFF).toByte()
            newWavData[7] = ((riffSize shr 24) and 0xFF).toByte()
            
            // 查找并更新 data chunk size
            var headerOffset = 12 // 跳过 RIFF header
            while (headerOffset < headerSize - 8) {
                val chunkId = String(newWavData, headerOffset, 4)
                if (chunkId == "data") {
                    // 更新 data chunk size
                    newWavData[headerOffset + 4] = (newDataSize and 0xFF).toByte()
                    newWavData[headerOffset + 5] = ((newDataSize shr 8) and 0xFF).toByte()
                    newWavData[headerOffset + 6] = ((newDataSize shr 16) and 0xFF).toByte()
                    newWavData[headerOffset + 7] = ((newDataSize shr 24) and 0xFF).toByte()
                    break
                }
                val currentChunkSize = bytesToInt(newWavData, headerOffset + 4, 4, littleEndian = true)
                headerOffset += 8 + currentChunkSize
            }
            
            // 复制处理后的 PCM 数据
            System.arraycopy(processedPcmData, 0, newWavData, headerSize, newDataSize)

            Timber.tag(TAG).d("on processAudioWithSpeed: processed ${floatSamples.size} samples to ${processedSamples.size} samples")
            return newWavData
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "on processAudioWithSpeed: error processing audio")
            return wavData // 出错时返回原始数据
        } finally {
            soundTouch.close()
        }
    }
}
