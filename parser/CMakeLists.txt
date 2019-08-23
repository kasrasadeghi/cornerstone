aux_source_directory(${CMAKE_CURRENT_LIST_DIR} parser_src)
add_executable(parser ${parser_src})

target_link_libraries(parser cornerstonelib_debug)
include_directories(${PROJECT_ROOT}/lib)
