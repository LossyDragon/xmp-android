cmake_minimum_required(VERSION 3.4)
project(xmp-jni LANGUAGES C)

# Add libxmp's CMakeLists.txt
add_subdirectory(libxmp)

add_library(xmp-jni SHARED xmp-jni.c opensl.c)

target_link_libraries(xmp-jni xmp_static OpenSLES android log)

# xmp-jni.c needs use of xmp.h and common.h
target_include_directories(xmp-jni PRIVATE libxmp/include libxmp/src)
