package com.hwzy.app

import android.app.Application
import com.hwzy.app.utils.SentryTree
import io.sentry.Sentry
import io.sentry.android.core.SentryAndroid
import timber.log.Timber

// 全局上下文容器: 早于第一个 Activity 初始化（在应用进程创建时启动），晚于最后一个 Activity 销毁
// 1. 获取全局上下文（getApplicationContext()）
// 2. 存储跨组件的全局数据。
// 3. 初始化需要全局生效的库（如: Timber）
class MyApp: Application() {
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
    }
}
