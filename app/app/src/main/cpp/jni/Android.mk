LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

# 包含路径
LOCAL_C_INCLUDES += $(LOCAL_PATH)/../soundtouch/include $(LOCAL_PATH)/../SoundStretch
LOCAL_C_INCLUDES += $(LOCAL_PATH)/../soundtouch/include $(LOCAL_PATH)/../soundtouch/include

LOCAL_MODULE    := soundtouch

# 源文件路径
LOCAL_SRC_FILES := soundtouch-jni.cpp ../soundtouch/source/AAFilter.cpp  ../soundtouch/source/FIFOSampleBuffer.cpp \
                ../soundtouch/source/FIRFilter.cpp ../soundtouch/source/cpu_detect_x86.cpp \
                ../soundtouch/source/sse_optimized.cpp ../soundstretch/WavFile.cpp \
                ../soundtouch/source/RateTransposer.cpp ../soundtouch/source/SoundTouch.cpp \
                ../soundtouch/source/InterpolateCubic.cpp ../soundtouch/source/InterpolateLinear.cpp \
                ../soundtouch/source/InterpolateShannon.cpp ../soundtouch/source/TDStretch.cpp \
                ../soundtouch/source/BPMDetect.cpp ../soundtouch/source/PeakFinder.cpp

# for native audio
#LOCAL_SHARED_LIBRARIES += -lgcc
# --whole-archive -lgcc

# for logging
LOCAL_LDLIBS    += -llog

# for native asset manager
#LOCAL_LDLIBS    += -landroid

# Custom Flags（编译选项）:
LOCAL_CFLAGS += -O3
# 使用整数采样（低端设备，牺牲音质换取性能）
# LOCAL_CFLAGS += -DSOUNDTOUCH_INTEGER_SAMPLES

# -fvisibility=hidden : don't export all symbols
LOCAL_CFLAGS += -fvisibility=hidden -fdata-sections -ffunction-sections -ffast-math

# OpenMP mode : enable these flags to enable using OpenMP for parallel computation
# TODO 打开后，启动卡住 - dlopen failed: 'libomp.so' not found
#LOCAL_CFLAGS += -fopenmp
#LOCAL_LDFLAGS += -fopenmp

# Use ARM instruction set instead of Thumb for improved calculation performance in ARM CPUs	
LOCAL_ARM_MODE := arm

include $(BUILD_SHARED_LIBRARY)
