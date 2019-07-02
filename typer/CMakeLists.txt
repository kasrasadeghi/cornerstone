set(src "")
aux_source_directory(${CMAKE_CURRENT_LIST_DIR} src)
add_executable(typer ${src} ${CMAKE_SOURCE_DIR})