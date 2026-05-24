#include "audio.h"
#include <oboe/Oboe.h>
#include <android/log.h>
#include <pthread.h>
#include <stdatomic.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <atomic>

#define TAG "XmpOboe"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

#define BUFFER_TIME 40

#define lock() pthread_mutex_lock(&mutex)
#define unlock() pthread_mutex_unlock(&mutex)

static std::atomic<bool> expect_silence(false);

// Helper to safely load atomic_int
static inline int atomic_load_int(atomic_int* ptr) {
  return atomic_load(ptr);
}

// Helper to safely store atomic_int
static inline void atomic_store_int(atomic_int* ptr, int val) {
  atomic_store(ptr, val);
}

class XmpAudioCallback : public oboe::AudioStreamDataCallback,
                         public oboe::AudioStreamErrorCallback {
public:
  XmpAudioCallback(char* buf, int bufSize, int bufNum, atomic_int* firstFree, atomic_int* lastFree, std::atomic<float>* volScale, std::atomic<int32_t>* underrunCount)
    : buffer(buf), buffer_size(bufSize), buffer_num(bufNum), first_free(firstFree), last_free(lastFree), volume_scale(*volScale), underrun_count_(*underrunCount), buffer_position(0) {}

  oboe::DataCallbackResult onAudioReady(oboe::AudioStream* audioStream, void* audioData, int32_t numFrames) override {
    static int callback_count = 0;
    static int32_t last_xrun_count = 0;

    if (++callback_count >= 1000) {
      callback_count = 0;

      if (audioStream->isXRunCountSupported()) {
        oboe::ResultWithValue<int32_t> xRunResult = audioStream->getXRunCount();
        if (xRunResult) { // Operator bool checks if result is OK
          int32_t current_xruns = xRunResult.value();
          if (current_xruns != last_xrun_count) {
            LOGW("XRuns detected: %d (new: %d)", current_xruns, current_xruns - last_xrun_count);
            last_xrun_count = current_xruns;
          }
        }
      }
    }

    int16_t* outputBuffer = static_cast<int16_t*>(audioData);
    int32_t samplesRequested = numFrames * audioStream->getChannelCount();
    int32_t bytesRequested = samplesRequested * sizeof(int16_t);
    int32_t bytesCopied = 0;

    while (bytesCopied < bytesRequested) {
      int lf = atomic_load_int(last_free);
      int ff = atomic_load_int(first_free);

      // Check if we have data available
      if (lf == ff) {
        int32_t remainingBytes = bytesRequested - bytesCopied;
        if (!expect_silence.load(std::memory_order_relaxed)) {
          LOGW("UNDERRUN: No audio data available, filling %d bytes with silence", remainingBytes);
          underrun_count_.fetch_add(1, std::memory_order_relaxed);
        }
        memset(&outputBuffer[bytesCopied / sizeof(int16_t)], 0, remainingBytes);
        return oboe::DataCallbackResult::Continue;
      }

      // Read from current position in current buffer
      char* source = &buffer[lf * buffer_size + buffer_position];
      int32_t bytesAvailableInBuffer = buffer_size - buffer_position;
      int32_t bytesToCopy = (bytesRequested - bytesCopied) < bytesAvailableInBuffer ? (bytesRequested - bytesCopied) : bytesAvailableInBuffer;

      // Apply volume scaling while copying
      float vol = volume_scale.load(std::memory_order_relaxed);
      int16_t* src16 = (int16_t*)source;
      int16_t* dst16 = &outputBuffer[bytesCopied / sizeof(int16_t)];
      int samplesToCopy = bytesToCopy / sizeof(int16_t);

      if (vol >= 0.99f) {
        memcpy(dst16, src16, bytesToCopy);
      } else {
        for (int i = 0; i < samplesToCopy; i++) {
          dst16[i] = (int16_t)(src16[i] * vol);
        }
      }

      bytesCopied += bytesToCopy;
      buffer_position += bytesToCopy;

      // If we've consumed the entire buffer, move to next one
      if (buffer_position >= buffer_size) {
        buffer_position = 0;
        int next_buffer = (lf + 1) % buffer_num;
        atomic_store_int(last_free, next_buffer);
      }
    }

    return oboe::DataCallbackResult::Continue;
  }

  void onErrorAfterClose(oboe::AudioStream* oboeStream, oboe::Result error) override {
    LOGE("Stream closed due to error: %s", oboe::convertToText(error));

    if (error == oboe::Result::ErrorDisconnected) {
      LOGE("Audio device disconnected");
    }
  }

private:
  char* buffer;
  int buffer_size;
  int buffer_num;
  atomic_int* first_free;
  atomic_int* last_free;
  std::atomic<float>& volume_scale;
  std::atomic<int32_t>& underrun_count_;
  int buffer_position; // Tracks position within current buffer (in bytes)
};

