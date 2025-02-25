cmake_minimum_required(VERSION 3.29)

project(digital_hub VERSION 1.0.0 LANGUAGES CXX)



# 设置 C++ 标准
set(CMAKE_CXX_STANDARD 23)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# 编译选项
if(MSVC)
    add_compile_options(/WX)
else()
    add_compile_options(-Werror)
endif()

# 自动处理UI、资源和 MOC 文件
set(CMAKE_AUTOUIC ON)
set(CMAKE_AUTORCC ON)
set(CMAKE_AUTOMOC ON)

# 查找Qt6库
find_package(Qt6 REQUIRED COMPONENTS Widgets Network MultimediaWidgets Charts Sql)

# 包含目录
include_directories(
    ${CMAKE_SOURCE_DIR}/include
    ${CMAKE_SOURCE_DIR}/3rdparty/include
    ${CMAKE_CURRENT_BINARY_DIR}  # 用于自动生成的UI头文件
)

# 源文件列表
file(GLOB_RECURSE SOURCES
    "src/*.cpp"
    "ui/*.cpp"
)

# UI文件列表
file(GLOB INCLUDE_FILES
    "include/*.h"
)

# UI文件列表
file(GLOB UI_FILES
    "ui/*.ui"
)

# 资源文件列表
file(GLOB QRC_FILES
    "qrc/*.qrc"
)

# 生成可执行文件
add_executable(${PROJECT_NAME}
    ${SOURCES}
    ${UI_FILES}
    ${QRC_FILES}
    ${INCLUDE_FILES}
)

target_link_libraries(${PROJECT_NAME} PRIVATE
        Qt6::Widgets
        Qt6::Network
        Qt6::MultimediaWidgets
        Qt6::Charts
        Qt6::Sql)

set(ICON_FILE ${CMAKE_CURRENT_SOURCE_DIR}/qrc/main.ico)
