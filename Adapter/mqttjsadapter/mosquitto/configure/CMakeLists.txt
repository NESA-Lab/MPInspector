cmake_minimum_required (VERSION 2.6)
project (ap_mosquitto)
add_library(ap_mosquitto SHARED ap_dummy.c)

include_directories("${CMAKE_SOURCE_DIR}/../devel/")