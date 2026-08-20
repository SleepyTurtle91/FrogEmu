# Install script for directory: /workspace/projects/MyGBAEmulator/mGBA-core

# Set the install prefix
if(NOT DEFINED CMAKE_INSTALL_PREFIX)
  set(CMAKE_INSTALL_PREFIX "/usr/local")
endif()
string(REGEX REPLACE "/$" "" CMAKE_INSTALL_PREFIX "${CMAKE_INSTALL_PREFIX}")

# Set the install configuration name.
if(NOT DEFINED CMAKE_INSTALL_CONFIG_NAME)
  if(BUILD_TYPE)
    string(REGEX REPLACE "^[^A-Za-z0-9_]+" ""
           CMAKE_INSTALL_CONFIG_NAME "${BUILD_TYPE}")
  else()
    set(CMAKE_INSTALL_CONFIG_NAME "Debug")
  endif()
  message(STATUS "Install configuration: \"${CMAKE_INSTALL_CONFIG_NAME}\"")
endif()

# Set the component getting installed.
if(NOT CMAKE_INSTALL_COMPONENT)
  if(COMPONENT)
    message(STATUS "Install component: \"${COMPONENT}\"")
    set(CMAKE_INSTALL_COMPONENT "${COMPONENT}")
  else()
    set(CMAKE_INSTALL_COMPONENT)
  endif()
endif()

# Install shared libraries without execute permission?
if(NOT DEFINED CMAKE_INSTALL_SO_NO_EXE)
  set(CMAKE_INSTALL_SO_NO_EXE "0")
endif()

# Is this installation the result of a crosscompile?
if(NOT DEFINED CMAKE_CROSSCOMPILING)
  set(CMAKE_CROSSCOMPILING "TRUE")
endif()

# Set default install directory permissions.
if(NOT DEFINED CMAKE_OBJDUMP)
  set(CMAKE_OBJDUMP "/home/user/development/android-sdk/ndk/25.1.8937393/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-objdump")
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xUnspecifiedx" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/lib" TYPE STATIC_LIBRARY FILES "/workspace/projects/MyGBAEmulator/app/.cxx/Debug/62445o3d/x86_64/mGBA-core/libmgba.a")
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xmgbax" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/share/icons/hicolor/16x16/apps" TYPE FILE RENAME "io.mgba.mGBA.png" FILES "/workspace/projects/MyGBAEmulator/mGBA-core/res/mgba-16.png")
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xmgbax" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/share/icons/hicolor/24x24/apps" TYPE FILE RENAME "io.mgba.mGBA.png" FILES "/workspace/projects/MyGBAEmulator/mGBA-core/res/mgba-24.png")
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xmgbax" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/share/icons/hicolor/32x32/apps" TYPE FILE RENAME "io.mgba.mGBA.png" FILES "/workspace/projects/MyGBAEmulator/mGBA-core/res/mgba-32.png")
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xmgbax" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/share/icons/hicolor/48x48/apps" TYPE FILE RENAME "io.mgba.mGBA.png" FILES "/workspace/projects/MyGBAEmulator/mGBA-core/res/mgba-48.png")
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xmgbax" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/share/icons/hicolor/64x64/apps" TYPE FILE RENAME "io.mgba.mGBA.png" FILES "/workspace/projects/MyGBAEmulator/mGBA-core/res/mgba-64.png")
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xmgbax" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/share/icons/hicolor/96x96/apps" TYPE FILE RENAME "io.mgba.mGBA.png" FILES "/workspace/projects/MyGBAEmulator/mGBA-core/res/mgba-96.png")
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xmgbax" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/share/icons/hicolor/128x128/apps" TYPE FILE RENAME "io.mgba.mGBA.png" FILES "/workspace/projects/MyGBAEmulator/mGBA-core/res/mgba-128.png")
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xmgbax" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/share/icons/hicolor/256x256/apps" TYPE FILE RENAME "io.mgba.mGBA.png" FILES "/workspace/projects/MyGBAEmulator/mGBA-core/res/mgba-256.png")
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xmgbax" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/share/icons/hicolor/512x512/apps" TYPE FILE RENAME "io.mgba.mGBA.png" FILES "/workspace/projects/MyGBAEmulator/mGBA-core/res/mgba-512.png")
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xmgba-devx" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/include" TYPE DIRECTORY FILES "/workspace/projects/MyGBAEmulator/mGBA-core/include/mgba" FILES_MATCHING REGEX "/[^/]*\\.h$")
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xmgba-devx" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/include" TYPE DIRECTORY FILES "/workspace/projects/MyGBAEmulator/mGBA-core/include/mgba-util" FILES_MATCHING REGEX "/[^/]*\\.h$")
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xmgba-devx" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/include/mgba" TYPE FILE FILES "/workspace/projects/MyGBAEmulator/app/.cxx/Debug/62445o3d/x86_64/mGBA-core/include/mgba/flags.h")
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xmgbax" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/share/doc/mGBA/licenses" TYPE FILE FILES "/workspace/projects/MyGBAEmulator/mGBA-core/res/licenses/inih.txt")
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xmgbax" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/share/doc/mGBA" TYPE FILE FILES
    "/workspace/projects/MyGBAEmulator/mGBA-core/README.md"
    "/workspace/projects/MyGBAEmulator/mGBA-core/README_DE.md"
    "/workspace/projects/MyGBAEmulator/mGBA-core/README_ES.md"
    "/workspace/projects/MyGBAEmulator/mGBA-core/README_JP.md"
    "/workspace/projects/MyGBAEmulator/mGBA-core/README_ZH_CN.md"
    )
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xmgbax" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/share/doc/mGBA" TYPE FILE FILES
    "/workspace/projects/MyGBAEmulator/mGBA-core/CHANGES"
    "/workspace/projects/MyGBAEmulator/mGBA-core/LICENSE"
    )
endif()

if(NOT CMAKE_INSTALL_LOCAL_ONLY)
  # Include the install script for each subdirectory.
  include("/workspace/projects/MyGBAEmulator/app/.cxx/Debug/62445o3d/x86_64/mGBA-core/src/debugger/cmake_install.cmake")
  include("/workspace/projects/MyGBAEmulator/app/.cxx/Debug/62445o3d/x86_64/mGBA-core/src/feature/cmake_install.cmake")
  include("/workspace/projects/MyGBAEmulator/app/.cxx/Debug/62445o3d/x86_64/mGBA-core/src/arm/cmake_install.cmake")
  include("/workspace/projects/MyGBAEmulator/app/.cxx/Debug/62445o3d/x86_64/mGBA-core/src/core/cmake_install.cmake")
  include("/workspace/projects/MyGBAEmulator/app/.cxx/Debug/62445o3d/x86_64/mGBA-core/src/gb/cmake_install.cmake")
  include("/workspace/projects/MyGBAEmulator/app/.cxx/Debug/62445o3d/x86_64/mGBA-core/src/gba/cmake_install.cmake")
  include("/workspace/projects/MyGBAEmulator/app/.cxx/Debug/62445o3d/x86_64/mGBA-core/src/sm83/cmake_install.cmake")
  include("/workspace/projects/MyGBAEmulator/app/.cxx/Debug/62445o3d/x86_64/mGBA-core/src/util/cmake_install.cmake")
  include("/workspace/projects/MyGBAEmulator/app/.cxx/Debug/62445o3d/x86_64/mGBA-core/test/cmake_install.cmake")

endif()

