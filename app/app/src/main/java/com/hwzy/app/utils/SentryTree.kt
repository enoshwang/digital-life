package com.hwzy.app.utils

import android.util.Log
import io.sentry.Sentry
import io.sentry.SentryLevel
import timber.log.Timber

class SentryTree : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        // 根据 Timber 日志级别映射到 Sentry 级别
        val sentryLevel = when (priority) {
            Log.VERBOSE, Log.DEBUG -> SentryLevel.DEBUG
            Log.INFO -> SentryLevel.INFO
            Log.WARN -> SentryLevel.WARNING
            Log.ERROR -> SentryLevel.ERROR
            Log.ASSERT -> SentryLevel.FATAL
            else -> SentryLevel.INFO
        }

        // 处理异常
        if (t != null) {
            Sentry.captureException(t) { event ->
                event.level = sentryLevel
                if (tag != null) event.setTag("timber_tag", tag)
            }
        }
    }
}
