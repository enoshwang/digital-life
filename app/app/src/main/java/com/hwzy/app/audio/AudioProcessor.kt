package com.hwzy.app.audio

import net.surina.soundtouch.SoundTouch
import android.os.AsyncTask
import kotlinx.coroutines.Dispatchers
import timber.log.Timber

class AudioProcessor {

    companion object {
        private const val TAG = "AudioProcessor"
    }

    /**
     * 处理音频文件：加快节拍 15%，降低音调 3 个半音
     */
    fun processAudio(inputPath: String, outputPath: String) {
        // 在后台线程中处理
        AsyncTask.execute {
            try {
                SoundTouch.create().use { soundTouch ->
                    // 设置节拍：1.15 = 加快 15%
                    soundTouch.setTempo(1.15f)

                    // 设置音调：-3.0 = 降低 3 个半音
                    soundTouch.setPitchSemiTones(-3.0f)

                    // 处理文件
                    val result = soundTouch.processFile(inputPath, outputPath)

                    if (result == 0) {
                        // 处理成功
                        Timber.tag(TAG).d("AudioProcessor, 处理成功: $outputPath")
                    } else {
                        // 处理失败
                        val error = SoundTouch.getErrorString()
                        Timber.tag(TAG).e("AudioProcessor, 处理失败: $error")
                    }
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e("AudioProcessor, 处理异常, e=$e")
            }
        }
    }

    /**
     * 使用协程处理（Kotlin Coroutines）
     */

    /*
    suspend fun processAudioCoroutine(
        inputPath: String,
        outputPath: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            SoundTouch.create().use { soundTouch ->
                soundTouch.setTempo(1.2f)  // 加快 20%
                soundTouch.setPitchSemiTones(2.0f)  // 提高 2 个半音

                val result = soundTouch.processFile(inputPath, outputPath)

                if (result == 0) {
                    Result.success(outputPath)
                } else {
                    Result.failure(Exception(SoundTouch.getErrorString()))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    */
}
