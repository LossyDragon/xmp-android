LOCAL_PATH := $(call my-dir)/libxmp

include $(CLEAR_VARS)

include $(LOCAL_PATH)/src/Makefile
include $(LOCAL_PATH)/src/loaders/Makefile
include $(LOCAL_PATH)/src/loaders/prowizard/Makefile
include $(LOCAL_PATH)/src/depackers/Makefile

SRC_SOURCES := $(addprefix src/,$(SRC_OBJS))
LOADERS_SOURCES := $(addprefix src/loaders/,$(LOADERS_OBJS))
PROWIZ_SOURCES := $(addprefix src/loaders/prowizard/,$(PROWIZ_OBJS))
DEPACKERS_SOURCES := $(addprefix src/depackers/,$(DEPACKERS_OBJS))

LOCAL_MODULE := libxmp-jni

LOCAL_CFLAGS := -O3 -DHAVE_MKSTEMP -DHAVE_FNMATCH -DHAVE_ROUND \
 				-I$(LOCAL_PATH)/include -I$(LOCAL_PATH)/src \
 				-Wno-int-to-pointer-cast -Wno-pointer-to-int-cast

LOCAL_SRC_FILES := $(SRC_SOURCES:.o=.c) \
		   $(LOADERS_SOURCES:.o=.c) \
		   $(PROWIZ_SOURCES:.o=.c) \
		   $(DEPACKERS_SOURCES:.o=.c) \
		   $(LOCAL_PATH)/../xmp-jni.cpp \
		   $(LOCAL_PATH)/../OpenSL.cpp

# for native audio
LOCAL_LDLIBS := -lOpenSLES -llog

include $(BUILD_SHARED_LIBRARY)