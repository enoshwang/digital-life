cmake_minimum_required(VERSION 3.2)
message("start cmake")

if(WIN32)
	message(STATUS "It is windows")
	#SET(CMAKE_TOOLCHAIN_FILE "C:/Users/enosh/Documents/vcpkg/scripts/buildsystems/vcpkg.cmake")
elseif (UNIX)
	MESSAGE(STATUS "It is UNIX-like OS's.")
else ()
	MESSAGE(status "It is Other OS (not windows|UNIX-like)")
endif ()

project(digital_hub VERSION 0.1 LANGUAGES CXX)

message("set output dir(must set in root CMakeLists.txt)")
# 设置静态库的输出目录
#set(CMAKE_ARCHIVE_OUTPUT_DIRECTORY ${PROJECT_SOURCE_DIR}/dist/lib)
# 设置动态库的输出目录
#set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${PROJECT_SOURCE_DIR}/dist/lib)
# 设置可执行文件的输出目录(windows dll 与 exe 都为 runtime)
#set(CMAKE_RUNTIME_OUTPUT_DIRECTORY ${PROJECT_SOURCE_DIR}/dist/bin)

# include
include_directories(
    ${CMAKE_SOURCE_DIR}/3rdparty/include
)

set(CMAKE_INCLUDE_CURRENT_DIR ON)

set(CMAKE_AUTOUIC ON)  # UIC能被自动执行
set(CMAKE_AUTOMOC ON)  # MOC能被自动执行
set(CMAKE_AUTORCC ON)  # RCC能被自动执行

set(CMAKE_PREFIX_PATH $ENV{Qt6_DIR})

#查找 Qt6 或 Qt5，导入 Widgets、Network 模块
find_package(QT NAMES Qt6 Qt5 REQUIRED COMPONENTS Widgets Network MultimediaWidgets Charts Sql)
#查找 Qt6，导入 Widgets、Network 模块
find_package(Qt${QT_VERSION_MAJOR} REQUIRED COMPONENTS Widgets Network MultimediaWidgets Charts Sql)

#aux_source_directory(${CMAKE_CURRENT_SOURCE_DIR} PROJECT_SOURCES)
set(PROJECT_SOURCES main.cpp mainwindow.cpp mainwindow.h mainwindow.ui
    login.h login.cpp login.ui client.h client.cpp client.ui register.h register.cpp register.ui qrc/main.qrc
    quihelper.h quihelper.cpp qrc/main.rc common_proto.h player.cpp player.h player.ui videowidget.h videowidget.cpp
    browserdataanalysis.h browserdataanalysis.cpp browserdataanalysis.ui imageviewer.h imageviewer.cpp imageviewer.ui
)

#f (WIN32)
#    # 隐藏控制台
#    set(CMAKE_WIN32_EXECUTABLE ON)
#endif()

if(${QT_VERSION_MAJOR} GREATER_EQUAL 6)
    qt_add_executable(${PROJECT_NAME}
        MANUAL_FINALIZATION    #可选参数，手动结束创建目标的过程
        ${PROJECT_SOURCES}
    )
else()
    if(ANDROID)
        add_library(${PROJECT_NAME} SHARED
            ${PROJECT_SOURCES}
            )
    else()
        add_executable(${PROJECT_NAME}
            ${PROJECT_SOURCES}
        )
    endif()
endif()

# 链接 find_package() 导入的 Qt6::Widgets 库
target_link_libraries(${PROJECT_NAME} PRIVATE Qt${QT_VERSION_MAJOR}::Widgets)
# 链接 find_package() 导入的 Qt6::Network 库
target_link_libraries(${PROJECT_NAME} PRIVATE Qt${QT_VERSION_MAJOR}::Network)
target_link_libraries(${PROJECT_NAME} PRIVATE Qt${QT_VERSION_MAJOR}::MultimediaWidgets)
target_link_libraries(${PROJECT_NAME} PRIVATE Qt${QT_VERSION_MAJOR}::Charts)
target_link_libraries(${PROJECT_NAME} PRIVATE Qt${QT_VERSION_MAJOR}::Sql)

set(ICON_FILE ${CMAKE_CURRENT_SOURCE_DIR}/qrc/main.ico)
#设置目标可执行文件的属性，如MacOSX的Bundle标识符、版本号和短版本字符串，以及在Windows下作为可执行文件运行。
set_target_properties(${PROJECT_NAME} PROPERTIES
    MACOSX_BUNDLE_GUI_IDENTIFIER my.example.com
    MACOSX_BUNDLE_BUNDLE_VERSION ${PROJECT_VERSION}
    MACOSX_BUNDLE_SHORT_VERSION_STRING ${PROJECT_VERSION_MAJOR}.${PROJECT_VERSION_MINOR}
    MACOSX_BUNDLE TRUE
    MACOSX_BUNDLE_ICON_FILE ${ICON_FILE}
    WIN32_EXECUTABLE TRUE
)

install(TARGETS ${PROJECT_NAME}
    BUNDLE DESTINATION .
    LIBRARY DESTINATION ${CMAKE_INSTALL_LIBDIR}
)

if(QT_VERSION_MAJOR EQUAL 6)
    qt_finalize_executable(${PROJECT_NAME})   #结束生成可执行文件的过程
endif()

message("end cmake")
