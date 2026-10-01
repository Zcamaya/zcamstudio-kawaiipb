cmake_minimum_required(VERSION 3.22.1)
project(kawaii_escpr C)

file(GLOB ESC_PR_CORE_SOURCES CONFIGURE_DEPENDS "${CMAKE_CURRENT_SOURCE_DIR}/escpr/lib/*.c")

add_library(kawaii_escpr STATIC ${ESC_PR_CORE_SOURCES})
target_include_directories(kawaii_escpr PUBLIC escpr escpr/lib)
target_compile_definitions(kawaii_escpr PRIVATE
    HAVE_CONFIG_H
    GCOMSW_PRT_USE_USB
    GCOMSW_PRT_USE_LPR
    GCOMSW_PRT_USE_RAW
    GCOMSW_EL_CDLABEL
    GCOMSW_EF_MAINTE
    GCOMSW_ASYNCJOB_SUPPORT
    GCOMSW_RAWFILE_SUPPORT
)
target_compile_options(kawaii_escpr PRIVATE
    -Wno-pointer-sign
    -Wno-unused-parameter
    -Wno-deprecated-declarations
)

file(GLOB GUTENPRINT_CORE_SOURCES CONFIGURE_DEPENDS "${CMAKE_CURRENT_SOURCE_DIR}/third_party/gutenprint/src/main/*.c")
add_library(kawaii_gutenprint STATIC ${GUTENPRINT_CORE_SOURCES})
target_include_directories(kawaii_gutenprint PUBLIC
    third_party/gutenprint/include
    third_party/gutenprint/src/main
)
target_compile_definitions(kawaii_gutenprint PRIVATE
    PACKAGE=\"Gutenprint\"
    VERSION=\"5.3.6-pre\"
    RELEASE_DATE=\"2026-02-01\"
    GUTENPRINT_RELEASE_VERSION=\"5.3\"
    STP_MAJOR_VERSION=5
    STP_MINOR_VERSION=3
    STP_MICRO_VERSION=6
    STP_CURRENT_INTERFACE=0
    STP_INTERFACE_AGE=0
    STP_BINARY_AGE=0
    PKGXMLDATADIR=\"/data/gutenprint\"
    PKGMODULEDIR=\"/data/gutenprint/modules\"
    PACKAGE_DATA_DIR=\"/data/gutenprint\"
    PACKAGE_LIB_DIR=\"/data/gutenprint/lib\"
)
target_compile_options(kawaii_gutenprint PRIVATE
    -Wno-pointer-sign
    -Wno-unused-parameter
    -Wno-deprecated-declarations
)

add_library(kawaii_escpr_jni SHARED epson_android_jni.c)
target_include_directories(kawaii_escpr_jni PRIVATE escpr escpr/lib)
target_link_libraries(kawaii_escpr_jni PRIVATE kawaii_escpr kawaii_gutenprint log m)

add_library(kawaii_gutenprint_jni SHARED gutenprint_android_jni.c)
target_include_directories(kawaii_gutenprint_jni PRIVATE
    third_party/gutenprint/include
    third_party/gutenprint/src/main
)
target_link_libraries(kawaii_gutenprint_jni PRIVATE kawaii_gutenprint log m dl)