#include <pthread.h>
#include <malloc.h>
#include <cstdlib>
#include <unistd.h>
#include "OpenSL.h"
#include <android/log.h>

std::mutex openSlMutex;
#define lock() const std::lock_guard<std::mutex> lock(openSlMutex);

#define TAG "XMP_Native"
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,TAG,__VA_ARGS__)
#define LOGW(...)  __android_log_print(ANDROID_LOG_WARN,TAG,__VA_ARGS__)

int OpenSL::fill_buffer(int looped) {
    int ret;

    /* fill and enqueue buffer */
    char *b = &openSLBuffer[first_free * buffer_size];
    INC(first_free, buffer_num);

    ret = play_buffer(b, buffer_size, looped);
    lock()
    if (buffer_queue != nullptr) {
        (*buffer_queue)->Enqueue(buffer_queue, b, buffer_size);
    }

    return ret;
}

int OpenSL::get_volume() {
    SLmillibel vol;
    SLresult r;

    r = (*player_vol)->GetVolumeLevel(player_vol, &vol);
    return r == SL_RESULT_SUCCESS ? -vol : -1;
}

int OpenSL::has_free_buffer() {
    bool freeBuffer = last_free != first_free;
    return freeBuffer;
}

int OpenSL::opensl_open(int sr, int num) {
    SLresult r;
    SLuint32 rate;

    switch (sr) {
        case 8000:
            rate = SL_SAMPLINGRATE_8;
            break;
        case 22050:
            rate = SL_SAMPLINGRATE_22_05;
            break;
        case 44100:
            rate = SL_SAMPLINGRATE_44_1;
            break;
        case 48000:
            rate = SL_SAMPLINGRATE_48;
            break;
        default: {
            LOGE("Failed to get valid sample rate");
            return -1;
        }
    }

    /* create engine */
    r = slCreateEngine(&engine_obj, 0, nullptr, 0, nullptr, nullptr);
    if (r != SL_RESULT_SUCCESS) {
        LOGE("Failed to create audio engine");
        return -1;
    }

    r = (*engine_obj)->Realize(engine_obj, SL_BOOLEAN_FALSE);
    if (r != SL_RESULT_SUCCESS) {
        LOGE("Failed to realize audio engine");
        return -1;
    }

    r = (*engine_obj)->GetInterface(engine_obj, SL_IID_ENGINE, &engine_engine);
    if (r != SL_RESULT_SUCCESS) {
        LOGE("Failed to get audio engine interface");
        (*engine_obj)->Destroy(engine_obj);
        return -1;
    }

    /* create output mix */
    const SLInterfaceID ids[] = {
            SL_IID_VOLUME
    };

    const SLboolean req[] = {
            SL_BOOLEAN_FALSE
    };

    r = (*engine_engine)->CreateOutputMix(engine_engine, &output_mix_obj, 1, ids, req);
    if (r != SL_RESULT_SUCCESS) {
        LOGE("Couldn't create output mixer");
        (*engine_obj)->Destroy(engine_obj);
        return -1;
    }

    /* realize output mix */
    r = (*output_mix_obj)->Realize(output_mix_obj, SL_BOOLEAN_FALSE);
    if (r != SL_RESULT_SUCCESS) {
        LOGE("Couldn't realize output mixer");
        (*engine_obj)->Destroy(engine_obj);
        return -1;
    }

    SLDataLocator_BufferQueue loc_bufq = {
            SL_DATALOCATOR_BUFFERQUEUE,
            static_cast<SLuint32>(num)
    };

    SLDataFormat_PCM format_pcm = {
            SL_DATAFORMAT_PCM,
            2,
            rate,
            SL_PCMSAMPLEFORMAT_FIXED_16,
            SL_PCMSAMPLEFORMAT_FIXED_16,
            SL_SPEAKER_FRONT_LEFT | SL_SPEAKER_FRONT_RIGHT,
            SL_BYTEORDER_LITTLEENDIAN
    };

    SLDataSource audio_source = {
            (void *) &loc_bufq,
            (void *) &format_pcm
    };

    /* configure audio sink */
    SLDataLocator_OutputMix loc_outmix = {
            SL_DATALOCATOR_OUTPUTMIX,
            output_mix_obj
    };

    SLDataSink audio_sink = {
            (void *) &loc_outmix,
            nullptr
    };

    /* create audio player */
    const SLInterfaceID ids1[] = {
            SL_IID_VOLUME, SL_IID_BUFFERQUEUE
    };

    const SLboolean req1[] = {
            SL_BOOLEAN_TRUE
    };

    r = (*engine_engine)->CreateAudioPlayer(engine_engine, &player_obj,
                                            &audio_source, &audio_sink, 2, ids1, req1);
    if (r != SL_RESULT_SUCCESS) {
        LOGE("Failed to create audio player");
        goto err2;
    }

    /* realize player */
    r = (*player_obj)->Realize(player_obj, SL_BOOLEAN_FALSE);
    if (r != SL_RESULT_SUCCESS) {
        LOGE("Failed to realize audio player");
        goto err2;
    }

    /* get play interface */
    r = (*player_obj)->GetInterface(player_obj, SL_IID_PLAY, &player_play);
    if (r != SL_RESULT_SUCCESS) {
        LOGE("Failed to get audio player interface");
        goto err3;
    }

    /* get volume interface */
    r = (*player_obj)->GetInterface(player_obj, SL_IID_VOLUME, &player_vol);
    if (r != SL_RESULT_SUCCESS) {
        LOGE("Failed to get volume interface");
        goto err3;
    }

    /* get buffer queue interface */
    r = (*player_obj)->GetInterface(player_obj, SL_IID_ANDROIDSIMPLEBUFFERQUEUE, &buffer_queue);
    if (r != SL_RESULT_SUCCESS) {
        LOGE("Failed to get buffer queue interface");
        goto err3;
    }

    /* register callback on buffer queue */
    r = (*buffer_queue)->RegisterCallback(buffer_queue, player_callback, nullptr);
    if (r != SL_RESULT_SUCCESS) {
        LOGE("Failed to register buffer queue callback");
        goto err3;
    }

    return 0;

    err3:
    (*player_obj)->Destroy(player_obj);

    err2:
    (*output_mix_obj)->Destroy(output_mix_obj);

    return -1;
}