static std::shared_ptr<oboe::AudioStream> audio_stream;
static XmpAudioCallback* audio_callback = nullptr;
static atomic_int first_free;
static atomic_int last_free;
static char* buffer = nullptr;
static int buffer_num;
static int buffer_size;
static pthread_mutex_t mutex;
static std::atomic<float> volume_scale(1.0f);
static std::atomic<int32_t> underrun_count(0);

static int oboe_open(int sr, int num, char* buf, int buf_size) {
  oboe::AudioStreamBuilder builder;

  // Initialize mutex
  if (pthread_mutex_init(&mutex, nullptr) != 0) {
    LOGE("Failed to initialize mutex");
    return -1;
  }

  // Create callback with the buffer that's already allocated
  audio_callback = new XmpAudioCallback(buf, buf_size, num, &first_free, &last_free, &volume_scale, &underrun_count);

  // Configure stream - request sample rate but Oboe may override
  builder.setDirection(oboe::Direction::Output)
    ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
    ->setSharingMode(oboe::SharingMode::Shared)
    ->setFormat(oboe::AudioFormat::I16)
    ->setChannelCount(oboe::ChannelCount::Stereo)
    ->setSampleRate(sr)
    ->setDataCallback(audio_callback)
    ->setErrorCallback(audio_callback);

  // Open stream
  oboe::Result result = builder.openStream(audio_stream);
  if (result != oboe::Result::OK) {
    LOGE("Failed to open stream: %s", oboe::convertToText(result));
    delete audio_callback;
    audio_callback = nullptr;
    pthread_mutex_destroy(&mutex);
    return -1;
  }

  LOGI("Stream opened: rate=%d, burst=%d, capacity=%d, sharingMode=%s",
    audio_stream->getSampleRate(),
    audio_stream->getFramesPerBurst(),
    audio_stream->getBufferCapacityInFrames(),
    audio_stream->getSharingMode() == oboe::SharingMode::Exclusive ? "Exclusive" : "Shared");

  // Log which audio API is being used
  oboe::AudioApi audioApi = audio_stream->getAudioApi();
  const char* apiName = (audioApi == oboe::AudioApi::AAudio) ? "AAudio" : (audioApi == oboe::AudioApi::OpenSLES) ? "OpenSL ES" : "Unknown";
  LOGI("Audio API: %s", apiName);

  // Return the actual sample rate Oboe opened with
  return audio_stream->getSampleRate();
}

static void oboe_close() {
  lock();

  if (audio_stream) {
    audio_stream->stop();
    audio_stream->close();
    audio_stream.reset();
  }

  if (audio_callback) {
    delete audio_callback;
    audio_callback = nullptr;
  }

  unlock();
  pthread_mutex_destroy(&mutex);
}

void close_audio() {
  oboe_close();
  if (buffer) {
    free(buffer);
    buffer = nullptr;
  }
}

int open_audio(int rate, int latency) {
  buffer_num = latency / BUFFER_TIME;

  if (buffer_num < 3) buffer_num = 3;

  // Allocate buffer with requested rate first (we'll resize if needed)
  buffer_size = rate * 2 * 2 * BUFFER_TIME / 1000;
  buffer = (char*)malloc(buffer_size * buffer_num);
  if (buffer == nullptr) {
    LOGE("Failed to allocate audio buffer");
    return -1;
  }

  // Open Oboe - it will return the actual sample rate
  int actual_rate = oboe_open(rate, buffer_num, buffer, buffer_size);
  if (actual_rate < 0) {
    free(buffer);
    buffer = nullptr;
    return -1;
  }

  // If actual rate differs from requested, we need to reallocate buffers
  if (actual_rate != rate) {
    LOGI("Sample rate mismatch: requested=%d, actual=%d - reallocating buffers", rate, actual_rate);

    // Close the stream
    oboe_close();

    // Recalculate buffer size for actual rate
    buffer_size = actual_rate * 2 * 2 * BUFFER_TIME / 1000;

    // Reallocate buffer
    free(buffer);
    buffer = (char*)malloc(buffer_size * buffer_num);
    if (buffer == nullptr) {
      LOGE("Failed to reallocate audio buffer");
      return -1;
    }

    // Reopen with correct buffer size
    int reopen_rate = oboe_open(actual_rate, buffer_num, buffer, buffer_size);
    if (reopen_rate < 0) {
      free(buffer);
      buffer = nullptr;
      return -1;
    }
  }

  // Both start at 0 - empty buffers
  first_free = 0;
  last_free = 0;

  LOGI("Audio opened: requested_rate=%d, actual_rate=%d, latency=%d, buffers=%d, buffer_size=%d", rate, actual_rate, latency, buffer_num, buffer_size);

  // Return the actual rate so xmp can use it
  return actual_rate;
}

