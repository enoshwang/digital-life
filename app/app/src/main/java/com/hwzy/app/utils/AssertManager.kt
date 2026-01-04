package com.hwzy.app.utils

import android.content.Context
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

object AssertManager {
    const val TAG = "AssertManager"

    /**
     * 将 assets 下的整个目录复制到内部存储
     * @param context Context
     * @param assetPath assets 中的路径，如 "voice/res"
     * @param targetDir 目标目录，如 context.cacheDir 或 filesDir 下的子目录
     */
    @Throws(IOException::class)
    private fun copyAssetsDirToInternalStorage(context: Context, assetPath: String, targetDir: File) {
        val assetManager = context.assets
        val fileNames = assetManager.list(assetPath) ?: return
        Timber.tag(TAG).d("开始复制资源文件从 assets/$assetPath 到 ${targetDir.absolutePath}")

        // 创建目标目录
        targetDir.mkdirs()

        for (fileName in fileNames) {
            val absoluteAssetPath = "$assetPath/$fileName"
            val assetFile = assetManager.open(absoluteAssetPath)

            val outFile = File(targetDir, fileName)

            Timber.tag(TAG).d("复制文件: $absoluteAssetPath -> ${outFile.absolutePath}")
            if (assetFile is FileInputStream) {
                FileOutputStream(outFile).use { out ->
                    assetFile.channel.transferTo(0, assetFile.channel.size(), out.channel)
                }
            } else {
                assetFile.use { input ->
                    outFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
        }
    }

    /**
     * 获取资源目录的真实路径，如果不存在则复制
     * @param context Context
     * @param assetResPath assets 中的资源路径
     * @param targetResPath 目标资源路径
     * @return 资源目录的绝对路径，如果复制失败则返回 null
     */
    fun getOrCopyResDir(context: Context, assetResPath: String, targetResPath: String): String? {
        val targetDir = File(context.getExternalFilesDir(null), targetResPath)

        // 删除旧目录（防止残留）
        targetDir.deleteRecursively()

        try {
            copyAssetsDirToInternalStorage(context, assetResPath, targetDir)
            return targetDir.absolutePath
        } catch (e: IOException) {
            Timber.tag(TAG).e(e, "复制资源文件失败: $assetResPath")
            return null
        }
    }
}
