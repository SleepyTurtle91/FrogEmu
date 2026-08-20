#include <jni.h>
#include <string>
#include <android/log.h>
#include <stddef.h>
#include <malloc.h>
#include <stdbool.h>

#define ENABLE_VFS 1
#include <fcntl.h>
extern "C" {
#include <mgba/core/core.h>
#include <mgba-util/vfs.h>
}

#define LOG_TAG "FroggBA"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static struct mCore* g_core = nullptr;
static uint32_t* g_videoBuffer = nullptr;
static unsigned g_width = 0;
static unsigned g_height = 0;

extern "C" JNIEXPORT jobject JNICALL
Java_com_lemonsquad_froggba_MainActivity_initEmulatorJNI(JNIEnv* env, jobject, jstring path) {
    if (g_core) {
        g_core->deinit(g_core);
        g_core = nullptr;
    }
    if (g_videoBuffer) {
        free(g_videoBuffer);
        g_videoBuffer = nullptr;
    }

    const char *nativePath = env->GetStringUTFChars(path, nullptr);
    LOGI("Initializing Emulator with ROM: %s", nativePath);
    
    g_core = mCoreCreate(mPLATFORM_GBA);
    if (!g_core) {
        LOGE("Failed to create mGBA core.");
        env->ReleaseStringUTFChars(path, nativePath);
        return nullptr;
    }
    
    if (!g_core->init(g_core)) {
        LOGE("Failed to initialize mGBA core.");
        g_core->deinit(g_core);
        g_core = nullptr;
        env->ReleaseStringUTFChars(path, nativePath);
        return nullptr;
    }
    
    mCoreInitConfig(g_core, "FroggBA");
    
    struct VFile* rom = VFileOpen(nativePath, O_RDONLY);
    if (!rom) {
        LOGE("VFileOpen failed.");
        g_core->deinit(g_core);
        g_core = nullptr;
        env->ReleaseStringUTFChars(path, nativePath);
        return nullptr;
    }
    
    if (!g_core->loadROM(g_core, rom)) {
        LOGE("core->loadROM failed.");
        g_core->deinit(g_core);
        g_core = nullptr;
        env->ReleaseStringUTFChars(path, nativePath);
        return nullptr;
    }
    
    g_core->baseVideoSize(g_core, &g_width, &g_height);
    g_videoBuffer = (uint32_t*) calloc(g_width * g_height, sizeof(uint32_t));
    g_core->setVideoBuffer(g_core, g_videoBuffer, g_width);
    g_core->reset(g_core);
    
    LOGI("Emulator Initialized successfully! Resolution: %ux%u", g_width, g_height);
    env->ReleaseStringUTFChars(path, nativePath);
    
    return env->NewDirectByteBuffer(g_videoBuffer, g_width * g_height * sizeof(uint32_t));
}

extern "C" JNIEXPORT void JNICALL
Java_com_lemonsquad_froggba_MainActivity_runFrameJNI(JNIEnv* env, jobject) {
    if (g_core) {
        g_core->runFrame(g_core);
    }
}
