set(typer_src "")
aux_source_directory(${CMAKE_CURRENT_LIST_DIR} typer_src)
add_executable(typer ${typer_src})

include_directories(${PROJECT_ROOT}/lib)
target_link_libraries(typer cornerstonelib_debug)