cmake_minimum_required(VERSION 3.12)

set(PROJECT_NAME "cornerstone-cpp")
project(${PROJECT_NAME})

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

aux_source_directory(${CMAKE_CURRENT_LIST_DIR}/lib src)
include_directories(${CMAKE_CURRENT_LIST_DIR}/lib)

if (GTEST)
  ADD_SUBDIRECTORY(test)
else (GTEST)
  ADD_SUBDIRECTORY(main)
endif (GTEST)

ADD_SUBDIRECTORY(tester)
