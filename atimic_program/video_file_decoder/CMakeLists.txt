cmake_minimum_required(VERSION 3.28)

set(PROJECT_NAME video_file_decoder)
project(${PROJECT_NAME} VERSION 1.0 LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED True)
set(CMAKE_CXX_EXTENSIONS False)

include_directories(${CMAKE_SOURCE_DIR}/include)

aux_source_directory(${CMAKE_CURRENT_SOURCE_DIR}/src SRC)
add_executable(${PROJECT_NAME} ${SRC})