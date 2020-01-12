set(matcher_src "")
aux_source_directory(${CMAKE_CURRENT_LIST_DIR} matcher_src)
add_executable(matcher ${matcher_src})

target_link_libraries(matcher cornerstonelib_debug)
include_directories(${PROJECT_ROOT}/lib)

get_property(dirs DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR} PROPERTY INCLUDE_DIRECTORIES)
foreach(dir ${dirs})
  message(STATUS "dir='${dir}'")
endforeach()
