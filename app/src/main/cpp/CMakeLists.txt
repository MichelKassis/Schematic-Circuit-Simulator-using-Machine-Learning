cmake_minimum_required(VERSION 3.4.1)

add_library(image-processor SHARED
        image-processor.cpp)

# Include libraries needed for hello-jni lib
target_link_libraries(image-processor
        android
        log)