## I probably DORKED this up, but this compiles better.
## See: https://github.com/cmatsuoka/libxmp/issues/119
cmake_minimum_required(VERSION 3.2)

project(libxmp VERSION 4.5.0 LANGUAGES C CXX)

set(XMP_WITH_DEMO_PLAYER OFF)

set(LIBXMP_CORE_PLAYER OFF)

set(AMIGA ON)

set(CMAKE_C_STANDARD 99)

set(LIBXMP_SRC)

if (CMAKE_BUILD_TYPE_LOWER STREQUAL "debug")
    add_definitions(-DDEBUG -D_DEBUG)
endif ()

if ("${CMAKE_CXX_COMPILER_ID}" STREQUAL "Clang")
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wno-shift-negative-value")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wno-shift-negative-value")
endif ()

list(APPEND LIBXMP_SRC
        libxmp/src/control.c
        libxmp/src/dataio.c
        libxmp/src/effects.c
        libxmp/src/filter.c
        libxmp/src/fmopl.c
        libxmp/src/fnmatch.c
        libxmp/src/format.c
        libxmp/src/hio.c
        libxmp/src/lfo.c
        libxmp/src/load.c
        libxmp/src/loaders/669_load.c
        libxmp/src/loaders/abk_load.c
        libxmp/src/loaders/amf_load.c
        libxmp/src/loaders/arch_load.c
        libxmp/src/loaders/asif.c
        libxmp/src/loaders/asylum_load.c
        libxmp/src/loaders/chip_load.c
        libxmp/src/loaders/common.c
        libxmp/src/loaders/dbm_load.c
        libxmp/src/loaders/digi_load.c
        libxmp/src/loaders/dt_load.c
        libxmp/src/loaders/emod_load.c
        libxmp/src/loaders/far_load.c
        libxmp/src/loaders/flt_load.c
        libxmp/src/loaders/fnk_load.c
        libxmp/src/loaders/gal4_load.c
        libxmp/src/loaders/gal5_load.c
        libxmp/src/loaders/gdm_load.c
        libxmp/src/loaders/hmn_load.c
        libxmp/src/loaders/ice_load.c
        libxmp/src/loaders/iff.c
        libxmp/src/loaders/imf_load.c
        libxmp/src/loaders/ims_load.c
        libxmp/src/loaders/it_load.c
        libxmp/src/loaders/itsex.c
        libxmp/src/loaders/liq_load.c
        libxmp/src/loaders/masi_load.c
        libxmp/src/loaders/mdl_load.c
        libxmp/src/loaders/med2_load.c
        libxmp/src/loaders/med3_load.c
        libxmp/src/loaders/med4_load.c
        libxmp/src/loaders/mfp_load.c
        libxmp/src/loaders/mgt_load.c
        libxmp/src/loaders/mmd1_load.c
        libxmp/src/loaders/mmd3_load.c
        libxmp/src/loaders/mmd_common.c
        libxmp/src/loaders/mod_load.c
        libxmp/src/loaders/mtm_load.c
        libxmp/src/loaders/no_load.c
        libxmp/src/loaders/okt_load.c
        libxmp/src/loaders/prowizard/ac1d.c
        libxmp/src/loaders/prowizard/di.c
        libxmp/src/loaders/prowizard/eureka.c
        libxmp/src/loaders/prowizard/fc-m.c
        libxmp/src/loaders/prowizard/fuchs.c
        libxmp/src/loaders/prowizard/fuzzac.c
        libxmp/src/loaders/prowizard/gmc.c
        libxmp/src/loaders/prowizard/heatseek.c
        libxmp/src/loaders/prowizard/hrt.c
        libxmp/src/loaders/prowizard/ksm.c
        libxmp/src/loaders/prowizard/mp.c
        libxmp/src/loaders/prowizard/noiserun.c
        libxmp/src/loaders/prowizard/novotrade.c
        libxmp/src/loaders/prowizard/np1.c
        libxmp/src/loaders/prowizard/np2.c
        libxmp/src/loaders/prowizard/np3.c
        libxmp/src/loaders/prowizard/p40.c
        libxmp/src/loaders/prowizard/p61a.c
        libxmp/src/loaders/prowizard/pha.c
        libxmp/src/loaders/prowizard/pm10c.c
        libxmp/src/loaders/prowizard/pm18a.c
        libxmp/src/loaders/prowizard/pp10.c
        libxmp/src/loaders/prowizard/pp21.c
        libxmp/src/loaders/prowizard/prowiz.c
        libxmp/src/loaders/prowizard/prun1.c
        libxmp/src/loaders/prowizard/prun2.c
        libxmp/src/loaders/prowizard/ptktable.c
        libxmp/src/loaders/prowizard/skyt.c
        libxmp/src/loaders/prowizard/starpack.c
        libxmp/src/loaders/prowizard/tdd.c
        libxmp/src/loaders/prowizard/theplayer.c
        libxmp/src/loaders/prowizard/titanics.c
        libxmp/src/loaders/prowizard/tp1.c
        libxmp/src/loaders/prowizard/tp3.c
        libxmp/src/loaders/prowizard/tuning.c
        libxmp/src/loaders/prowizard/unic2.c
        libxmp/src/loaders/prowizard/unic.c
        libxmp/src/loaders/prowizard/wn.c
        libxmp/src/loaders/prowizard/xann.c
        libxmp/src/loaders/prowizard/zen.c
        libxmp/src/loaders/psm_load.c
        libxmp/src/loaders/pt3_load.c
        libxmp/src/loaders/ptm_load.c
        libxmp/src/loaders/pw_load.c
        libxmp/src/loaders/rtm_load.c
        libxmp/src/loaders/s3m_load.c
        libxmp/src/loaders/sample.c
        libxmp/src/loaders/sfx_load.c
        libxmp/src/loaders/stim_load.c
        libxmp/src/loaders/st_load.c
        libxmp/src/loaders/stm_load.c
        libxmp/src/loaders/stx_load.c
        libxmp/src/loaders/sym_load.c
        libxmp/src/loaders/ult_load.c
        libxmp/src/loaders/umx_load.c
        libxmp/src/loaders/voltable.c
        libxmp/src/loaders/xm_load.c
        libxmp/src/load_helpers.c
        libxmp/src/lutgen.c
        libxmp/src/md5.c
        libxmp/src/memio.c
        libxmp/src/mix_all.c
        libxmp/src/mixer.c
        libxmp/src/mix_paula.c
        libxmp/src/mkstemp.c
        libxmp/src/period.c
        libxmp/src/player.c
        libxmp/src/read_event.c
        libxmp/src/scan.c
        libxmp/src/smix.c
        libxmp/src/tempfile.c
        libxmp/src/virtual.c

        #Android JNI 'n Stuff
        xmp-jni.c
        opensl.c
        )

