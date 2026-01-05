package net.surina.soundtouch

/**
 * SoundTouch 音频处理库的 Kotlin 包装类
 *
 * 提供音频节拍、音调和播放速率的调整功能
 */
class SoundTouch private constructor(private val handle: Long) {

    companion object {
        init {
            // 加载原生库
            System.loadLibrary("soundtouch")
        }

        /**
         * 获取 SoundTouch 库版本字符串
         */
        @JvmStatic
        external fun getVersionString(): String

        /**
         * 获取错误信息字符串
         */
        @JvmStatic
        external fun getErrorString(): String

        /**
         * 创建新的 SoundTouch 实例
         */
        @JvmStatic
        private external fun newInstance(): Long

        /**
         * 创建 SoundTouch 实例
         */
        fun create(): SoundTouch {
            val handle = newInstance()
            return SoundTouch(handle)
        }
    }

    /**
     * 设置节拍变化（相对于原始节拍）
     * @param tempo 节拍值，1.0 = 原始节拍，2.0 = 两倍速度，0.5 = 一半速度
     */
    fun setTempo(tempo: Float) {
        setTempo(handle, tempo)
    }

    /**
     * 设置音调变化（半音）
     * @param pitch 音调变化，0.0 = 原始音调，+12.0 = 高一个八度，-12.0 = 低一个八度
     */
    fun setPitchSemiTones(pitch: Float) {
        setPitchSemiTones(handle, pitch)
    }

    /**
     * 设置播放速率（同时影响节拍和音调）
     * @param speed 速率值，1.0 = 原始速率，2.0 = 两倍速率，0.5 = 一半速率
     */
    fun setSpeed(speed: Float) {
        setSpeed(handle, speed)
    }

    /**
     * 处理音频文件
     * @param inputFile 输入文件路径
     * @param outputFile 输出文件路径
     * @return 0 表示成功，-1 表示失败（可通过 getErrorString() 获取错误信息）
     */
    fun processFile(inputFile: String, outputFile: String): Int {
        return processFile(handle, inputFile, outputFile)
    }

    /**
     * 设置采样率
     * @param sampleRate 采样率（Hz）
     */
    fun setSampleRate(sampleRate: Int) {
        setSampleRate(handle, sampleRate)
    }

    /**
     * 设置声道数
     * @param channels 声道数，1 = 单声道，2 = 立体声
     */
    fun setChannels(channels: Int) {
        setChannels(handle, channels)
    }

    /**
     * 输入音频样本进行处理
     * @param samples 音频样本数组（Float 数组）
     * @param numSamples 样本数量
     */
    fun putSamples(samples: FloatArray, numSamples: Int) {
        putSamples(handle, samples, numSamples)
    }

    /**
     * 接收处理后的音频样本
     * @param output 输出数组
     * @param maxSamples 最大样本数
     * @return 实际接收到的样本数
     */
    fun receiveSamples(output: FloatArray, maxSamples: Int): Int {
        return receiveSamples(handle, output, maxSamples)
    }

    /**
     * 刷新处理管道，确保所有输入数据都被处理
     */
    fun flush() {
        flush(handle)
    }

    /**
     * 清空所有缓冲数据
     */
    fun clear() {
        clear(handle)
    }

    /**
     * 获取当前可用的样本数
     * @return 可用样本数
     */
    fun numSamples(): Int {
        return numSamples(handle)
    }

    /**
     * 检查处理管道是否为空
     * @return true 如果为空
     */
    fun isEmpty(): Boolean {
        return isEmpty(handle)
    }

    /**
     * 释放资源
     */
    fun close() {
        deleteInstance(handle)
    }

    // 原生方法声明
    private external fun setTempo(handle: Long, tempo: Float)
    private external fun setPitchSemiTones(handle: Long, pitch: Float)
    private external fun setSpeed(handle: Long, speed: Float)
    private external fun processFile(handle: Long, inputFile: String, outputFile: String): Int
    private external fun setSampleRate(handle: Long, sampleRate: Int)
    private external fun setChannels(handle: Long, channels: Int)
    private external fun putSamples(handle: Long, samples: FloatArray, numSamples: Int)
    private external fun receiveSamples(handle: Long, output: FloatArray, maxSamples: Int): Int
    private external fun flush(handle: Long)
    private external fun clear(handle: Long)
    private external fun numSamples(handle: Long): Int
    private external fun isEmpty(handle: Long): Boolean
    private external fun deleteInstance(handle: Long)

    /**
     * 使用 try-with-resources 模式
     */
    fun use(block: (SoundTouch) -> Unit) {
        try {
            block(this)
        } finally {
            close()
        }
    }
}
