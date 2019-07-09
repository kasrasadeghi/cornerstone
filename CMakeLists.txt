cmake_minimum_required(VERSION 3.12)

set(PROJECT_NAME "cornerstone")
set(PROJECT_ROOT ${CMAKE_CURRENT_LIST_DIR})

project(${PROJECT_NAME})

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

add_subdirectory(lib)

if (GTEST)
  add_subdirectory(test)
else (GTEST)
  add_subdirectory(driver)
endif (GTEST)

add_subdirectory(tester)
add_subdirectory(typer)