if (NOT LIBXMP_CORE_PLAYER)
    list(APPEND LIBXMP_SRC
            libxmp/src/depackers/arcfs.c
            libxmp/src/depackers/bunzip2.c
            libxmp/src/depackers/crc32.c
            libxmp/src/depackers/gunzip.c
            libxmp/src/depackers/inflate.c
            libxmp/src/depackers/mmcmp.c
            libxmp/src/depackers/muse.c
            libxmp/src/depackers/oxm.c
            libxmp/src/depackers/ppdepack.c
            libxmp/src/depackers/readlzw.c
            libxmp/src/depackers/readrle.c
            libxmp/src/depackers/s404_dec.c
            libxmp/src/depackers/unarc.c
            libxmp/src/depackers/uncompress.c
            libxmp/src/depackers/unlha.c
            libxmp/src/depackers/unlzx.c
            libxmp/src/depackers/unsqsh.c
            libxmp/src/depackers/unxz.c
            libxmp/src/depackers/unzip.c
            libxmp/src/depackers/vorbis.c
            libxmp/src/depackers/xz_dec_lzma2.c
            libxmp/src/depackers/xz_dec_stream.c
            libxmp/src/extras.c
            libxmp/src/hmn_extras.c
            libxmp/src/med_extras.c
            )
endif ()

if (AMIGA)
    list(APPEND LIBXMP_SRC
            libxmp/src/depackers/xfd.c
            libxmp/src/depackers/xfd_link.c
            )
endif ()

add_library(xmp SHARED ${LIBXMP_SRC})

target_link_libraries(xmp android log OpenSLES)

target_compile_definitions(xmp PRIVATE -DBUILDING_STATIC)

target_include_directories(xmp PUBLIC ${CMAKE_CURRENT_SOURCE_DIR}/include)
target_include_directories(xmp PRIVATE ${CMAKE_CURRENT_SOURCE_DIR}/libxmp/src)

install(FILES
        include/xmp.h
        DESTINATION libxmp/src/include/xmp/)