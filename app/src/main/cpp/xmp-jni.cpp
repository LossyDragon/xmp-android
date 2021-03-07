/* Simple and ugly interface adaptor for jni
 * If you need a JNI interface for libxmp, check the Libxmp Java API
 * at https://github.com/cmatsuoka/libxmp-java
 */
#pragma ide diagnostic ignored "UnusedLocalVariable"

#include <cstring>
#include <cstdio>
#include <cstdlib>
#include <pthread.h>
#include <jni.h>
#include <android/log.h>
#include "xmp.h"
#include "OpenSL.h"

#define PERIOD_BASE 13696
#define MAX_BUFFER_SIZE 256
#define TAG "XMPNative"
#define LOG(...)  __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)

static xmp_context ctx = nullptr;
static struct xmp_module_info mi;
static struct xmp_frame_info *fi;

static int _playing = 0;
static int _cur_vol[XMP_MAX_CHANNELS];
static int _hold_vol[XMP_MAX_CHANNELS];
static int _pan[XMP_MAX_CHANNELS];
static int _ins[XMP_MAX_CHANNELS];
static int _key[XMP_MAX_CHANNELS];
static int _period[XMP_MAX_CHANNELS];
static int _finalvol[XMP_MAX_CHANNELS];
static int _last_key[XMP_MAX_CHANNELS];
static int _pos[XMP_MAX_CHANNELS];
static int _decay = 4;
static int _sequence;
static int _mod_is_loaded;
static int _now, _before;
static int _buffer_num;
static int _loop_count;
static char _buffer[MAX_BUFFER_SIZE];

std::mutex jniMutex;
#define lock() const std::lock_guard<std::mutex> lock(jniMutex);

