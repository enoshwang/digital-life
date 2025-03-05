add_executable(test_utils test_utils.cpp ../extern/catch_amalgamated.cpp)
target_link_libraries(test_utils PRIVATE my_utils)
add_test(NAME test_utils COMMAND test_utils)