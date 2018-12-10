cmake_minimum_required(VERSION 3.12)

set(PROJECT_NAME "cornerstone")
project(${PROJECT_NAME})

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)


add_executable(${PROJECT_NAME} main.cpp parser.cpp texp.cpp)
