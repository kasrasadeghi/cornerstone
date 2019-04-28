cmake_minimum_required(VERSION 3.12)

set(PROJECT_NAME "cornerstone-cpp")
project(${PROJECT_NAME})

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

list(APPEND src parser.cpp texp.cpp gen.cpp matcher.cpp grammar.cpp type.cpp)

if (GTEST)
  find_package(GTest REQUIRED)
  enable_testing()
  add_executable(${PROJECT_NAME}_test test.cpp ${src})
  target_link_libraries(${PROJECT_NAME}_test PUBLIC gtest gtest_main)
else (GTEST)
  add_executable(${PROJECT_NAME} main.cpp ${src})
endif (GTEST)