int OpenSL::open_audio(int rate, int latency) {
    int ret;

    buffer_num = latency / BUFFER_TIME;
    buffer_size = rate * 2 * 2 * BUFFER_TIME / 1000;

    if (buffer_num < 3)
        buffer_num = 3;

    openSLBuffer = static_cast<char *>(malloc(buffer_size * buffer_num));
    if (openSLBuffer == nullptr)
        return -1;

    ret = opensl_open(rate, buffer_num);
    if (ret < 0)
        return ret;

    first_free = 0;
    last_free = buffer_num - 1;

    return buffer_num;
}

int OpenSL::play_audio() {
    flush_audio();

    /* set player state to playing */
    if (restart_audio() < 0)
        return -1;

    return 0;
}

int OpenSL::restart_audio() {
    int ret = 0;

    /* enqueue initial buffers */
    while (has_free_buffer()) {
        fill_buffer(0);
    }

    lock()
    if (player_play != nullptr) {
        ret = (*player_play)->SetPlayState(player_play, SL_PLAYSTATE_PLAYING);
    }

    return ret == SL_RESULT_SUCCESS ? 0 : -1;
}

int OpenSL::set_volume(int vol) {
    SLresult r;
    r = (*player_vol)->SetVolumeLevel(player_vol, (SLmillibel) -vol);
    return r == SL_RESULT_SUCCESS ? 0 : -1;
}

int OpenSL::stop_audio() {
    int ret = 0;

    drop_audio();

    lock()
    if (player_play != nullptr) {
        ret = (*player_play)->SetPlayState(player_play, SL_PLAYSTATE_STOPPED);
    }

    int ret2 = ret == SL_RESULT_SUCCESS ? 0 : -1;

    return ret2;
}

void OpenSL::close_audio() {
    opensl_close();
    free(openSLBuffer);
}

void OpenSL::drop_audio() {
    lock()
    if (buffer_queue != nullptr) {
        (*buffer_queue)->Clear(buffer_queue);
    }

    first_free = 0;
    last_free = buffer_num - 1;
}

void OpenSL::flush_audio() {
    SLAndroidSimpleBufferQueueState state;

    lock()
    if (buffer_queue != nullptr) {
        (*buffer_queue)->GetState(buffer_queue, &state);
    }

    while (state.count != 0) {
        usleep(10000);
        if (buffer_queue != nullptr) {
            (*buffer_queue)->GetState(buffer_queue, &state);
        }

    }
}

void OpenSL::opensl_close() {
    lock()

    if (player_obj != nullptr)
        (*player_obj)->Destroy(player_obj);

    if (output_mix_obj != nullptr)
        (*output_mix_obj)->Destroy(output_mix_obj);

    if (engine_obj != nullptr)
        (*engine_obj)->Destroy(engine_obj);

    player_obj = nullptr;
    output_mix_obj = nullptr;
    engine_obj = nullptr;

    player_play = nullptr;
    buffer_queue = nullptr;
}

void OpenSL::player_callback(SLAndroidSimpleBufferQueueItf bq, void *context) {
    INC(last_free, buffer_num);

    /* underrun, shouldn't happen */
    if (last_free == first_free) {
        DEC(last_free, buffer_num);
        LOGW("Under running");
    }
}
