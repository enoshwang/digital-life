cmake_minimum_required(VERSION 3.31)

project(my_utils VERSION 1.0.0 LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 23)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

option(WARNINGS_AS_ERRORS "Treat warnings as errors" ON)
if(WARNINGS_AS_ERRORS)
    if(MSVC)
        add_compile_options(/WX)
    else()
        add_compile_options(-Werror)
    endif()
endif()

add_library(${PROJECT_NAME} STATIC
            src/filesystem.cpp
            src/uuid.cpp
            include/my_utils/utils.hpp
            include/my_utils/log.hpp
            include/my_utils/obj_pool.hpp
            include/my_utils/queue.hpp
            include/my_utils/singleton.hpp
            )

target_include_directories(${PROJECT_NAME}
    PUBLIC
    $<BUILD_INTERFACE:${CMAKE_CURRENT_SOURCE_DIR}/include>
    $<INSTALL_INTERFACE:include>
)

install(TARGETS ${PROJECT_NAME}
    EXPORT my_utils-targets
    ARCHIVE DESTINATION lib
    LIBRARY DESTINATION lib
    RUNTIME DESTINATION bin
    INCLUDES DESTINATION include
)

install(DIRECTORY include/ DESTINATION include)

install(EXPORT my_utils-targets
    FILE my_utils-config.cmake
    DESTINATION lib/cmake/my_utils
)

option(BUILD_TESTING "Build tests" ON)
if(BUILD_TESTING)
    enable_testing()
    add_subdirectory(tests)
endif()

add_executable(example examples/demo.cpp examples/demo_redefine.cpp)
target_link_libraries(example PRIVATE my_utils)