// Mock libpipewire harness: drives the compiled synth's on_midi/on_process through the pw_* ABI it links against.
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>
#include <stdarg.h>

struct events { uint32_t version; void *p[11]; }; // process = p[7]
struct chunk { uint32_t offset, size, stride; int32_t flags; };
struct spa_data { uint32_t type, flags; uint64_t fd; uint32_t mapoffset, maxsize; void *data; struct chunk *chunk; };
struct spa_buffer { uint32_t n_metas, n_datas; void *metas; struct spa_data *datas; };
struct pw_buffer { struct spa_buffer *buffer; void *user_data; uint64_t size; uint64_t requested; uint64_t time; };

typedef void (*process_fn)(void *);
static process_fn audio_process, midi_process;
static void *audio_stream = (void *)0x1001, *midi_stream = (void *)0x1002;
static int failures = 0;
#define CHECK(c, ...) do { if (!(c)) { failures++; printf("FAIL: " __VA_ARGS__); printf("\n"); } else { printf("ok: " __VA_ARGS__); printf("\n"); } } while (0)

// --- mocked pipewire API
void pw_init(int *a, char ***b) {}
void pw_deinit(void) {}
void *pw_main_loop_new(void *p) { return (void *)0x2001; }
void *pw_main_loop_get_loop(void *l) { return (void *)0x2002; }
int pw_main_loop_quit(void *l) { return 0; }
void pw_main_loop_destroy(void *l) {}
void *pw_properties_new(const char *k, ...) { return (void *)0x3001; }
int pw_stream_connect(void *s, int dir, uint32_t id, int flags, void *params, uint32_t n) { return 0; }
void *pw_stream_new_simple(void *loop, const char *name, void *props, const struct events *ev, void *data) {
    if (strcmp(name, "atsynth-out") == 0) { audio_process = (process_fn)ev->p[7]; return audio_stream; }
    if (strcmp(name, "atsynth-midi-in") == 0) { midi_process = (process_fn)ev->p[7]; return midi_stream; }
    printf("FAIL: unexpected stream name %s\n", name); failures++; return NULL;
}
static struct pw_buffer *pending_audio, *pending_midi;
void *pw_stream_dequeue_buffer(void *s) { return s == audio_stream ? pending_audio : pending_midi; }
int pw_stream_queue_buffer(void *s, struct pw_buffer *b) { return 0; }

// --- buffers
static struct pw_buffer *make_buffer(void *data, uint32_t maxsize, uint64_t requested) {
    struct pw_buffer *pb = calloc(1, sizeof *pb);
    struct spa_buffer *sb = calloc(1, sizeof *sb);
    struct spa_data *sd = calloc(1, sizeof *sd);
    struct chunk *ch = calloc(1, sizeof *ch);
    sd->data = data; sd->maxsize = maxsize; sd->chunk = ch;
    sb->n_datas = 1; sb->datas = sd;
    pb->buffer = sb; pb->requested = requested;
    return pb;
}
// spa_pod sequence with one control: header {size,type=16}{unit,pad}; control {offset,type}{pod size,pod type}{payload}
static struct pw_buffer *midi_buffer_raw(uint8_t status, uint8_t note, uint8_t vel) {
    uint8_t *m = calloc(1, 64);
    uint32_t *w = (uint32_t *)m;
    w[1] = 16;                 // sequence type
    w[0] = 8 + 16 + 8;         // body size: unit/pad(8) + control(8+8+pad(3->8))
    w[4] = 0; w[5] = 2;        // control: offset, type = midi (2)
    w[6] = 3; w[7] = 8;        // value pod: size 3, type Bytes
    m[32] = status; m[33] = note; m[34] = vel;
    return make_buffer(m, 64, 0);
}
static struct pw_buffer *midi_buffer_ump(uint32_t word) {
    uint8_t *m = calloc(1, 64);
    uint32_t *w = (uint32_t *)m;
    w[1] = 16; w[0] = 8 + 16 + 8;
    w[4] = 0; w[5] = 4;        // type = ump (4)
    w[6] = 4; w[7] = 8;
    memcpy(m + 32, &word, 4);
    return make_buffer(m, 64, 0);
}

