aux_source_directory(${CMAKE_CURRENT_LIST_DIR} test_src)
add_executable(${PROJECT_NAME}_test ${test_src})

target_link_libraries(${PROJECT_NAME}_test PUBLIC cornerstonelib_debug)
include_directories(${PROJECT_ROOT}/lib)

# add gtest
find_package(GTest REQUIRED)
enable_testing()
target_link_libraries(${PROJECT_NAME}_test PUBLIC gtest gtest_main)