LOCAL_PATH := $(call my-dir)

# Copied from libxmp/jni
LIBXMP_PATH := $(LOCAL_PATH)/libxmp
include $(CLEAR_VARS)
include $(LIBXMP_PATH)/src/Makefile
include $(LIBXMP_PATH)/src/loaders/Makefile
include $(LIBXMP_PATH)/src/loaders/prowizard/Makefile
include $(LIBXMP_PATH)/src/depackers/Makefile
include $(LIBXMP_PATH)/src/depackers/lhasa/Makefile

SRC_SOURCES := $(addprefix $(LIBXMP_PATH)/src/,$(SRC_OBJS))
LOADERS_SOURCES := $(addprefix $(LIBXMP_PATH)/src/loaders/,$(LOADERS_OBJS))
PROWIZ_SOURCES := $(addprefix $(LIBXMP_PATH)/src/loaders/prowizard/,$(PROWIZ_OBJS))
LHASA_SOURCES := $(addprefix $(LIBXMP_PATH)/src/depackers/lhasa/,$(LHASA_OBJS))
DEPACKERS_SOURCES := $(addprefix $(LIBXMP_PATH)/src/depackers/,$(DEPACKERS_OBJS))

LOCAL_MODULE := xmp
LOCAL_SHORT_COMMANDS := true # Yay Windows
LOCAL_CFLAGS := -O3 -DHAVE_MKSTEMP -DHAVE_FNMATCH -DHAVE_DIRENT -DHAVE_POWF \
                -I$(LIBXMP_PATH)/include
LOCAL_SRC_FILES := $(SRC_SOURCES:.o=.c) \
                   $(LOADERS_SOURCES:.o=.c) \
                   $(PROWIZ_SOURCES:.o=.c) \
                   $(LHASA_SOURCES:.o=.c) \
                   $(DEPACKERS_SOURCES:.o=.c)
LOCAL_EXPORT_C_INCLUDES := $(LIBXMP_PATH)/include
include $(BUILD_STATIC_LIBRARY)

# jni wrapper
include $(CLEAR_VARS)
LOCAL_MODULE := xmp-jni
LOCAL_CFLAGS := -O3 -Wno-int-to-pointer-cast -Wno-pointer-to-int-cast
LOCAL_STATIC_LIBRARIES := xmp
LOCAL_SRC_FILES := xmp-jni.c opensl.c
LOCAL_LDLIBS := -lOpenSLES
include $(BUILD_SHARED_LIBRARY)