cmake_minimum_required(VERSION 3.28)

project(ew_reactor_server VERSION 1.0.0 LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 23)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

add_compile_options(-Werror)

# Portably get access to memset_s
ADD_DEFINITIONS(-D__STDC_WANT_LIB_EXT1__=1)

# include
include_directories(
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${CMAKE_CURRENT_SOURCE_DIR}/3rdparty/include
    ${CMAKE_CURRENT_SOURCE_DIR}/../my_utils/build/dist/include/
)

# 构建可执行文件
aux_source_directory(${CMAKE_CURRENT_SOURCE_DIR} ROOT_SOURCE)

add_executable(${PROJECT_NAME} ${ROOT_SOURCE})
target_link_libraries(${PROJECT_NAME} PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}/../my_utils/build/dist/lib/libmy_utils.a)



