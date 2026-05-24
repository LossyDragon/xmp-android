/*
 * Modernized C++17 JNI interface for libxmp
 * For a full JNI interface, check the libxmp Java API at:
 * https://github.com/cmatsuoka/libxmp-java or https://github.com/TheEssem/libxmp-java
 */

extern "C" {
#include "xmp.h"
#include "common.h"
}

#include "audio.h"
#include <android/log.h>
#include <jni.h>

#include <algorithm>  // std::min, std::max, std::fill
#include <array>      // std::array
#include <cstdio>     // snprintf, FILE*, fclose
#include <memory>     // std::unique_ptr, std::make_unique
#include <mutex>      // std::mutex, std::unique_lock
#include <sys/stat.h> // fstat, struct stat
#include <unistd.h>   // gettid, pid_t
#include <vector>     // std::vector

namespace {
  constexpr int MAX_BUFFER_SIZE = 256;
  constexpr int PERIOD_BASE = 13696;
  constexpr int BUFFER_TIME_MS = 40;
  constexpr int MIN_BUFFER_NUM = 3;

  constexpr const char* TAG = "Xmp Mod Player jni";
}

#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

#define JNI_FUNCTION(name) Java_org_helllabs_libxmp_Xmp_##name

namespace {

  inline pid_t get_thread_id() {
    return gettid();
  }

  // State management class
  class XmpPlayerState {
  public:
    static XmpPlayerState& instance() {
      static XmpPlayerState instance;
      return instance;
    }

    XmpPlayerState(const XmpPlayerState&) = delete;

    XmpPlayerState& operator=(const XmpPlayerState&) = delete;

    // Accessors with lock guards
    std::unique_lock<std::mutex> lock() {
      return std::unique_lock<std::mutex>(mutex_);
    }

    bool isInitialized() const {
      return initialized_;
    }

    void setInitialized(bool val) {
      initialized_ = val;
    }

    bool isModuleLoaded() const {
      return mod_is_loaded_;
    }

    void setModuleLoaded(bool val) {
      mod_is_loaded_ = val;
    }

    bool isPlaying() const {
      return playing_;
    }

    void setPlaying(bool val) {
      playing_ = val;
    }

    int getActualRate() const {
      return actual_rate_;
    }

    void setActualRate(int rate) {
      actual_rate_ = rate;
    }

    int getBufferNum() const {
      return buffer_num_;
    }

    void setBufferNum(int num) {
      buffer_num_ = num;
    }

    int getLoopCount() const {
      return loop_count_;
    }

    void setLoopCount(int count) {
      loop_count_ = count;
    }

    int getSequence() const {
      return sequence_;
    }

    void setSequence(int seq) {
      sequence_ = seq;
    }

    int getDecay() const {
      return decay_;
    }

    xmp_context getContext() const {
      return ctx_;
    }

    void setContext(xmp_context ctx) {
      ctx_ = ctx;
    }

    xmp_module_info& getModuleInfo() {
      return mi_;
    }

    std::array<int, XMP_MAX_CHANNELS>& getCurrentVolume() {
      return cur_vol_;
    }

    std::array<int, XMP_MAX_CHANNELS>& getFinalVolume() {
      return final_vol_;
    }

    std::array<int, XMP_MAX_CHANNELS>& getHoldVolume() {
      return hold_vol_;
    }

    std::array<int, XMP_MAX_CHANNELS>& getInstruments() {
      return ins_;
    }

    std::array<int, XMP_MAX_CHANNELS>& getKeys() {
      return key_;
    }

    std::array<int, XMP_MAX_CHANNELS>& getLastKeys() {
      return last_key_;
    }

    std::array<int, XMP_MAX_CHANNELS>& getPan() {
      return pan_;
    }

    std::array<int, XMP_MAX_CHANNELS>& getPeriod() {
      return period_;
    }

    std::array<int, XMP_MAX_CHANNELS>& getPosition() {
      return pos_;
    }

    void allocateFrameInfo(int count) {
      frame_info_ = std::make_unique<xmp_frame_info[]>(count);
    }

    void freeFrameInfo() {
      frame_info_.reset();
    }

    xmp_frame_info* getFrameInfo() {
      return frame_info_.get();
    }

    int getBefore() const {
      return before_;
    }

    void setBefore(int val) {
      before_ = val;
    }

    int getNow() const {
      return now_;
    }

    void setNow(int val) {
      now_ = val;
    }

    void incrementBefore() {
      before_ = (before_ + 1) % buffer_num_;
    }

  private:
    XmpPlayerState() = default;

    ~XmpPlayerState() = default;

    std::mutex mutex_;
    bool initialized_ = false;
    bool mod_is_loaded_ = false;
    bool playing_ = false;
    int actual_rate_ = 0;
    int buffer_num_ = 0;
    int loop_count_ = 0;
    int sequence_ = 0;
    int decay_ = 4;
    int before_ = 0;
    int now_ = 0;

    xmp_context ctx_ = nullptr;
    xmp_module_info mi_{};
    std::unique_ptr<xmp_frame_info[]> frame_info_;

    std::array<int, XMP_MAX_CHANNELS> cur_vol_{};
    std::array<int, XMP_MAX_CHANNELS> final_vol_{};
    std::array<int, XMP_MAX_CHANNELS> hold_vol_{};
    std::array<int, XMP_MAX_CHANNELS> ins_{};
    std::array<int, XMP_MAX_CHANNELS> key_{};
    std::array<int, XMP_MAX_CHANNELS> last_key_{};
    std::array<int, XMP_MAX_CHANNELS> pan_{};
    std::array<int, XMP_MAX_CHANNELS> period_{};
    std::array<int, XMP_MAX_CHANNELS> pos_{};
  };

