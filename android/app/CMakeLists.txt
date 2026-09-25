cmake_minimum_required(VERSION 3.4.1)

# Include Node.js header files
include_directories(${CMAKE_CURRENT_SOURCE_DIR}/libnode/include/node/)

# Import prebuilt libnode.so
add_library(
    libnode
    SHARED
    IMPORTED
)

set_target_properties(
    libnode
    PROPERTIES
    IMPORTED_LOCATION
    ${CMAKE_CURRENT_SOURCE_DIR}/libnode/bin/${ANDROID_ABI}/libnode.so
)

# Build native bridge library
add_library(
    native-lib
    SHARED
    src/main/cpp/native-lib.cpp
)

find_library(
    log-lib
    log
)

target_link_libraries(
    native-lib
    libnode
    ${log-lib}
)