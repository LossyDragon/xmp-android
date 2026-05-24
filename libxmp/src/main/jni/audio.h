#ifndef XMP_JNI_AUDIO_H
#define XMP_JNI_AUDIO_H

#include <cstdint>

#ifdef __cplusplus
extern "C" {
#endif

void drop_audio(void);

int fill_buffer(int);

int get_volume(void);

int has_free_buffer(void);

int open_audio(int, int);

int play_audio(void);

int play_buffer(void*, int, int);

int restart_audio(void);

int set_volume(int);

int stop_audio(void);

void close_audio(void);

void set_expect_silence(int val);

struct AudioStats {
  int32_t xrun_count;
  int32_t underrun_count;
  int32_t frames_per_burst;
  int32_t buffer_capacity;
  int32_t buffer_size;
  int sample_rate;
  const char* audio_api;
  const char* sharing_mode;
};
int get_audio_stats(struct AudioStats* stats);

#ifdef __cplusplus
}
#endif

#endif
