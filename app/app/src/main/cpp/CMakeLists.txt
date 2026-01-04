cmake_minimum_required(VERSION 4.2.1)
project("soundtouch")

# 添加包含目录
include_directories(
        ${CMAKE_CURRENT_SOURCE_DIR}/soundtouch/include
        ${CMAKE_CURRENT_SOURCE_DIR}/soundstretch
)

# 收集源文件
file(GLOB_RECURSE SOUNDTOUCH_SOURCES
        "${CMAKE_CURRENT_SOURCE_DIR}/soundtouch/*.cpp"
        "${CMAKE_CURRENT_SOURCE_DIR}/soundtouch/*.c"
)

file(GLOB_RECURSE SOUNDSTRETCH_SOURCES
        "${CMAKE_CURRENT_SOURCE_DIR}/soundstretch/*.cpp"
)

# 添加 JNI 源文件
set(JNI_SOURCES
        ${CMAKE_CURRENT_SOURCE_DIR}/jni/soundtouch-jni.cpp
)

# 创建共享库
add_library(soundtouch SHARED
        ${JNI_SOURCES}
        ${SOUNDTOUCH_SOURCES}
        ${SOUNDSTRETCH_SOURCES}
)

# 链接库
target_link_libraries(soundtouch
        android
        log
)

# 编译选项
target_compile_options(soundtouch PRIVATE
        -fvisibility=hidden
        -fdata-sections
        -ffunction-sections
        -ffast-math
        -O3
)
