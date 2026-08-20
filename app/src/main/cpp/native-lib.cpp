#include <jni.h>
#include <string>
#include <string.h>
#include <android/log.h>
#include <stddef.h>
#include <malloc.h>
#include <stdbool.h>

#define ENABLE_VFS 1
#include <fcntl.h>
extern "C" {
#include <mgba/core/core.h>
#include <mgba-util/vfs.h>
#include <mgba-util/audio-buffer.h>
}

#define LOG_TAG "FroggBA"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// ──────────────────────────────────────────────────────────────────────
// All globals are owned exclusively by EmulationThread.
// No other thread may call any function below.
// ──────────────────────────────────────────────────────────────────────

static struct mCore* g_core = nullptr;
static uint32_t* g_videoBuffer   = nullptr; // mGBA writes here  (back)
static uint32_t* g_displayBuffer = nullptr; // GL thread reads   (front)
static unsigned g_width  = 0;
static unsigned g_height = 0;

// ── initCoreJNI ─────────────────────────────────────────────────────
// Creates an mGBA core, loads a ROM, allocates double-buffered video,
// and returns a DirectByteBuffer wrapping the DISPLAY (front) buffer.
extern "C" JNIEXPORT jobject JNICALL
Java_com_lemonsquad_froggba_EmulationThread_initCoreJNI(JNIEnv* env, jobject, jstring path) {
    // Tear down any previous core
    if (g_core)          { g_core->deinit(g_core); g_core = nullptr; }
    if (g_videoBuffer)   { free(g_videoBuffer);    g_videoBuffer = nullptr; }
    if (g_displayBuffer) { free(g_displayBuffer);  g_displayBuffer = nullptr; }

    const char* nativePath = env->GetStringUTFChars(path, nullptr);
    LOGI("Initializing emulator with ROM: %s", nativePath);

    g_core = mCoreCreate(mPLATFORM_GBA);
    if (!g_core) {
        LOGE("mCoreCreate failed.");
        env->ReleaseStringUTFChars(path, nativePath);
        return nullptr;
    }

    if (!g_core->init(g_core)) {
        LOGE("core->init failed.");
        g_core->deinit(g_core); g_core = nullptr;
        env->ReleaseStringUTFChars(path, nativePath);
        return nullptr;
    }

    mCoreInitConfig(g_core, "FroggBA");

    struct VFile* rom = VFileOpen(nativePath, O_RDONLY);
    if (!rom) {
        LOGE("VFileOpen failed.");
        g_core->deinit(g_core); g_core = nullptr;
        env->ReleaseStringUTFChars(path, nativePath);
        return nullptr;
    }

    if (!g_core->loadROM(g_core, rom)) {
        LOGE("core->loadROM failed.");
        g_core->deinit(g_core); g_core = nullptr;
        env->ReleaseStringUTFChars(path, nativePath);
        return nullptr;
    }

    g_core->baseVideoSize(g_core, &g_width, &g_height);

    g_videoBuffer   = (uint32_t*) calloc(g_width * g_height, sizeof(uint32_t));
    g_displayBuffer = (uint32_t*) calloc(g_width * g_height, sizeof(uint32_t));
    g_core->setVideoBuffer(g_core, g_videoBuffer, g_width);

    g_core->setAudioBufferSize(g_core, 8192);
    g_core->reset(g_core);

    LOGI("Emulator ready. Resolution: %ux%u", g_width, g_height);
    env->ReleaseStringUTFChars(path, nativePath);

    // The GL thread receives a view into the DISPLAY buffer only.
    return env->NewDirectByteBuffer(g_displayBuffer,
                                    g_width * g_height * sizeof(uint32_t));
}

// ── stepFrameJNI ────────────────────────────────────────────────────
// One atomic emulator tick:
//   1. Push input
//   2. Run one GBA frame
//   3. Publish video  (memcpy back → front)
//   4. Read audio
// Returns the number of audio FRAMES read (each frame = 2 × int16).
extern "C" JNIEXPORT jint JNICALL
Java_com_lemonsquad_froggba_EmulationThread_stepFrameJNI(
        JNIEnv* env, jobject, jint keyMask,
        jshortArray audioOut, jint audioCapacity) {
    if (!g_core) return 0;

    g_core->setKeys(g_core, keyMask);
    g_core->runFrame(g_core);

    // Publish video — 153 600 bytes on GBA, ~50 µs on ARM64
    memcpy(g_displayBuffer, g_videoBuffer,
           g_width * g_height * sizeof(uint32_t));

    // Read audio
    struct mAudioBuffer* audioBuf = g_core->getAudioBuffer(g_core);
    if (!audioBuf) return 0;

    jshort* buf = env->GetShortArrayElements(audioOut, nullptr);
    size_t framesRead = mAudioBufferRead(audioBuf, (int16_t*)buf, audioCapacity);
    env->ReleaseShortArrayElements(audioOut, buf, 0);

    return (jint) framesRead;
}

// ── getSampleRateJNI ────────────────────────────────────────────────
extern "C" JNIEXPORT jint JNICALL
Java_com_lemonsquad_froggba_EmulationThread_getSampleRateJNI(JNIEnv*, jobject) {
    return g_core ? g_core->audioSampleRate(g_core) : 32768;
}

// ── destroyCoreJNI ──────────────────────────────────────────────────
extern "C" JNIEXPORT void JNICALL
Java_com_lemonsquad_froggba_EmulationThread_destroyCoreJNI(JNIEnv*, jobject) {
    if (g_core)          { g_core->deinit(g_core); g_core = nullptr; }
    if (g_videoBuffer)   { free(g_videoBuffer);    g_videoBuffer = nullptr; }
    if (g_displayBuffer) { free(g_displayBuffer);  g_displayBuffer = nullptr; }
    LOGI("Emulator core destroyed.");
}
