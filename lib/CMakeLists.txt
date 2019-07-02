include_directories(${CMAKE_CURRENT_LIST_DIR})

set(lib_src "")
aux_source_directory(${CMAKE_CURRENT_LIST_DIR} lib_src)
add_library(${PROJECT_NAME}lib STATIC ${lib_src})