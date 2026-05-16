cmake_minimum_required(VERSION 3.4)

set(PROJECT_NAME "xmp-jni")

project(${PROJECT_NAME} LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Add libxmp's CMakeLists.txt
add_subdirectory(libxmp)
target_compile_options(xmp_static PRIVATE -Wno-unused-function)

# Find Oboe package from gradle import.
find_package(oboe REQUIRED CONFIG)

add_library(${PROJECT_NAME} SHARED xmp-jni.cpp oboe.cpp)

# Link libraries
target_link_libraries(${PROJECT_NAME} xmp_static oboe::oboe android log)

# Include directories
# xmp-jni.cpp needs use of xmp.h and common.h
target_include_directories(${PROJECT_NAME} PRIVATE libxmp/include libxmp/src)