extern "C" {

static struct xmp_subinstrument *get_subinstrument(int ins, int key) {
    if (ins >= 0 && ins < mi.mod->ins) {
        if (mi.mod->xxi[ins].map[key].ins != 0xff) {
            int mapped = mi.mod->xxi[ins].map[key].ins;
            return &mi.mod->xxi[ins].sub[mapped];
        }
    }

    return nullptr;
}

int OpenSL::play_buffer(void *buffer, int size, int looped) {
    int ret = -XMP_END;
    int num_loop;

    lock()

    if (_playing) {
        num_loop = looped ? 0 : _loop_count + 1;
        ret = xmp_play_buffer(ctx, buffer, size, num_loop);
        xmp_get_frame_info(ctx, &fi[_now]);
        INC(_before, _buffer_num);
        _now = (_before + _buffer_num - 1) % _buffer_num;
        _loop_count = fi[_now].loop_count;
    }

    return ret;
}

/* For ModList */
JNIEXPORT jboolean JNICALL
Java_org_helllabs_android_xmp_Xmp_init(JNIEnv *env, jobject obj, jint rate, jint ms) {

    ctx = xmp_create_context();

    if ((_buffer_num = OpenSL::open_audio(rate, ms)) < 0) {
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

JNIEXPORT void JNICALL
Java_org_helllabs_android_xmp_Xmp_deinit(JNIEnv *env, jobject obj) {
    xmp_free_context(ctx);
    OpenSL::close_audio();
}

JNIEXPORT jint JNICALL
Java_org_helllabs_android_xmp_Xmp_loadModule(JNIEnv *env, jobject obj, jstring name) {
    const char *filename;
    int res;

    filename = (*env).GetStringUTFChars(name, nullptr);
    /* __android_log_print(ANDROID_LOG_DEBUG, "libxmp", "%s", filename); */
    res = xmp_load_module(ctx, (char *) filename);
    (*env).ReleaseStringUTFChars(name, filename);

    xmp_get_module_info(ctx, &mi);

    memset(_pos, 0, XMP_MAX_CHANNELS * sizeof(int));
    _sequence = 0;
    _mod_is_loaded = 1;

    return res;
}

JNIEXPORT jboolean JNICALL
Java_org_helllabs_android_xmp_Xmp_testModule(JNIEnv *env, jobject obj, jstring name, jobject info) {
    const char *filename;
    int i, res;
    struct xmp_test_info ti{};

    filename = (*env).GetStringUTFChars(name, nullptr);
    /* __android_log_print(ANDROID_LOG_DEBUG, "libxmp", "%s", filename); */
    res = xmp_test_module((char *) filename, &ti);

    /* If the module title is empty, use the file basename */
    unsigned char modName = strlen(ti.name) - 1;
    for (i = modName; i >= 0; i--) {
        if (ti.name[i] == ' ') {
            ti.name[i] = 0;
        } else {
            break;
        }
    }
    if (strlen(ti.name) == 0) {
        const char *x = strrchr(filename, '/');
        if (x == nullptr) {
            x = filename;
        }
        strncpy(ti.name, x + 1, XMP_NAME_SIZE);
    }

    (*env).ReleaseStringUTFChars(name, filename);

    if (res == 0) {
        if (info != nullptr) {
            jclass modInfoClass = (*env).FindClass("org/helllabs/android/xmp/util/ModInfo");
            jfieldID field;

            if (modInfoClass == nullptr)
                return JNI_FALSE;

            field = (*env).GetFieldID(modInfoClass, "name", "Ljava/lang/String;");
            if (field == nullptr)
                return JNI_FALSE;
            (*env).SetObjectField(info, field, (*env).NewStringUTF(ti.name));

            field = (*env).GetFieldID(modInfoClass, "type", "Ljava/lang/String;");
            if (field == nullptr)
                return JNI_FALSE;
            (*env).SetObjectField(info, field, (*env).NewStringUTF(ti.type));
        }

        return JNI_TRUE;
    }

    return JNI_FALSE;
}

JNIEXPORT jint JNICALL
Java_org_helllabs_android_xmp_Xmp_releaseModule(JNIEnv *env, jobject obj) {
    lock()
    if (_mod_is_loaded) {
        _mod_is_loaded = 0;
        xmp_release_module(ctx);
    }

    return 0;
}

JNIEXPORT jint JNICALL
Java_org_helllabs_android_xmp_Xmp_startPlayer(JNIEnv *env, jobject obj, jint rate) {
    int i, ret;

    lock()

    fi = static_cast<xmp_frame_info *>(calloc(1, _buffer_num * sizeof(struct xmp_frame_info)));
    if (fi == nullptr) {
        return -101;
    }

    for (i = 0; i < XMP_MAX_CHANNELS; i++) {
        _key[i] = -1;
        _last_key[i] = -1;
    }

    _now = _before = 0;
    _loop_count = 0;
    _playing = 1;

    // Don't pass any format flags.
    ret = xmp_start_player(ctx, rate, 0 & XMP_FORMAT_UNSIGNED);

    return ret;
}

JNIEXPORT jint JNICALL
Java_org_helllabs_android_xmp_Xmp_endPlayer(JNIEnv *env, jobject obj) {
    lock()
    if (_playing) {
        _playing = 0;
        xmp_end_player(ctx);
        free(fi);
        fi = nullptr;
    }

    return 0;
}

JNIEXPORT jint JNICALL
Java_org_helllabs_android_xmp_Xmp_playAudio(JNIEnv *env, jobject obj) {
    return OpenSL::play_audio();
}

JNIEXPORT void JNICALL
Java_org_helllabs_android_xmp_Xmp_dropAudio(JNIEnv *env, jobject obj) {
    return OpenSL::drop_audio();
}

JNIEXPORT jboolean JNICALL
Java_org_helllabs_android_xmp_Xmp_stopAudio(JNIEnv *env, jobject obj) {
    return OpenSL::stop_audio() == 0 ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_org_helllabs_android_xmp_Xmp_restartAudio(JNIEnv *env, jobject obj) {
    return OpenSL::restart_audio() == 0 ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_org_helllabs_android_xmp_Xmp_hasFreeBuffer(JNIEnv *env, jobject obj) {
    return OpenSL::has_free_buffer() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jint JNICALL
Java_org_helllabs_android_xmp_Xmp_fillBuffer(JNIEnv *env, jobject obj, jboolean looped) {
    return OpenSL::fill_buffer(looped);
}

JNIEXPORT jint JNICALL
Java_org_helllabs_android_xmp_Xmp_nextPosition(JNIEnv *env, jobject obj) {
    return xmp_next_position(ctx);
}

JNIEXPORT jint JNICALL
Java_org_helllabs_android_xmp_Xmp_prevPosition(JNIEnv *env, jobject obj) {
    return xmp_prev_position(ctx);
}

JNIEXPORT jint JNICALL
Java_org_helllabs_android_xmp_Xmp_setPosition(JNIEnv *env, jobject obj, jint n) {
    return xmp_set_position(ctx, n);
}

JNIEXPORT jint JNICALL
Java_org_helllabs_android_xmp_Xmp_stopModule(JNIEnv *env, jobject obj) {
    xmp_stop_module(ctx);
    return 0;
}

JNIEXPORT jint JNICALL
Java_org_helllabs_android_xmp_Xmp_restartModule(JNIEnv *env, jobject obj) {
    xmp_restart_module(ctx);
    return 0;
}

JNIEXPORT jint JNICALL
Java_org_helllabs_android_xmp_Xmp_seek(JNIEnv *env, jobject obj, jint time) {
    int ret;
    int i;

    lock()

    ret = xmp_seek_time(ctx, time);
    if (_playing) {
        for (i = 0; i < _buffer_num; i++) {
            fi[i].time = time;
        }
    }

    return ret;
}

JNIEXPORT jint JNICALL
Java_org_helllabs_android_xmp_Xmp_time(JNIEnv *env, jobject obj) {
    return _playing ? fi[_before].time : -1;
}

JNIEXPORT jint JNICALL
Java_org_helllabs_android_xmp_Xmp_mute(JNIEnv *env, jobject obj, jint chn, jint status) {
    return xmp_channel_mute(ctx, chn, status);
}

JNIEXPORT void JNICALL
Java_org_helllabs_android_xmp_Xmp_getInfo(JNIEnv *env, jobject obj, jintArray values) {
    int v[7];

    lock()
    if (_playing) {
        v[0] = fi[_before].pos /*& 0xff*/;
        v[1] = fi[_before].pattern /*& 0xff*/;
        v[2] = fi[_before].row /*& 0xff*/;
        v[3] = fi[_before].num_rows /*& 0xff*/;
        v[4] = fi[_before].frame /*& 0xff*/;
        v[5] = fi[_before].speed /*& 0xff*/;
        v[6] = fi[_before].bpm /*& 0xff*/;

        (*env).SetIntArrayRegion(values, 0, 7, v);
    }
}

JNIEXPORT void JNICALL
Java_org_helllabs_android_xmp_Xmp_setPlayer(JNIEnv *env, jobject obj, jint parm, jint val) {
    xmp_set_player(ctx, parm, val);
}

JNIEXPORT jint JNICALL
Java_org_helllabs_android_xmp_Xmp_getPlayer(JNIEnv *env, jobject obj, jint parm) {
    return xmp_get_player(ctx, parm);
}

JNIEXPORT jint JNICALL
Java_org_helllabs_android_xmp_Xmp_getLoopCount(JNIEnv *env, jobject obj) {
    return fi[_before].loop_count;
}

JNIEXPORT void JNICALL
Java_org_helllabs_android_xmp_Xmp_getModVars(JNIEnv *env, jobject obj, jintArray vars) {
    // seq_duration, length, pattern, channel, instruments, sample, num_sequences, _sequence
    int v[8];

    lock()

    if (!_mod_is_loaded) {
        return;
    }

    v[0] = mi.seq_data[_sequence].duration;
    v[1] = mi.mod->len;
    v[2] = mi.mod->pat;
    v[3] = mi.mod->chn;
    v[4] = mi.mod->ins;
    v[5] = mi.mod->smp;
    v[6] = mi.num_sequences;
    v[7] = _sequence;

    (*env).SetIntArrayRegion(vars, 0, 8, v);
}

JNIEXPORT jstring JNICALL
Java_org_helllabs_android_xmp_Xmp_getVersion(JNIEnv *env, jobject obj) {
    return (*env).NewStringUTF(xmp_version);
}

JNIEXPORT jobjectArray JNICALL
Java_org_helllabs_android_xmp_Xmp_getFormats(JNIEnv *env, jobject obj) {
    jstring s;
    jclass stringClass;
    jobjectArray stringArray;
    int i, num;
    char **list;
    char buf[80];

    list = (char **) xmp_get_format_list();
    for (num = 0; list[num] != nullptr; num++);

    stringClass = (*env).FindClass("java/lang/String");
    if (stringClass == nullptr)
        return nullptr;

    stringArray = (*env).NewObjectArray(num, stringClass, nullptr);
    if (stringArray == nullptr)
        return nullptr;

    for (i = 0; i < num; i++) {
        s = (*env).NewStringUTF(list[i]);
        (*env).SetObjectArrayElement(stringArray, i, s);
        (*env).DeleteLocalRef(s);
    }

    return stringArray;
}

JNIEXPORT jstring JNICALL
Java_org_helllabs_android_xmp_Xmp_getModName(JNIEnv *env, jobject obj) {
    const char *s = _mod_is_loaded ? mi.mod->name : "";
    return (*env).NewStringUTF(s);
}

JNIEXPORT jstring JNICALL
Java_org_helllabs_android_xmp_Xmp_getModType(JNIEnv *env, jobject obj) {
    const char *s = _mod_is_loaded ? mi.mod->type : "";
    return (*env).NewStringUTF(s);
}

JNIEXPORT jstring JNICALL
Java_org_helllabs_android_xmp_Xmp_getComment(JNIEnv *env, jobject obj) {
    if (mi.comment)
        return (*env).NewStringUTF(mi.comment);
    else
        return nullptr;
}

JNIEXPORT jobjectArray JNICALL
Java_org_helllabs_android_xmp_Xmp_getInstruments(JNIEnv *env, jobject obj) {
    jstring s;
    jclass stringClass;
    jobjectArray stringArray;
    int i;
    char buf[80];
    int ins;

    if (!_mod_is_loaded)
        return nullptr;

    stringClass = (*env).FindClass("java/lang/String");
    if (stringClass == nullptr)
        return nullptr;

    stringArray = (*env).NewObjectArray(mi.mod->ins, stringClass, nullptr);
    if (stringArray == nullptr)
        return nullptr;

    for (i = 0; i < mi.mod->ins; i++) {
        snprintf(buf, 80, "%02X %s", i + 1, mi.mod->xxi[i].name);
        s = (*env).NewStringUTF(buf);
        (*env).SetObjectArrayElement(stringArray, i, s);
        (*env).DeleteLocalRef(s);
    }

    return stringArray;
}

JNIEXPORT void JNICALL
Java_org_helllabs_android_xmp_Xmp_getChannelData(JNIEnv *env, jobject obj, jintArray vol,
                                                 jintArray finalvol,
                                                 jintArray pan, jintArray ins,
                                                 jintArray key, jintArray period) {
    struct xmp_subinstrument *sub;
    int chn = mi.mod->chn;
    int i;

    lock();

    if (!_mod_is_loaded || !_playing) {
        return;
    }

    for (i = 0; i < chn; i++) {
        struct xmp_channel_info *ci = &fi[_before].channel_info[i];

        if (ci->event.vol > 0) {
            _hold_vol[i] = ci->event.vol * 0x40 / mi.vol_base;
        }

        _cur_vol[i] -= _decay;
        if (_cur_vol[i] < 0) {
            _cur_vol[i] = 0;
        }

        if (ci->event.note > 0 && ci->event.note <= 0x80) {
            _key[i] = ci->event.note - 1;
            _last_key[i] = _key[i];
            sub = get_subinstrument(ci->instrument, _key[i]);
            if (sub != nullptr) {
                _cur_vol[i] = sub->vol * 0x40 / mi.vol_base;
            }
        } else {
            _key[i] = -1;
        }

        if (ci->event.vol > 0) {
            _key[i] = _last_key[i];
            _cur_vol[i] = ci->event.vol * 0x40 / mi.vol_base;
        }

        _ins[i] = (signed char) ci->instrument;
        _finalvol[i] = ci->volume;
        _pan[i] = ci->pan;
        _period[i] = ci->period >> 8;
    }

    (*env).SetIntArrayRegion(vol, 0, chn, _cur_vol);
    (*env).SetIntArrayRegion(finalvol, 0, chn, _finalvol);
    (*env).SetIntArrayRegion(pan, 0, chn, _pan);
    (*env).SetIntArrayRegion(ins, 0, chn, _ins);
    (*env).SetIntArrayRegion(key, 0, chn, _key);
    (*env).SetIntArrayRegion(period, 0, chn, _period);
}

JNIEXPORT void JNICALL
Java_org_helllabs_android_xmp_Xmp_getPatternRow(JNIEnv *env, jobject obj, jint pat, jint row,
                                                jbyteArray rowNotes, jbyteArray rowInstruments,
                                                jintArray rowFxType, jintArray rowFxParm) {
    struct xmp_pattern *xxp;
    unsigned char row_note[XMP_MAX_CHANNELS];
    unsigned char row_ins[XMP_MAX_CHANNELS];
    unsigned int row_fxt[XMP_MAX_CHANNELS];
    unsigned int row_fxp[XMP_MAX_CHANNELS];
    int chn;
    int i;

    if (!_mod_is_loaded)
        return;

    if (pat > mi.mod->pat || row > mi.mod->xxp[pat]->rows)
        return;

    xxp = mi.mod->xxp[pat];
    chn = mi.mod->chn;

    for (i = 0; i < chn; i++) {
        struct xmp_track *xxt = mi.mod->xxt[xxp->index[i]];
        struct xmp_event *e = &xxt->event[row];

        row_note[i] = e->note;
        row_ins[i] = e->ins;

        // Get the Effect or Secondary Effect type
        if (e->fxt > 0) {
            row_fxt[i] = e->fxt;
            row_fxp[i] = e->fxp;
        } else if (e->f2t > 0) {
            row_fxt[i] = e->f2t;
            row_fxp[i] = e->f2p;
        } else {
            row_fxt[i] = -1;
            row_fxp[i] = -1;
        }
    }

    (*env).SetByteArrayRegion(rowNotes, 0, chn, reinterpret_cast<const jbyte *>(row_note));
    (*env).SetByteArrayRegion(rowInstruments, 0, chn, reinterpret_cast<const jbyte *>(row_ins));
    (*env).SetIntArrayRegion(rowFxType, 0, chn, reinterpret_cast<const jint *>(row_fxt));
    (*env).SetIntArrayRegion(rowFxParm, 0, chn, reinterpret_cast<const jint *>(row_fxp));
}

JNIEXPORT void JNICALL
Java_org_helllabs_android_xmp_Xmp_getSampleData(JNIEnv *env, jobject obj, jboolean trigger,
                                                jint ins, jint key,
                                                jint period, jint chn, jint width,
                                                jbyteArray buffer) {
    struct xmp_subinstrument *sub;
    struct xmp_sample *xxs;
    int i, pos, transient_size;
    int limit;
    int step, len, lps, lpe;

    lock()

    if (!_mod_is_loaded)
        goto err;

    if (width > MAX_BUFFER_SIZE) {
        width = MAX_BUFFER_SIZE;
    }

    if (period == 0) {
        goto err;
    }

    if (ins < 0 || ins > mi.mod->ins || key > 0x80) {
        goto err;
    }

    sub = get_subinstrument(ins, key);
    if (sub == nullptr || sub->sid < 0 || sub->sid >= mi.mod->smp) {
        goto err;
    }

    xxs = &mi.mod->xxs[sub->sid];
    if (xxs == nullptr || xxs->flg & XMP_SAMPLE_SYNTH || xxs->len == 0) {
        goto err;
    }

    step = (PERIOD_BASE << 4) / period;
    len = xxs->len << 5; // Sample Length
    lps = xxs->lps << 5; // Loop Start
    lpe = xxs->lpe << 5; // Loop End

    pos = _pos[chn];

    /* In case of new keypress, reset sample */
    if (trigger == JNI_TRUE || (pos >> 5) >= xxs->len) {
        pos = 0;
    }

    /* Limit is the buffer size or the remaining transient size */
    if (step == 0) {
        transient_size = 0;
    } else if (xxs->flg & XMP_SAMPLE_LOOP) {
        transient_size = (lps - pos) / step;
    } else {
        transient_size = (len - pos) / step;
    }

    if (transient_size < 0) {
        transient_size = 0;
    }

    limit = width;
    if (limit > transient_size) {
        limit = transient_size;
    }

    if (xxs->flg & XMP_SAMPLE_16BIT) {
        /* transient */
        for (i = 0; i < limit; i++) {
            _buffer[i] = ((short *) xxs->data)[pos >> 5] >> 8;
            pos += step;
        }

        /* loop */
        if (xxs->flg & XMP_SAMPLE_LOOP) {
            for (i = limit; i < width; i++) {
                _buffer[i] = ((short *) xxs->data)[pos >> 5] >> 8;
                pos += step;
                if (pos >= lpe)
                    pos = lps + pos - lpe;
                if (pos >= lpe)        /* avoid division */
                    pos = lps;
            }
        } else {
            for (i = limit; i < width; i++) {
                _buffer[i] = 0;
            }
        }
    } else {
        /* transient */
        for (i = 0; i < limit; i++) {
            _buffer[i] = xxs->data[pos >> 5];
            pos += step;
        }

        /* loop */
        if (xxs->flg & XMP_SAMPLE_LOOP) {
            for (i = limit; i < width; i++) {
                _buffer[i] = xxs->data[pos >> 5];
                pos += step;
                if (pos >= lpe)
                    pos = lps + pos - lpe;
                if (pos >= lpe)        /* avoid division */
                    pos = lps;
            }
        } else {
            for (i = limit; i < width; i++) {
                _buffer[i] = 0;
            }
        }
    }

    _pos[chn] = pos;

    (*env).SetByteArrayRegion(buffer, 0, width, reinterpret_cast<const jbyte *>(_buffer));

    return;

    err:
    memset(_buffer, 0, width);
    (*env).SetByteArrayRegion(buffer, 0, width, reinterpret_cast<const jbyte *>(_buffer));
}

JNIEXPORT jboolean JNICALL
Java_org_helllabs_android_xmp_Xmp_setSequence(JNIEnv *env, jobject obj, jint seq) {
    if (seq >= mi.num_sequences)
        return JNI_FALSE;

    if (mi.seq_data[_sequence].duration <= 0)
        return JNI_FALSE;

    if (_sequence == seq)
        return JNI_FALSE;

    _sequence = seq;
    _loop_count = 0;

    xmp_set_position(ctx, mi.seq_data[_sequence].entry_point);
    xmp_play_buffer(ctx, nullptr, 0, 0);

    return JNI_TRUE;
}

JNIEXPORT void JNICALL
Java_org_helllabs_android_xmp_Xmp_getSeqVars(JNIEnv *env, jobject obj, jintArray vars) {
    int i, num, v[16];

    if (!_mod_is_loaded)
        return;

    num = mi.num_sequences;
    if (num > 16) {
        num = 16;
    }

    for (i = 0; i < num; i++) {
        v[i] = mi.seq_data[i].duration;
    }

    (*env).SetIntArrayRegion(vars, 0, num, v);
}

JNIEXPORT jint JNICALL
Java_org_helllabs_android_xmp_Xmp_getVolume(JNIEnv *env, jobject obj) {
    return OpenSL::get_volume();
}

JNIEXPORT jint JNICALL
Java_org_helllabs_android_xmp_Xmp_setVolume(JNIEnv *env, jobject obj, jint vol) {
    return OpenSL::set_volume(vol);
}
}
