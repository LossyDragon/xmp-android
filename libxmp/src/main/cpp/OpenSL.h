#ifndef XMP_ANDROID_MASTER_OPENSL_H
#define XMP_ANDROID_MASTER_OPENSL_H

#include <android/log.h>
#include <SLES/OpenSLES_Android.h>
#include <mutex>

#define TAG "XMP_Native"
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,TAG,__VA_ARGS__)
#define LOGW(...)  __android_log_print(ANDROID_LOG_WARN,TAG,__VA_ARGS__)
#define LOGI(...)  __android_log_print(ANDROID_LOG_INFO,TAG,__VA_ARGS__)

#define INC(x, max) do { \
    if (++(x) >= (max)) { (x) = 0; } \
} while (0)

#define DEC(x, max) do { \
    if (--(x) < 0) { (x) = (max) - 1; } \
} while (0)

#define BUFFER_TIME 40

static char *openSLBuffer;
static SLPlayItf player_play;
static SLObjectItf engine_obj;
static SLEngineItf engine_engine;
static SLObjectItf output_mix_obj;
static SLObjectItf player_obj;
static SLVolumeItf player_vol;
static SLAndroidSimpleBufferQueueItf buffer_queue;
static int buffer_num;
static int buffer_size;
static volatile int first_free, last_free;
static int playing;

class OpenSL {
public:
    ~OpenSL() = default;

    static int fill_buffer(int looped);

    static int get_volume();

    static int has_free_buffer();

    static int opensl_open(int sr, int num);

    static int open_audio(int rate, int latency);

    static int play_audio();

    static int restart_audio();

    static int set_volume(int vol);

    static int stop_audio();

    static void close_audio();

    static void drop_audio();

    static void flush_audio();

    static void opensl_close();

    static void player_callback(SLAndroidSimpleBufferQueueItf bq, void *context);

    static int play_buffer(void *buffer, int size, int looped);

};

#endif //XMP_ANDROID_MASTER_OPENSL_H