int synth_main(void);
static int run_checks(void) {
    CHECK(audio_process && midi_process, "both streams registered process callbacks");
    // silence before any note
    float out[480];
    pending_audio = make_buffer(out, sizeof out, 480);
    memset(out, 0x7f, sizeof out);
    audio_process(NULL);
    int silent = 1; for (int i = 0; i < 480; i++) if (out[i] != 0.0f) silent = 0;
    CHECK(silent, "silence before note-on");
    struct chunk *ch = pending_audio->buffer->datas->chunk;
    CHECK(ch->offset == 0 && ch->size == 480 * 4 && ch->stride == 4, "chunk metadata offset=%u size=%u stride=%u", ch->offset, ch->size, ch->stride);
    // raw MIDI note on A4
    pending_midi = midi_buffer_raw(0x90, 69, 100);
    midi_process(NULL);
    double w = 2.0 * M_PI * 440.0 / 48000.0, maxerr = 0; int n = 0;
    for (int blk = 0; blk < 4; blk++) {
        audio_process(NULL);
        for (int i = 0; i < 480; i++, n++) { double e = fabs(out[i] - 0.2 * sin(n * w)); if (e > maxerr) maxerr = e; }
    }
    CHECK(maxerr < 1e-4, "440Hz sine at 0.2 amplitude across 1920 samples (max err %g)", maxerr);
    // requested < maxsize is honoured
    pending_audio->requested = 100; memset(out, 0, sizeof out); audio_process(NULL);
    CHECK(ch->size == 100 * 4, "requested frame count limits chunk size (%u)", ch->size);
    pending_audio->requested = 480;
    // note off
    pending_midi = midi_buffer_raw(0x80, 69, 0); midi_process(NULL);
    audio_process(NULL);
    silent = 1; for (int i = 0; i < 480; i++) if (out[i] != 0.0f) silent = 0;
    CHECK(silent, "silence after note-off");
    // UMP note on C5 (72): mt=2, status 0x90, note 72, vel 100
    uint32_t ump = (2u << 28) | (0x90u << 16) | (72u << 8) | 100u;
    pending_midi = midi_buffer_ump(ump); midi_process(NULL);
    audio_process(NULL);
    // phase is continuous across notes, so verify frequency via the sine recurrence and amplitude via the peak
    double w2 = 2.0 * M_PI * 440.0 * exp2((72 - 69) / 12.0) / 48000.0; maxerr = 0; double peak = 0;
    for (int i = 2; i < 480; i++) { double e = fabs(out[i] - (2 * cos(w2) * out[i - 1] - out[i - 2])); if (e > maxerr) maxerr = e; }
    for (int i = 0; i < 480; i++) if (fabs(out[i]) > peak) peak = fabs(out[i]);
    CHECK(maxerr < 1e-4 && fabs(peak - 0.2) < 1e-3, "UMP note-on C5 produces 523.25Hz sine (recurrence err %g, peak %g)", maxerr, peak);
    // note-on with velocity 0 acts as note-off
    pending_midi = midi_buffer_raw(0x90, 72, 0); midi_process(NULL);
    audio_process(NULL);
    silent = 1; for (int i = 0; i < 480; i++) if (out[i] != 0.0f) silent = 0;
    CHECK(silent, "velocity-0 note-on is note-off");
    return failures;
}
int pw_main_loop_run(void *l) { return run_checks(); }
int main(void) {
    int r = synth_main();
    printf("%s (%d failures, synth_main returned %d)\n", failures == 0 ? "ALL PASS" : "FAILURES", failures, r);
    return failures == 0 ? 0 : 1;
}