  // Field ID cache structures using modern initialization
  struct ModInfoIDs {
    jfieldID name = nullptr;
    jfieldID type = nullptr;
  };

  struct ChannelVarsIDs {
    jfieldID volumes = nullptr;
    jfieldID finalVols = nullptr;
    jfieldID pans = nullptr;
    jfieldID instruments = nullptr;
    jfieldID keys = nullptr;
    jfieldID periods = nullptr;
    jfieldID holdVols = nullptr;
  };

  struct ModVarsIDs {
    jfieldID currentSequence = nullptr;
    jfieldID lengthInPatterns = nullptr;
    jfieldID numChannels = nullptr;
    jfieldID numInstruments = nullptr;
    jfieldID numPatterns = nullptr;
    jfieldID numSamples = nullptr;
    jfieldID numSequence = nullptr;
    jfieldID seqDuration = nullptr;
  };

  struct FrameInfoIDs {
    jfieldID posField = nullptr;
    jfieldID patternField = nullptr;
    jfieldID rowField = nullptr;
    jfieldID numRowsField = nullptr;
    jfieldID frameField = nullptr;
    jfieldID speedField = nullptr;
    jfieldID bpmField = nullptr;
  };

  // Global field ID caches
  ModInfoIDs g_modInfoIDs;
  ChannelVarsIDs g_channelVarsIDs;
  ModVarsIDs g_modVarsIDs;
  FrameInfoIDs g_frameInfoIDs;

  // Field ID caching functions
  void cacheModInfoIDs(JNIEnv* env) {
    if (jclass cls = env->FindClass("org/helllabs/libxmp/model/ModInfo")) {
      g_modInfoIDs.name = env->GetFieldID(cls, "name", "Ljava/lang/String;");
      g_modInfoIDs.type = env->GetFieldID(cls, "type", "Ljava/lang/String;");
      env->DeleteLocalRef(cls);
    }
  }

  void cacheChannelVarsIDs(JNIEnv* env) {
    if (jclass cls = env->FindClass("org/helllabs/libxmp/model/ChannelInfo")) {
      g_channelVarsIDs.volumes = env->GetFieldID(cls, "volumes", "[I");
      g_channelVarsIDs.finalVols = env->GetFieldID(cls, "finalVols", "[I");
      g_channelVarsIDs.pans = env->GetFieldID(cls, "pans", "[I");
      g_channelVarsIDs.instruments = env->GetFieldID(cls, "instruments", "[I");
      g_channelVarsIDs.keys = env->GetFieldID(cls, "keys", "[I");
      g_channelVarsIDs.periods = env->GetFieldID(cls, "periods", "[I");
      g_channelVarsIDs.holdVols = env->GetFieldID(cls, "holdVols", "[I");
      env->DeleteLocalRef(cls);
    }
  }

  void cacheModVarsIDs(JNIEnv* env) {
    if (jclass cls = env->FindClass("org/helllabs/libxmp/model/ModVars")) {
      g_modVarsIDs.currentSequence = env->GetFieldID(cls, "currentSequence", "I");
      g_modVarsIDs.lengthInPatterns = env->GetFieldID(cls, "lengthInPatterns", "I");
      g_modVarsIDs.numChannels = env->GetFieldID(cls, "numChannels", "I");
      g_modVarsIDs.numInstruments = env->GetFieldID(cls, "numInstruments", "I");
      g_modVarsIDs.numPatterns = env->GetFieldID(cls, "numPatterns", "I");
      g_modVarsIDs.numSamples = env->GetFieldID(cls, "numSamples", "I");
      g_modVarsIDs.numSequence = env->GetFieldID(cls, "numSequence", "I");
      g_modVarsIDs.seqDuration = env->GetFieldID(cls, "seqDuration", "I");
      env->DeleteLocalRef(cls);
    }
  }

  void cacheFrameInfoIDs(JNIEnv* env) {
    if (jclass cls = env->FindClass("org/helllabs/libxmp/model/FrameInfo")) {
      g_frameInfoIDs.posField = env->GetFieldID(cls, "pos", "I");
      g_frameInfoIDs.patternField = env->GetFieldID(cls, "pattern", "I");
      g_frameInfoIDs.rowField = env->GetFieldID(cls, "row", "I");
      g_frameInfoIDs.numRowsField = env->GetFieldID(cls, "numRows", "I");
      g_frameInfoIDs.frameField = env->GetFieldID(cls, "frame", "I");
      g_frameInfoIDs.speedField = env->GetFieldID(cls, "speed", "I");
      g_frameInfoIDs.bpmField = env->GetFieldID(cls, "bpm", "I");
      env->DeleteLocalRef(cls);
    }
  }

  // RAII wrapper for FILE*
  class FileHandle {
  public:
    explicit FileHandle(FILE* f) : file_(f) {}

    ~FileHandle() {
      if (file_) {
        fclose(file_);
      }
    }

    FileHandle(const FileHandle&) = delete;

    FileHandle& operator=(const FileHandle&) = delete;

    FILE* get() const {
      return file_;
    }

    explicit operator bool() const {
      return file_ != nullptr;
    }

  private:
    FILE* file_;
  };

