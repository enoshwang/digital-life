#
# Build library bilaries for all supported architectures
#

# 支持的架构
APP_ABI := arm64-v8a #all armeabi-v7a（32 位 ARM） armeabi

# 优化级别
APP_OPTIM := release

# 使用静态 C++ 标准库
APP_STL := c++_static

# 启用异常支持
APP_CPPFLAGS := -fexceptions # -D SOUNDTOUCH_DISABLE_X86_OPTIMIZATIONS
