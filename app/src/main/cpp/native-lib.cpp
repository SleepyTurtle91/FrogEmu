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
#include <mgba/gba/interface.h>
#include <mgba/internal/gba/gba.h>
#include <mgba/internal/gba/sio.h>
#include <mgba/internal/gba/io.h>
}

#define LOG_TAG "FrogEmu"
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

// ── FroggBA Link Adapter Driver ─────────────────────────────────────
struct FroggBALinkDriver {
    struct GBASIODriver d;
    int deviceId;          // 0 = Master, 1..3 = Slave
    int connectedDevices;  // 1 to 4
    bool isConnected;
    bool transferPending;
    uint16_t pendingOutWord;
};

static struct FroggBALinkDriver g_linkDriver;

static bool _froggbaSioInit(struct GBASIODriver* d) {
    LOGI("FroggBA SIO Driver initialized");
    return true;
}

static void _froggbaSioDeinit(struct GBASIODriver* d) {
    LOGI("FroggBA SIO Driver deinitialized");
}

static void _froggbaSioReset(struct GBASIODriver* d) {
    struct FroggBALinkDriver* driver = (struct FroggBALinkDriver*) d;
    driver->transferPending = false;
    driver->pendingOutWord = 0xFFFF;
}

static bool _froggbaSioHandlesMode(struct GBASIODriver* d, enum GBASIOMode mode) {
    return mode == GBA_SIO_MULTI;
}

static int _froggbaSioConnectedDevices(struct GBASIODriver* d) {
    struct FroggBALinkDriver* driver = (struct FroggBALinkDriver*) d;
    return driver->isConnected ? driver->connectedDevices : 0;
}

static int _froggbaSioDeviceId(struct GBASIODriver* d) {
    struct FroggBALinkDriver* driver = (struct FroggBALinkDriver*) d;
    return driver->deviceId;
}

static bool _froggbaSioStart(struct GBASIODriver* d) {
    struct FroggBALinkDriver* driver = (struct FroggBALinkDriver*) d;
    if (!driver->isConnected) {
        return true; // Not connected: let core finish locally with disconnected behavior
    }
    struct GBASIO* sio = d->p;
    if (!sio || !sio->p) return true;

    // Capture the 16-bit word the game is transmitting
    driver->pendingOutWord = sio->p->memory.io[GBA_REG(SIOMLT_SEND)];
    driver->transferPending = true;

    // Suppress mGBA's internal timer; transfer will complete when EmulationThread
    // calls completeLinkTransferJNI() after network/loopback exchange
    return false;
}

static void _initLinkDriver() {
    memset(&g_linkDriver, 0, sizeof(g_linkDriver));
    g_linkDriver.d.init = _froggbaSioInit;
    g_linkDriver.d.deinit = _froggbaSioDeinit;
    g_linkDriver.d.reset = _froggbaSioReset;
    g_linkDriver.d.handlesMode = _froggbaSioHandlesMode;
    g_linkDriver.d.connectedDevices = _froggbaSioConnectedDevices;
    g_linkDriver.d.deviceId = _froggbaSioDeviceId;
    g_linkDriver.d.start = _froggbaSioStart;
    g_linkDriver.deviceId = 0;
    g_linkDriver.connectedDevices = 2;
    g_linkDriver.isConnected = false;
    g_linkDriver.transferPending = false;
    g_linkDriver.pendingOutWord = 0xFFFF;
}

// ── initCoreJNI ─────────────────────────────────────────────────────
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

    mCoreInitConfig(g_core, "FrogEmu");

    // Initialize and attach the FroggBA Link Adapter
    _initLinkDriver();
    g_core->setPeripheral(g_core, mPERIPH_GBA_LINK_PORT, &g_linkDriver.d);

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

    LOGI("Emulator ready with Link Adapter attached. Resolution: %ux%u", g_width, g_height);
    env->ReleaseStringUTFChars(path, nativePath);

    // The GL thread receives a view into the DISPLAY buffer only.
    return env->NewDirectByteBuffer(g_displayBuffer,
                                    g_width * g_height * sizeof(uint32_t));
}

// ── stepFrameJNI ────────────────────────────────────────────────────
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

// ── Link Adapter JNI methods (EmulationThread ONLY) ──────────────────

extern "C" JNIEXPORT void JNICALL
Java_com_lemonsquad_froggba_EmulationThread_setLinkConfigJNI(
        JNIEnv*, jobject, jboolean connected, jint deviceId, jint numDevices) {
    if (g_linkDriver.isConnected != (bool)connected ||
        g_linkDriver.deviceId != deviceId ||
        g_linkDriver.connectedDevices != numDevices) {
        g_linkDriver.isConnected = connected;
        g_linkDriver.deviceId = deviceId;
        g_linkDriver.connectedDevices = numDevices;
        LOGI("Link Config updated: connected=%d, deviceId=%d, numDevices=%d",
             (int)connected, (int)deviceId, (int)numDevices);
    }
}

extern "C" JNIEXPORT jint JNICALL
Java_com_lemonsquad_froggba_EmulationThread_getLinkPendingOutJNI(JNIEnv*, jobject) {
    if (g_linkDriver.transferPending) {
        return (jint) g_linkDriver.pendingOutWord;
    }
    return -1;
}

extern "C" JNIEXPORT void JNICALL
Java_com_lemonsquad_froggba_EmulationThread_completeLinkTransferJNI(
        JNIEnv* env, jobject, jshortArray multiData4) {
    if (!g_linkDriver.transferPending || !g_linkDriver.d.p) return;

    jshort* data = env->GetShortArrayElements(multiData4, nullptr);
    uint16_t rawData[4];
    for (int i = 0; i < 4; ++i) {
        rawData[i] = (uint16_t) data[i];
    }
    env->ReleaseShortArrayElements(multiData4, data, JNI_ABORT);

    // Call official mGBA completion: populates SIOMULTI0..3, clears busy bit, raises SIO IRQ
    GBASIOMultiplayerFinishTransfer(g_linkDriver.d.p, rawData, 0);
    g_linkDriver.transferPending = false;
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