void flush_audio() {
  lock();

  if (audio_stream) {
    // Wait for buffers to drain
    struct timespec sleepTime = {.tv_sec = 0, .tv_nsec = 10000 * 1000};

    while (last_free != first_free) {
      unlock();
      nanosleep(&sleepTime, nullptr);
      lock();
    }
  }

  unlock();
}

void drop_audio() {
  lock();

  // Reset buffer positions - empty buffers
  first_free = 0;
  last_free = 0;

  unlock();
}

int play_audio() {
  // Start the stream - the callback will begin pulling data
  // The main thread will fill buffers via fill_buffer() calls
  if (restart_audio() < 0) return -1;

  return 0;
}

void set_expect_silence(int val) {
  expect_silence.store(val != 0, std::memory_order_relaxed);
}

int has_free_buffer() {
  int lf = atomic_load_int(&last_free);
  int ff = atomic_load_int(&first_free);

  // Check if there's space to write a new buffer
  // If first_free + 1 (wrapped) == last_free, we're full
  int next_first = (ff + 1) % buffer_num;
  return next_first != lf;
}

int fill_buffer(int looped) {
  int ret;

  int ff = atomic_load_int(&first_free);
  int lf = atomic_load_int(&last_free);

  // Fill and enqueue buffer at first_free position
  char* b = &buffer[ff * buffer_size];

  // Move first_free forward
  int next_first = (ff + 1) % buffer_num;
  atomic_store_int(&first_free, next_first);

  ret = play_buffer(b, buffer_size, looped);

  // LOGI("fill_buffer: filled buffer %d, first_free %d->%d, last_free=%d, ret=%d",
  //      ff, ff, next_first, lf, ret);

  return ret;
}

int restart_audio() {
  lock();

  if (!audio_stream) {
    LOGE("restart_audio: audio_stream is null");
    unlock();
    return -1;
  }

  // Load atomic values for logging
  int ff = atomic_load_int(&first_free);
  int lf = atomic_load_int(&last_free);
  LOGI("restart_audio: Starting stream, first_free=%d, last_free=%d", ff, lf);

  oboe::Result result = audio_stream->requestStart();

  unlock();

  if (result != oboe::Result::OK) {
    LOGE("Failed to start stream: %s", oboe::convertToText(result));
    return -1;
  }

  LOGI("Stream started successfully");
  return 0;
}

int stop_audio() {
  lock();

  if (!audio_stream) {
    unlock();
    return -1;
  }

  oboe::Result result = audio_stream->requestStop();

  unlock();

  drop_audio();

  return result == oboe::Result::OK ? 0 : -1;
}

int get_volume() {
  // Volume is stored as 0.0-1.0, convert to millibels-like format
  // to maintain compatibility with existing code
  // -vol format where 0 = max volume, higher numbers = quieter
  float vol = volume_scale.load(std::memory_order_relaxed);
  int result = (int)((1.0f - vol) * 1000.0f);
  return result;
}

int set_volume(int vol) {
  // Convert from -vol format (0 = max, higher = quieter) to 0.0-1.0
  float new_volume = 1.0f - ((float)vol / 1000.0f);

  // Clamp to valid range
  if (new_volume < 0.0f) new_volume = 0.0f;
  if (new_volume > 1.0f) new_volume = 1.0f;

  // Store volume - will be applied in audio callback
  volume_scale.store(new_volume, std::memory_order_relaxed);

  return 0;
}

int get_audio_stats(struct AudioStats* stats) {
  if (!audio_stream || !stats) {
    return -1;
  }

  stats->sample_rate = audio_stream->getSampleRate();
  stats->frames_per_burst = audio_stream->getFramesPerBurst();
  stats->buffer_capacity = audio_stream->getBufferCapacityInFrames();
  stats->buffer_size = audio_stream->getBufferSizeInFrames();
  stats->underrun_count = underrun_count.load(std::memory_order_relaxed);

  // XRun count
  if (audio_stream->isXRunCountSupported()) {
    oboe::ResultWithValue<int32_t> xRunResult = audio_stream->getXRunCount();
    stats->xrun_count = xRunResult ? xRunResult.value() : 0;
  } else {
    stats->xrun_count = 0;
  }

  // Audio API
  oboe::AudioApi audioApi = audio_stream->getAudioApi();
  stats->audio_api = (audioApi == oboe::AudioApi::AAudio) ? "AAudio" : (audioApi == oboe::AudioApi::OpenSLES) ? "OpenSL ES" : "Unknown";

  // Sharing mode
  stats->sharing_mode = (audio_stream->getSharingMode() == oboe::SharingMode::Exclusive) ? "Exclusive" : "Shared";

  return 0;
}
