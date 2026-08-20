cmake_minimum_required(VERSION 3.22.1)
project("mygbaemulator")

set(LIBMGBA_ONLY ON CACHE BOOL "Build only the core library")
set(BUILD_SHARED ON CACHE BOOL "Build shared library")
set(BUILD_STATIC OFF CACHE BOOL "Build static library")
set(DISABLE_FRONTENDS ON CACHE BOOL "Disable frontends")

add_subdirectory(../../../../mGBA-core mGBA-core)

add_library(${CMAKE_PROJECT_NAME} SHARED native-lib.cpp)
target_link_libraries(${CMAKE_PROJECT_NAME} mgba android log)
target_include_directories(${CMAKE_PROJECT_NAME} PRIVATE ../../../../mGBA-core/include)

# Match mgba's compile definitions
get_directory_property(MGBA_DIR_DEFS DIRECTORY ../../../../mGBA-core COMPILE_DEFINITIONS)
target_compile_definitions(${CMAKE_PROJECT_NAME} PRIVATE ${MGBA_DIR_DEFS})
target_compile_definitions(${CMAKE_PROJECT_NAME} PRIVATE $<TARGET_PROPERTY:mgba,COMPILE_DEFINITIONS>)