  xmp_subinstrument* getSubinstrument(const xmp_module_info& mi, int ins, int key) {
    if (ins < 0 || ins >= mi.mod->ins || key < 0 || key >= XMP_MAX_KEYS) {
      return nullptr;
    }

    if (mi.mod->xxi[ins].map[key].ins == 0xff) {
      return nullptr;
    }

    int mapped = mi.mod->xxi[ins].map[key].ins;

    // Additional safety check
    if (mapped < 0 || mapped >= mi.mod->xxi[ins].nsm) {
      return nullptr;
    }

    return &mi.mod->xxi[ins].sub[mapped];
  }

}

extern "C" {

int play_buffer(void* buffer, int size, int looped) {
  XmpPlayerState& state = XmpPlayerState::instance();
  std::unique_lock<std::mutex> lock = state.lock();

  if (!state.isPlaying()) {
    return -XMP_END;
  }

  int num_loop = looped ? 0 : state.getLoopCount() + 1;
  int ret = xmp_play_buffer(state.getContext(), buffer, size, num_loop);

  xmp_frame_info* fi = state.getFrameInfo();
  xmp_get_frame_info(state.getContext(), &fi[state.getNow()]);

  state.incrementBefore();
  state.setNow((state.getBefore() + state.getBufferNum() - 1) % state.getBufferNum());
  state.setLoopCount(fi[state.getNow()].loop_count);

  return ret;
}

JNIEXPORT jboolean JNICALL JNI_FUNCTION(init)(JNIEnv* env, jobject obj, jint rate, jint ms) {
  LOGI("init() called - rate: %d, ms: %d, tid: %d", rate, ms, get_thread_id());

  XmpPlayerState& state = XmpPlayerState::instance();

  xmp_context ctx = xmp_create_context();
  if (!ctx) {
    LOGE("init() failed - could not create context");
    return JNI_FALSE;
  }

  int actual_rate = open_audio(rate, ms);
  if (actual_rate < 0) {
    LOGE("init() failed - could not open audio");
    xmp_free_context(ctx);
    return JNI_FALSE;
  }

  state.setContext(ctx);
  state.setActualRate(actual_rate);

  // Calculate buffer_num
  int buffer_num = ms / BUFFER_TIME_MS;
  if (buffer_num < MIN_BUFFER_NUM) {
    buffer_num = MIN_BUFFER_NUM;
  }
  state.setBufferNum(buffer_num);

  if (actual_rate != rate) {
    LOGI("init() - sample rate adjusted: requested=%d, actual=%d", rate, actual_rate);
  }

  // Cache field IDs
  cacheChannelVarsIDs(env);
  cacheFrameInfoIDs(env);
  cacheModInfoIDs(env);
  cacheModVarsIDs(env);
  // cacheSequenceVarsIDs(env);

  state.setInitialized(true);
  LOGI("init() completed successfully - actual_rate=%d", actual_rate);

  return JNI_TRUE;
}

JNIEXPORT jint JNICALL JNI_FUNCTION(deinit)(JNIEnv* env, jobject obj) {
  LOGI("deinit() called - tid: %d", get_thread_id());

  XmpPlayerState& state = XmpPlayerState::instance();

  {
    std::unique_lock<std::mutex> lock = state.lock();
    LOGD("deinit() - acquired lock");
    state.setInitialized(false);
  }
  LOGD("deinit() - released lock, set initialized=false");

  xmp_free_context(state.getContext());
  LOGD("deinit() - freed context");

  close_audio();
  LOGI("deinit() completed - tid: %d", get_thread_id());

  return 0;
}

JNIEXPORT jint JNICALL JNI_FUNCTION(loadModuleFd)(JNIEnv* env, jobject obj, jint fd, jobject modInfo) {
  LOGI("loadModuleFd() called - fd: %d, tid: %d", fd, get_thread_id());

  FileHandle file(fdopen(fd, "rb"));
  if (!file) {
    LOGE("loadModuleFd() - fdopen failed: %s", strerror(errno));
    return -1;
  }

  xmp_test_info ti{};
  int res = xmp_test_module_from_file(file.get(), &ti);

  if (res != 0) {
    LOGW("loadModuleFd() - test failed: %d", res);
    return -2;
  }

  // Populate modInfo
  if (!g_modInfoIDs.name || !g_modInfoIDs.type) {
    cacheModInfoIDs(env);
  }

  jstring name = env->NewStringUTF(ti.name);
  jstring type = env->NewStringUTF(ti.type);
  env->SetObjectField(modInfo, g_modInfoIDs.name, name);
  env->SetObjectField(modInfo, g_modInfoIDs.type, type);

  rewind(file.get());

  struct stat statbuf{};
  if (fstat(fd, &statbuf) != 0) {
    LOGE("loadModuleFd() - fstat failed: %s", strerror(errno));
    return -3;
  }

  XmpPlayerState& state = XmpPlayerState::instance();
  res = xmp_load_module_from_file(state.getContext(), file.get(), static_cast<off_t>(statbuf.st_size));

  if (res == 0) {
    xmp_get_module_info(state.getContext(), &state.getModuleInfo());
    state.getPosition().fill(0);
    state.setSequence(0);
    state.setModuleLoaded(true);
    LOGI("loadModuleFd() - loaded: %s", ti.name);
  }

  return res;
}

JNIEXPORT jboolean JNICALL JNI_FUNCTION(testModuleFd)(JNIEnv* env, jobject obj, jint fd, jobject modInfo) {
  LOGI("testModuleFd() called - fd: %d, tid: %d", fd, get_thread_id());

  FileHandle file(fdopen(fd, "rb"));
  if (!file) {
    LOGE("testModuleFd() - fdopen failed for fd %d: %s", fd, strerror(errno));
    return JNI_FALSE;
  }

  xmp_test_info ti{};
  int res = xmp_test_module_from_file(file.get(), &ti);

  LOGD("testModuleFd() - test result: %d", res);

  // Ensure field IDs are cached
  if (!g_modInfoIDs.name || !g_modInfoIDs.type) {
    LOGW("testModuleFd() - field IDs not cached, caching now");
    cacheModInfoIDs(env);
  }

  if (res == 0) {
    LOGI("testModuleFd() - valid module: '%s' (type: %s)", ti.name, ti.type);

    jstring name = env->NewStringUTF(ti.name);
    jstring type = env->NewStringUTF(ti.type);

    if (!name || !type) {
      LOGE("testModuleFd() - failed to create Java strings");
      return JNI_FALSE;
    }

    env->SetObjectField(modInfo, g_modInfoIDs.name, name);
    env->SetObjectField(modInfo, g_modInfoIDs.type, type);

    LOGD("testModuleFd() - successfully populated modInfo");
  } else {
      char path[PATH_MAX];
      ssize_t len = readlink(("/proc/self/fd/" + std::to_string(fd)).c_str(), path, sizeof(path) - 1);
      if (len > 0) path[len] = '\0';
      LOGW("testModuleFd() - not a valid module, error: %d, file: %s", res, len > 0 ? path : "unknown");
  }

  return res == 0 ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jint JNICALL JNI_FUNCTION(releaseModule)(JNIEnv* env, jobject obj) {
  XmpPlayerState& state = XmpPlayerState::instance();
  std::unique_lock<std::mutex> lock = state.lock();

  if (state.isModuleLoaded()) {
    state.setModuleLoaded(false);
    xmp_release_module(state.getContext());
  }

  return 0;
}

JNIEXPORT jint JNICALL JNI_FUNCTION(startPlayer)(JNIEnv* env, jobject obj, jint rate) {
  XmpPlayerState& state = XmpPlayerState::instance();
  int use_rate = state.getActualRate() > 0 ? state.getActualRate() : rate;

  LOGI("startPlayer() - requested: %d, using: %d, tid: %d", rate, use_rate, get_thread_id());

  std::unique_lock<std::mutex> lock = state.lock();
  LOGD("startPlayer() - acquired lock");

  state.allocateFrameInfo(state.getBufferNum());
  if (!state.getFrameInfo()) {
    LOGE("startPlayer() - failed to allocate frame info");
    return -101;
  }

  state.getKeys().fill(-1);
  state.getLastKeys().fill(-1);

  state.setBefore(0);
  state.setNow(0);
  state.setLoopCount(0);
  state.setPlaying(true);

  int ret = xmp_start_player(state.getContext(), use_rate, 0);

  LOGI("startPlayer() completed - result: %d, tid: %d", ret, get_thread_id());
  return ret;
}

JNIEXPORT jint JNICALL JNI_FUNCTION(endPlayer)(JNIEnv* env, jobject obj) {
  LOGI("endPlayer() called - tid: %d", get_thread_id());

  XmpPlayerState& state = XmpPlayerState::instance();
  std::unique_lock<std::mutex> lock = state.lock();
  LOGD("endPlayer() - acquired lock");

  if (state.isPlaying()) {
    state.setPlaying(false);
    LOGD("endPlayer() - stopping player");
    xmp_end_player(state.getContext());
    state.freeFrameInfo();
    LOGD("endPlayer() - freed frame info");
  }

  LOGI("endPlayer() completed - tid: %d", get_thread_id());
  return 0;
}

JNIEXPORT jint JNICALL JNI_FUNCTION(playAudio)(JNIEnv* env, jobject obj) {
  return play_audio();
}

JNIEXPORT void JNICALL JNI_FUNCTION(dropAudio)(JNIEnv* env, jobject obj) {
  drop_audio();
}

JNIEXPORT jboolean JNICALL JNI_FUNCTION(stopAudio)(JNIEnv* env, jobject obj) {
  return stop_audio() == 0 ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL JNI_FUNCTION(restartAudio)(JNIEnv* env, jobject obj) {
  return restart_audio() == 0 ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL JNI_FUNCTION(hasFreeBuffer)(JNIEnv* env, jobject obj) {
  return has_free_buffer() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jint JNICALL JNI_FUNCTION(fillBuffer)(JNIEnv* env, jobject obj, jboolean looped) {
  return fill_buffer(looped);
}

JNIEXPORT jint JNICALL JNI_FUNCTION(nextPosition)(JNIEnv* env, jobject obj) {
  XmpPlayerState& state = XmpPlayerState::instance();
  return xmp_next_position(state.getContext());
}

JNIEXPORT jint JNICALL JNI_FUNCTION(prevPosition)(JNIEnv* env, jobject obj) {
  XmpPlayerState& state = XmpPlayerState::instance();
  return xmp_prev_position(state.getContext());
}

JNIEXPORT jint JNICALL JNI_FUNCTION(setPosition)(JNIEnv* env, jobject obj, jint n) {
  XmpPlayerState& state = XmpPlayerState::instance();
  return xmp_set_position(state.getContext(), n);
}

JNIEXPORT jint JNICALL JNI_FUNCTION(stopModule)(JNIEnv* env, jobject obj) {
  XmpPlayerState& state = XmpPlayerState::instance();
  xmp_stop_module(state.getContext());
  return 0;
}

JNIEXPORT jint JNICALL JNI_FUNCTION(restartModule)(JNIEnv* env, jobject obj) {
  XmpPlayerState& state = XmpPlayerState::instance();
  xmp_restart_module(state.getContext());
  return 0;
}

JNIEXPORT jint JNICALL JNI_FUNCTION(seek)(JNIEnv* env, jobject obj, jint time) {
  XmpPlayerState& state = XmpPlayerState::instance();
  std::unique_lock<std::mutex> lock = state.lock();

  int ret = xmp_seek_time(state.getContext(), time);

  if (state.isPlaying()) {
    xmp_frame_info* fi = state.getFrameInfo();
    for (int i = 0; i < state.getBufferNum(); i++) {
      fi[i].time = time;
    }
  }

  return ret;
}

JNIEXPORT jint JNICALL JNI_FUNCTION(time)(JNIEnv* env, jobject obj) {
  XmpPlayerState& state = XmpPlayerState::instance();
  return state.isPlaying() ? state.getFrameInfo()[state.getBefore()].time : -1;
}

JNIEXPORT jint JNICALL JNI_FUNCTION(mute)(JNIEnv* env, jobject obj, jint chn, jint status) {
  XmpPlayerState& state = XmpPlayerState::instance();
  return xmp_channel_mute(state.getContext(), chn, status);
}

JNIEXPORT void JNICALL JNI_FUNCTION(getInfo)(JNIEnv* env, jobject obj, jobject frameInfo) {
  XmpPlayerState& state = XmpPlayerState::instance();

  if (!state.isModuleLoaded()) return;

  if (!g_frameInfoIDs.posField) {
    cacheFrameInfoIDs(env);
  }

  std::unique_lock<std::mutex> lock = state.lock();

  if (state.isPlaying()) {
    const xmp_frame_info& fi = state.getFrameInfo()[state.getBefore()];
    env->SetIntField(frameInfo, g_frameInfoIDs.posField, fi.pos);
    env->SetIntField(frameInfo, g_frameInfoIDs.patternField, fi.pattern);
    env->SetIntField(frameInfo, g_frameInfoIDs.rowField, fi.row);
    env->SetIntField(frameInfo, g_frameInfoIDs.numRowsField, fi.num_rows);
    env->SetIntField(frameInfo, g_frameInfoIDs.frameField, fi.frame);
    env->SetIntField(frameInfo, g_frameInfoIDs.speedField, fi.speed);
    env->SetIntField(frameInfo, g_frameInfoIDs.bpmField, fi.bpm);
  }
}

JNIEXPORT void JNICALL JNI_FUNCTION(setPlayer)(JNIEnv* env, jobject obj, jint parm, jint value) {
  XmpPlayerState& state = XmpPlayerState::instance();
  xmp_set_player(state.getContext(), parm, value);
}

JNIEXPORT jint JNICALL JNI_FUNCTION(getPlayer)(JNIEnv* env, jobject obj, jint parm) {
  XmpPlayerState& state = XmpPlayerState::instance();
  return xmp_get_player(state.getContext(), parm);
}

JNIEXPORT jint JNICALL JNI_FUNCTION(getLoopCount)(JNIEnv* env, jobject obj) {
  XmpPlayerState& state = XmpPlayerState::instance();
  return state.getFrameInfo()[state.getBefore()].loop_count;
}

JNIEXPORT void JNICALL JNI_FUNCTION(getModVars)(JNIEnv* env, jobject obj, jobject modVars) {
  XmpPlayerState& state = XmpPlayerState::instance();
  int tid = get_thread_id();

  // LOGD("getModVars() called - tid: %d, initialized: %d", tid, state.isInitialized());

  if (!state.isInitialized()) {
    LOGW("getModVars() - early exit: not initialized - tid: %d", tid);
    return;
  }

  // LOGD("getModVars() - attempting lock - tid: %d", tid);
  std::unique_lock<std::mutex> lock = state.lock();
  // LOGD("getModVars() - acquired lock - tid: %d", tid);

  if (!state.isModuleLoaded()) {
    LOGW("getModVars() - module not loaded - tid: %d", tid);
    return;
  }

  if (!g_modVarsIDs.currentSequence) {
    cacheModVarsIDs(env);
  }

  const xmp_module_info& mi = state.getModuleInfo();
  int seq = state.getSequence();

  env->SetIntField(modVars, g_modVarsIDs.seqDuration, mi.seq_data[seq].duration);
  env->SetIntField(modVars, g_modVarsIDs.lengthInPatterns, mi.mod->len);
  env->SetIntField(modVars, g_modVarsIDs.numPatterns, mi.mod->pat);
  env->SetIntField(modVars, g_modVarsIDs.numChannels, mi.mod->chn);
  env->SetIntField(modVars, g_modVarsIDs.numInstruments, mi.mod->ins);
  env->SetIntField(modVars, g_modVarsIDs.numSamples, mi.mod->smp);
  env->SetIntField(modVars, g_modVarsIDs.numSequence, mi.num_sequences);
  env->SetIntField(modVars, g_modVarsIDs.currentSequence, seq);

  // LOGD("getModVars() completed - tid: %d", tid);
}

JNIEXPORT jstring JNICALL JNI_FUNCTION(getVersion)(JNIEnv* env, jobject obj) {
  return env->NewStringUTF(xmp_version);
}

JNIEXPORT jobjectArray JNICALL JNI_FUNCTION(getFormats)(JNIEnv* env, jobject obj) {
  const char* const* list = xmp_get_format_list();

  jclass stringClass = env->FindClass("java/lang/String");
  if (!stringClass) return env->NewObjectArray(0, env->FindClass("java/lang/String"), nullptr);

  int num = 0;
  if (list) {
    while (list[num] != nullptr) {
      ++num;
    }
  }

  jobjectArray stringArray = env->NewObjectArray(num, stringClass, nullptr);
  if (!stringArray) {
    env->DeleteLocalRef(stringClass);
    return env->NewObjectArray(0, stringClass, nullptr);
  }

  for (int i = 0; i < num; i++) {
    jstring s = env->NewStringUTF(list[i]);
    env->SetObjectArrayElement(stringArray, i, s);
    env->DeleteLocalRef(s);
  }

  env->DeleteLocalRef(stringClass);
  return stringArray;
}

JNIEXPORT jstring JNICALL JNI_FUNCTION(getModName)(JNIEnv* env, jobject obj) {
  XmpPlayerState& state = XmpPlayerState::instance();
  const char* name = state.isModuleLoaded() ? state.getModuleInfo().mod->name : "";
  return env->NewStringUTF(name);
}

JNIEXPORT jstring JNICALL JNI_FUNCTION(getModType)(JNIEnv* env, jobject obj) {
  XmpPlayerState& state = XmpPlayerState::instance();
  const char* type = state.isModuleLoaded() ? state.getModuleInfo().mod->type : "";
  return env->NewStringUTF(type);
}

JNIEXPORT jbyteArray JNICALL JNI_FUNCTION(getComment)(JNIEnv* env, jobject obj) {
  XmpPlayerState& state = XmpPlayerState::instance();
  const xmp_module_info& mi = state.getModuleInfo();

  if (!mi.comment) {
    return env->NewByteArray(0);
  }

  size_t length = strlen(mi.comment);
  jbyteArray byteArray = env->NewByteArray(static_cast<jsize>(length));
  env->SetByteArrayRegion(byteArray, 0, static_cast<jsize>(length), reinterpret_cast<const jbyte*>(mi.comment));
  return byteArray;
}

JNIEXPORT jobjectArray JNICALL JNI_FUNCTION(getInstruments)(JNIEnv* env, jobject obj) {
  XmpPlayerState& state = XmpPlayerState::instance();

  jclass stringClass = env->FindClass("java/lang/String");
  if (!stringClass) return nullptr;

  if (!state.isModuleLoaded()) {
    jobjectArray empty = env->NewObjectArray(0, stringClass, nullptr);
    env->DeleteLocalRef(stringClass);
    return empty;
  }

  const xmp_module_info& mi = state.getModuleInfo();

  jobjectArray stringArray = env->NewObjectArray(mi.mod->ins, stringClass, nullptr);
  if (!stringArray) {
    jobjectArray empty = env->NewObjectArray(0, stringClass, nullptr);
    env->DeleteLocalRef(stringClass);
    return empty;
  }

  for (int i = 0; i < mi.mod->ins; i++) {
    std::array<char, 64> buf{};
    snprintf(buf.data(), buf.size(), "%02X %s", i + 1, mi.mod->xxi[i].name);

    jstring s = env->NewStringUTF(buf.data());
    env->SetObjectArrayElement(stringArray, i, s);
    env->DeleteLocalRef(s);
  }

  env->DeleteLocalRef(stringClass);
  return stringArray;
}

JNIEXPORT void JNICALL JNI_FUNCTION(getChannelData)(JNIEnv* env, jobject obj, jobject channelInfo) {
  XmpPlayerState& state = XmpPlayerState::instance();
  int tid = get_thread_id();

  if (!state.isInitialized()) {
    LOGW("getChannelData() - early exit: not initialized - tid: %d", tid);
    return;
  }

  std::unique_lock<std::mutex> lock = state.lock();

  if (!state.isModuleLoaded() || !state.isPlaying()) {
    return;
  }

  if (!g_channelVarsIDs.finalVols) {
    cacheChannelVarsIDs(env);
  }

  const xmp_module_info& mi = state.getModuleInfo();
  int chn = mi.mod->chn;
  const xmp_frame_info& fi = state.getFrameInfo()[state.getBefore()];

  std::array<int, XMP_MAX_CHANNELS>& cur_vol = state.getCurrentVolume();
  std::array<int, XMP_MAX_CHANNELS>& final_vol = state.getFinalVolume();
  std::array<int, XMP_MAX_CHANNELS>& hold_vol = state.getHoldVolume();
  std::array<int, XMP_MAX_CHANNELS>& ins = state.getInstruments();
  std::array<int, XMP_MAX_CHANNELS>& key = state.getKeys();
  std::array<int, XMP_MAX_CHANNELS>& last_key = state.getLastKeys();
  std::array<int, XMP_MAX_CHANNELS>& pan = state.getPan();
  std::array<int, XMP_MAX_CHANNELS>& period = state.getPeriod();

  for (int i = 0; i < chn; i++) {
    const xmp_channel_info& ci = fi.channel_info[i];

    if (ci.event.vol > 0) {
      hold_vol[i] = ci.event.vol * 0x40 / mi.vol_base;
    }

    cur_vol[i] -= state.getDecay();
    cur_vol[i] = std::max(0, cur_vol[i]);

    if (ci.event.note > 0 && ci.event.note <= 0x80) {
      key[i] = ci.event.note - 1;
      last_key[i] = key[i];

      if (xmp_subinstrument* sub = getSubinstrument(mi, ci.instrument, key[i])) {
        cur_vol[i] = sub->vol * 0x40 / mi.vol_base;
      }
    } else {
      key[i] = -1;
    }

    if (ci.event.vol > 0) {
      key[i] = last_key[i];
      cur_vol[i] = ci.event.vol * 0x40 / mi.vol_base;
    }

    ins[i] = static_cast<int>(static_cast<unsigned char>(ci.instrument));
    final_vol[i] = ci.volume;
    pan[i] = ci.pan;
    period[i] = static_cast<int>(ci.period) >> 8;
  }

  auto vol = (jintArray)env->GetObjectField(channelInfo, g_channelVarsIDs.volumes);
  auto finalVols = (jintArray)env->GetObjectField(channelInfo, g_channelVarsIDs.finalVols);
  auto pans = (jintArray)env->GetObjectField(channelInfo, g_channelVarsIDs.pans);
  auto instruments = (jintArray)env->GetObjectField(channelInfo, g_channelVarsIDs.instruments);
  auto keys = (jintArray)env->GetObjectField(channelInfo, g_channelVarsIDs.keys);
  auto periods = (jintArray)env->GetObjectField(channelInfo, g_channelVarsIDs.periods);
  auto holdVols = (jintArray)env->GetObjectField(channelInfo, g_channelVarsIDs.holdVols);

  env->SetIntArrayRegion(vol, 0, chn, cur_vol.data());
  env->SetIntArrayRegion(finalVols, 0, chn, final_vol.data());
  env->SetIntArrayRegion(pans, 0, chn, pan.data());
  env->SetIntArrayRegion(instruments, 0, chn, ins.data());
  env->SetIntArrayRegion(keys, 0, chn, key.data());
  env->SetIntArrayRegion(periods, 0, chn, period.data());
  env->SetIntArrayRegion(holdVols, 0, chn, hold_vol.data());
}

JNIEXPORT void JNICALL JNI_FUNCTION(getPatternRow)(JNIEnv* env, jobject obj, jint pat, jint row, jbyteArray rowNotes, jbyteArray rowInstruments, jbyteArray rowFxType, jbyteArray rowFxParm) {
  XmpPlayerState& state = XmpPlayerState::instance();

  if (!state.isModuleLoaded()) return;

  const xmp_module_info& mi = state.getModuleInfo();

  if (pat >= mi.mod->pat) return;

  const xmp_pattern* xxp = mi.mod->xxp[pat];

  if (!xxp || row >= xxp->rows) return;

  int chn = mi.mod->chn;

  std::array<jbyte, XMP_MAX_CHANNELS> row_note{};
  std::array<jbyte, XMP_MAX_CHANNELS> row_ins{};
  std::array<jbyte, XMP_MAX_CHANNELS> row_fxt{};
  std::array<jbyte, XMP_MAX_CHANNELS> row_fxp{};

  for (int i = 0; i < chn; i++) {
    const xmp_track* xxt = mi.mod->xxt[xxp->index[i]];
    const xmp_event& e = xxt->event[row];

    row_note[i] = static_cast<jbyte>(e.note);
    row_ins[i] = static_cast<jbyte>(e.ins);

    if (e.fxt > 0) { // NOLINT(*-branch-clone)
      row_fxt[i] = static_cast<jbyte>(e.fxt);
      row_fxp[i] = static_cast<jbyte>(e.fxp);
    } else if (e.f2t > 0) {
      row_fxt[i] = static_cast<jbyte>(e.f2t);
      row_fxp[i] = static_cast<jbyte>(e.f2p);
    } else if (e.fxt == 0 && e.fxp > 0) {
      // Likely Arpeggio
      row_fxt[i] = static_cast<jbyte>(e.fxt);
      row_fxp[i] = static_cast<jbyte>(e.fxp);
    } else {
      row_fxt[i] = -1;
      row_fxp[i] = -1;
    }
  }

  env->SetByteArrayRegion(rowNotes, 0, chn, row_note.data());
  env->SetByteArrayRegion(rowInstruments, 0, chn, row_ins.data());
  env->SetByteArrayRegion(rowFxType, 0, chn, row_fxt.data());
  env->SetByteArrayRegion(rowFxParm, 0, chn, row_fxp.data());
}

JNIEXPORT void JNICALL JNI_FUNCTION(getSampleData)(JNIEnv* env, jobject obj, jboolean trigger, jint ins, jint key, jint period, jint chn, jint width, jbyteArray buffer) {
  XmpPlayerState& state = XmpPlayerState::instance();
  std::unique_lock<std::mutex> lock = state.lock();

  std::array<jbyte, MAX_BUFFER_SIZE> sample_buffer{};

  auto populateBuffer = [&](int size) { env->SetByteArrayRegion(buffer, 0, size, sample_buffer.data()); };

  if (!state.isModuleLoaded()) {
    populateBuffer(width);
    return;
  }

  width = std::min(width, MAX_BUFFER_SIZE);

  // Validate channel index
  if (chn < 0 || chn >= XMP_MAX_CHANNELS) {
    populateBuffer(width);
    return;
  }

  // Validate all parameters
  if (period <= 0 || ins < 0 || key < 0 || key >= XMP_MAX_KEYS) {
    populateBuffer(width);
    return;
  }

  const xmp_module_info& mi = state.getModuleInfo();

  if (ins >= mi.mod->ins) {
    populateBuffer(width);
    return;
  }

  xmp_subinstrument* sub = getSubinstrument(mi, ins, key);
  if (!sub || sub->sid < 0 || sub->sid >= mi.mod->smp) {
    populateBuffer(width);
    return;
  }

  const xmp_sample* xxs = &mi.mod->xxs[sub->sid];

  if (xxs->flg & XMP_SAMPLE_SYNTH || xxs->len == 0) {
    populateBuffer(width);
    return;
  }

  int step = (PERIOD_BASE << 4) / period;
  int len = xxs->len << 5;
  int lps = xxs->lps << 5;
  int lpe = xxs->lpe << 5;

  std::array<int, XMP_MAX_CHANNELS>& pos = state.getPosition();
  int current_pos = pos[chn];

  // Reset position on trigger or if out of bounds
  if (trigger == JNI_TRUE || (current_pos >> 5) >= xxs->len) {
    current_pos = 0;
  }

  // Calculate transient size
  int transient_size = 0;
  if (step > 0) {
    if (xxs->flg & XMP_SAMPLE_LOOP) {
      transient_size = (lps - current_pos) / step;
    } else {
      transient_size = (len - current_pos) / step;
    }
  }
  transient_size = std::max(0, transient_size);

  int limit = std::min(width, transient_size);

  // Fill buffer
  if (xxs->flg & XMP_SAMPLE_16BIT) {
    const auto* data = reinterpret_cast<const short*>(xxs->data);

    // Transient
    for (int i = 0; i < limit; i++) {
      sample_buffer[i] = static_cast<jbyte>(data[current_pos >> 5] >> 8);
      current_pos += step;
    }

    // Loop
    if (xxs->flg & XMP_SAMPLE_LOOP) {
      for (int i = limit; i < width; i++) {
        sample_buffer[i] = static_cast<jbyte>(data[current_pos >> 5] >> 8);
        current_pos += step;
        if (current_pos >= lpe) {
          current_pos = lps + current_pos - lpe;
          if (current_pos >= lpe) current_pos = lps;
        }
      }
    } else {
      std::fill(sample_buffer.begin() + limit, sample_buffer.begin() + width, 0);
    }
  } else {
    const auto* data = reinterpret_cast<const jbyte*>(xxs->data);

    // Transient
    for (int i = 0; i < limit; i++) {
      sample_buffer[i] = data[current_pos >> 5];
      current_pos += step;
    }

    // Loop
    if (xxs->flg & XMP_SAMPLE_LOOP) {
      for (int i = limit; i < width; i++) {
        sample_buffer[i] = data[current_pos >> 5];
        current_pos += step;
        if (current_pos >= lpe) {
          current_pos = lps + current_pos - lpe;
          if (current_pos >= lpe) current_pos = lps;
        }
      }
    } else {
      std::fill(sample_buffer.begin() + limit, sample_buffer.begin() + width, 0);
    }
  }

  pos[chn] = current_pos;
  populateBuffer(width);
}

JNIEXPORT jboolean JNICALL JNI_FUNCTION(setSequence)(JNIEnv* env, jobject obj, jint seq) {
  XmpPlayerState& state = XmpPlayerState::instance();
  const xmp_module_info& mi = state.getModuleInfo();

  if (seq >= mi.num_sequences) return JNI_FALSE;
  if (mi.seq_data[state.getSequence()].duration <= 0) return JNI_FALSE;
  if (state.getSequence() == seq) return JNI_FALSE;

  state.setSequence(seq);
  state.setLoopCount(0);

  xmp_set_position(state.getContext(), mi.seq_data[seq].entry_point);
  xmp_play_buffer(state.getContext(), nullptr, 0, 0);

  return JNI_TRUE;
}

JNIEXPORT jint JNICALL JNI_FUNCTION(getMaxSequences)(JNIEnv* env, jobject obj) {
  return MAX_SEQUENCES;
}

JNIEXPORT jintArray JNICALL JNI_FUNCTION(getSeqVars)(JNIEnv* env, jobject obj) {
  XmpPlayerState& state = XmpPlayerState::instance();

  if (!state.isModuleLoaded()) {
    return env->NewIntArray(0);
  }

  const xmp_module_info& mi = state.getModuleInfo();
  int num = mi.num_sequences;

  if (num <= 0) {
    return env->NewIntArray(0);
  }

  jintArray result = env->NewIntArray(num);
  if (!result) {
    return env->NewIntArray(0);
  }

  std::vector<jint> durations(num);
  for (int i = 0; i < num; i++) {
    durations[i] = mi.seq_data[i].duration;
  }
  env->SetIntArrayRegion(result, 0, num, durations.data());

  return result;
}

JNIEXPORT void JNICALL JNI_FUNCTION(setExpectSilence)(JNIEnv* env, jobject obj, jboolean value) {
  set_expect_silence(value == JNI_TRUE ? 1 : 0);
}

JNIEXPORT jint JNICALL JNI_FUNCTION(getVolume)(JNIEnv* env, jobject obj) {
  return get_volume();
}

JNIEXPORT jint JNICALL JNI_FUNCTION(setVolume)(JNIEnv* env, jobject obj, jint vol) {
  return set_volume(vol);
}

JNIEXPORT jobject JNICALL JNI_FUNCTION(getAudioStats)(JNIEnv* env, jobject obj) {
  struct AudioStats stats{};

  jclass statsClass = env->FindClass("org/helllabs/libxmp/model/AudioStats");
  if (!statsClass) return nullptr;

  jmethodID constructor = env->GetMethodID(statsClass, "<init>", "(IIIIIILjava/lang/String;Ljava/lang/String;)V");
  if (!constructor) {
    env->DeleteLocalRef(statsClass);
    return nullptr;
  }

  get_audio_stats(&stats);

  jstring apiStr = env->NewStringUTF(stats.audio_api ? stats.audio_api : "");
  jstring modeStr = env->NewStringUTF(stats.sharing_mode ? stats.sharing_mode : "");

  jobject statsObj =
    env->NewObject(statsClass, constructor, stats.xrun_count, stats.underrun_count, stats.frames_per_burst, stats.buffer_capacity, stats.buffer_size, stats.sample_rate, apiStr, modeStr);

  env->DeleteLocalRef(statsClass);
  return statsObj;
}

} // extern "C"
