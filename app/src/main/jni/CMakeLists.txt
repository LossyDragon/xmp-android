cmake_minimum_required(VERSION 3.4)
project(xmp-jni LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 14)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Add libxmp's CMakeLists.txt
add_subdirectory(libxmp)

find_package(oboe REQUIRED CONFIG)

add_library(xmp-jni SHARED xmp-jni.cpp oboe.cpp)

# Link libraries
target_link_libraries(xmp-jni xmp_static oboe::oboe android log)

# Include directories
# xmp-jni.c needs use of xmp.h and common.h
target_include_directories(xmp-jni PRIVATE libxmp/include libxmp/src)
