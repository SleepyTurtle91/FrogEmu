#include <jni.h>
#include <string>
#include <android/log.h>

#define ENABLE_VFS 1
#include <fcntl.h>
extern "C" {
#include <mgba/core/core.h>
#include <mgba-util/vfs.h>
}

#define LOG_TAG "FroggBA"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C" JNIEXPORT jstring JNICALL
Java_com_lemonsquad_froggba_MainActivity_stringFromJNI(JNIEnv* env, jobject) {
    return env->NewStringUTF("FroggBA: mGBA core initialized successfully!");
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_lemonsquad_froggba_MainActivity_loadRomJNI(JNIEnv* env, jobject, jstring path) {
    const char *nativePath = env->GetStringUTFChars(path, nullptr);
    LOGI("Loading ROM from path: %s", nativePath);
    
    struct mCore* core = mCoreCreate(mPLATFORM_GBA);
    if (!core) {
        LOGE("Failed to create mGBA core instance.");
        env->ReleaseStringUTFChars(path, nativePath);
        return JNI_FALSE;
    }
    
    if (!core->init(core)) {
        LOGE("Failed to initialize mGBA core.");
        core->deinit(core);
        env->ReleaseStringUTFChars(path, nativePath);
        return JNI_FALSE;
    }
    
    struct VFile* rom = VFileOpen(nativePath, O_RDONLY);
    if (!rom) {
        LOGE("VFileOpen failed to open the ROM file natively.");
        core->deinit(core);
        env->ReleaseStringUTFChars(path, nativePath);
        return JNI_FALSE;
    }
    
    bool success = core->loadROM(core, rom);
    if (success) {
        LOGI("Milestone 2 Achieved! core->loadROM returned SUCCESS.");
    } else {
        LOGE("core->loadROM failed (possibly invalid ROM format).");
    }
    
    env->ReleaseStringUTFChars(path, nativePath);
    return success ? JNI_TRUE : JNI_FALSE;
}
