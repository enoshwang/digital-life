cmake_minimum_required(VERSION 3.24)
message("start cmake")

project(wd_reactor_server VERSION 0.0.1 LANGUAGES CXX)

set(CMAKE_RUNTIME_OUTPUT_DIRECTORY ${PROJECT_SOURCE_DIR}/bin)

#set(CMAKE_CXX_COMPILER "clang++")

# Portably get access to memset_s
ADD_DEFINITIONS(-D__STDC_WANT_LIB_EXT1__=1)

# include
include_directories(
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${CMAKE_SOURCE_DIR}/../../3rdparty/include
    ${CMAKE_SOURCE_DIR}/../../common/wdcommon/include
)

# 构建可执行文件
aux_source_directory(${CMAKE_CURRENT_SOURCE_DIR} ROOT_SOURCE)
add_executable(${PROJECT_NAME} ${ROOT_SOURCE})
#set_target_properties(${PROJECT_NAME} PROPERTIES DEBUG_POSTFIX "_d")

message("end cmake")



