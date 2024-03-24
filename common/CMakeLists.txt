cmake_minimum_required(VERSION 3.22)
message("start cmake")

if (WIN32)
	message("it is windows")
elseif (UNIX)
	message("it is UNIX-like OS's")
else ()
	message("it is Other OS (not windows|UNIX-like)")
endif ()

project(wdcommon LANGUAGES CXX)

# cmake
include(${CMAKE_SOURCE_DIR}/../cmake/base.cmake)

# include
include_directories(${CMAKE_SOURCE_DIR}/include)

# 构建测试程序
aux_source_directory(${CMAKE_CURRENT_SOURCE_DIR}/test TEST_SRC)
add_executable(${PROJECT_NAME}_test ${TEST_SRC})

message("end cmake")