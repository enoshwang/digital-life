package com.hwzy.app

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.hwzy.app.utils.SentryTree
import com.tencent.mmkv.MMKV
import io.sentry.android.core.SentryAndroid
import timber.log.Timber


// 全局上下文容器: 早于第一个 Activity 初始化（在应用进程创建时启动），晚于最后一个 Activity 销毁
// 1. 获取全局上下文（getApplicationContext()）
// 2. 存储跨组件的全局数据。
// 3. 初始化需要全局生效的库（如: Timber）
class MyApp: Application() {
    private var activityCount = 0

    override fun onCreate() {
        super.onCreate()

        // 初始化 Sentry
        SentryAndroid.init(this)

        // 初始化 Timber
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(SentryTree())
        }

        Timber.d("onCreate")

        // 初始化 MMKV
        val rootDir = MMKV.initialize(this)
        Timber.d("mmkv root: $rootDir")

        // 监听应用中所有 Activity 的生命周期事件
        // 统计应用前后台状态（如 Activity 启动 / 停止计数）。
        // 在 Activity 创建时注入依赖或设置全局行为。
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(
                activity: Activity,
                savedInstanceState: Bundle?
            ) {
                Timber.d("onActivityCreated")
            }

            override fun onActivityStarted(activity: Activity) {
                activityCount++
                Timber.d("onActivityStarted. activityCount:$activityCount")
            }

            override fun onActivityResumed(activity: Activity) {
                Timber.d("onActivityResumed")
            }

            override fun onActivityPaused(activity: Activity) {
                Timber.d("onActivityPaused")
            }

            override fun onActivityStopped(activity: Activity) {
                activityCount--
                Timber.d("onActivityStopped. activityCount:$activityCount")
            }

            override fun onActivitySaveInstanceState(
                activity: Activity,
                outState: Bundle
            ) {
                Timber.d("onActivitySaveInstanceState")
            }

            override fun onActivityDestroyed(activity: Activity) {
                Timber.d("onActivityDestroyed")
            }
        })

        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            Timber.e(e,"Uncaught exception")
        }
    }

    // 当系统内存不足时调用，通知应用释放非必要资源。
    // 根据 level 参数释放不同级别的资源（如缓存、图片等）。
    // 在应用退到后台时保留关键资源，避免被系统杀死。
    override fun onTrimMemory(level: Int) {
        Timber.d("onTrimMemory")
        super.onTrimMemory(level)
        when (level) {
            TRIM_MEMORY_UI_HIDDEN -> {
               null
            }
            else -> {
                null
            }
        }
    }
}
