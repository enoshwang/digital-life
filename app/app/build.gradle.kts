plugins {
    // Android 应用插件：用于构建 Android 应用
    alias(libs.plugins.android.application)
    // Kotlin Android 插件：支持 Kotlin 语言
    alias(libs.plugins.kotlin.android)
    // Kotlin Compose 插件：支持 Jetpack Compose UI 框架
    alias(libs.plugins.kotlin.compose)
    // Kotlin Parcelize 插件：支持 Parcelable 序列化
    id("kotlin-parcelize")

    id("io.sentry.android.gradle") version "5.5.0"
}

android {
    // 项目命名空间
    namespace = "com.hwzy.app"

    // 编译SDK版本
    compileSdk = 35

    defaultConfig {
        // 应用ID
        applicationId = "com.hwzy.app"
        // 最低支持SDK版本
        minSdk = 33
        // 目标SDK版本
        targetSdk = 35
        // 版本号
        versionCode = 1
        // 版本名称
        versionName = "1.0.0"

        // 测试运行器
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // NDK 配置
        ndk {
            //noinspection ChromeOsAbiSupport
            abiFilters += listOf("arm64-v8a")
        }

        android {
            externalNativeBuild {
                ndkBuild {
                    path = file("src/main/cpp/jni/Android.mk")
                }

                //  或者使用  cmake {
                //         path = file("src/main/cpp/jni/Android.mk")
                //     }
            }
        }
    }

    buildTypes {
        release {
            // 是否启用代码混淆
            isMinifyEnabled = true
            // 是否启用资源压缩
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        // Java源版本
        sourceCompatibility = JavaVersion.VERSION_17
        // Java目标版本
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        // Kotlin JVM目标版本
        jvmTarget = "17"
    }
    buildFeatures {
        // 启用 Compose 功能
        compose = true
        buildConfig = true

        // 启用 ViewBinding
        viewBinding = true
    }
}

dependencies {

    // 核心库
    // AndroidX 核心 Kotlin 扩展
    implementation(libs.androidx.core.ktx)
    // Lifecycle库
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Compose相关
    // Activity 与 Compose集成
    implementation(libs.androidx.activity.compose)
    // Compose BOM
    implementation(platform(libs.androidx.compose.bom))
    // Compose UI基础库
    implementation(libs.androidx.ui)
    // Compose图形库
    implementation(libs.androidx.ui.graphics)
    // Material Design 3组件
    implementation(libs.androidx.material3)
    // navigation-compose
    implementation(libs.androidx.navigation.compose)
    // Compose Material Design extended icons. This module contains all Material icons. It is a very large dependency and should not be included directly.
    implementation(libs.androidx.material.icons.extended)

    // fragment-ktx
    implementation(libs.androidx.fragment.ktx)

    // 日志
    implementation(libs.timber)

    // mmkv
    implementation(libs.mmkv)

    // coil-compose
    implementation(libs.coil.compose)
    // foundation
    implementation(libs.androidx.foundation)
    // Pager
    implementation(libs.androidx.foundation.layout)

    // HTTP client
    implementation(libs.retrofit)

    // camerax
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.video)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.extensions)
}


sentry {
    org.set("hwzy")
    projectName.set("android")

    // this will upload your source code to Sentry to show it as part of the stack traces
    // disable if you don't want to expose your sources
    includeSourceContext.set(true)
}
