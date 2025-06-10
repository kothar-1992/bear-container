#include <jni.h>
#include <string>
#include <dlfcn.h>
#include "ESP.h"
#include "Hacks.h"
ESP espOverlay;
int type = 1, utype = 2;


/* ================ ESP FUNCTION =========================*/

extern "C" JNIEXPORT void JNICALL
Java_org_bearmod_container_floating_Overlay_DrawOn(JNIEnv *env, jclass , jobject espView, jobject canvas) {
    espOverlay = ESP(env, espView, canvas);
    if (espOverlay.isValid()){
        DrawESP(espOverlay, espOverlay.getWidth(), espOverlay.getHeight());
    }
}

extern "C"
JNIEXPORT jstring JNICALL
Java_org_bearmod_container_server_ApiServer_EXP(JNIEnv *env, jclass clazz) {
    // Authentication handled by Java KeyAuth API - return empty string
    return env->NewStringUTF("");
}

// Removed native authentication registration - using KeyAuth API instead
int Register1(JNIEnv *env) {
    // No native methods to register for authentication anymore
    return 0;
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env;
    vm->GetEnv((void **) &env, JNI_VERSION_1_6);
    if (Register1(env) != 0)
        return -1;
    FPS限制.SetFps(1000);
    FPS限制.AotuFPS_init();
    FPS限制.setAffinity();
    return JNI_VERSION_1_6;
}




extern "C" JNIEXPORT void JNICALL
Java_org_bearmod_container_floating_Overlay_Close(JNIEnv *, jobject) {
    Close();
    options.openState = -1;
    options.aimBullet = -1;
   // options.aimT = -1;
}


extern "C" JNIEXPORT jboolean JNICALL
Java_org_bearmod_container_floating_Overlay_getReady(JNIEnv *, jobject thiz) {
    int sockCheck = 1;

    if (!Create()) {
        perror("Creation failed");
        return false;
    }
    setsockopt(sock, SOL_SOCKET, SO_REUSEADDR, &sockCheck, sizeof(int));
    if (!Bind()) {
        perror("Bind failed");
        return false;
    }

    if (!Listen()) {
        perror("Listen failed");
        return false;
    }
    if (Accept()) {
        return true;
    }
}


extern "C"
JNIEXPORT void JNICALL
Java_org_bearmod_container_floating_ToggleAim_ToggleAim(JNIEnv *, jobject thiz, jboolean value) {
    if (value)
        options.openState = 0;
    else
        options.openState = -1;
}

extern "C"
JNIEXPORT void JNICALL
Java_org_bearmod_container_floating_ToggleBullet_ToggleBullet(JNIEnv *, jobject thiz, jboolean value) {
    if (value)
        options.aimBullet = 0;
    else
        options.aimBullet = -1;
}

extern "C"
JNIEXPORT void JNICALL
Java_org_bearmod_container_floating_ToggleSimulation_ToggleSimulation(JNIEnv *, jobject thiz, jboolean value) {
    if (value)
        options.openState = 0;
    else
        options.openState = -1;
}

extern "C" JNIEXPORT void JNICALL
Java_org_bearmod_container_floating_FloatingService_SettingValue(JNIEnv *, jobject, jint code, jboolean jboolean1) {

    switch ((int) code) {
        case 2:
            isPlayerLine = jboolean1;
            break;
        case 3:
            isPlayerBox = jboolean1;
            break;
        case 4:
            isSkeleton = jboolean1;
            break;
        case 5:
            isPlayerDistance = jboolean1;
            break;
        case 6:
            isPlayerHealth = jboolean1;
            break;
        case 7:
            isPlayerName = jboolean1;
            break;
        case 8:
            isPlayerHead = jboolean1;
            break;
        case 10:
            isPlayerWeapon = jboolean1;
            break;
        case 11:
            isGrenadeWarning = jboolean1;
            break;
        case 15:
            options.ignoreAi = jboolean1;
            break;
        case 16:
            isPlayerWeaponIcon = jboolean1;
            break;
        case 17:
            enemycountv1 = jboolean1;
            break;
        case 18:
            enemycountv2 = jboolean1;
            break;
        case 20:
            is360Alertv2 = jboolean1;
            break;

    }
}

extern "C"
JNIEXPORT void JNICALL
Java_org_bearmod_container_floating_FloatingService_SettingAim(JNIEnv *env, jobject thiz, jint setting_code, jboolean value) {
    switch ((int) setting_code) {
        case 1:
            options.openState = -1;
            break;
        case 2:
            options.aimBullet = -1;
            break;
        case 3:
            options.pour = value;
            break;
        case 4:
            options.ignoreBot = value;
            break;
        case 5:
            options.InputInversion = value;
            break;
        case 6:
            options.tracingStatus = value;
            break;
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_org_bearmod_container_floating_FloatingService_SettingMemory(JNIEnv *env, jobject thiz, jint setting_code, jboolean value) {
    switch ((int) setting_code) {
        case 1:
            otherFeature.LessRecoil = value;
            break;
        case 2:
            otherFeature.SmallCrosshair = value;
            break;
        case 3:
            otherFeature.Aimbot = value;
            break;
        case 4:
            otherFeature.WideView = value;
            break;
    }
}

extern "C" JNIEXPORT void JNICALL
Java_org_bearmod_container_floating_FloatingService_Range(JNIEnv *, jobject, jint range) {
    options.aimingRange = 1 + range;
}

extern "C" JNIEXPORT void JNICALL
Java_org_bearmod_container_floating_FloatingService_distances(JNIEnv *, jobject, jint distances) {
    options.aimingDist = distances;
}

extern "C"
JNIEXPORT void JNICALL
Java_org_bearmod_container_floating_FloatingService_recoil(JNIEnv *env, jobject thiz, jint recoil) {
    options.recCompe = recoil;
}

extern "C"
JNIEXPORT void JNICALL
Java_org_bearmod_container_floating_FloatingService_recoil2(JNIEnv *env, jobject thiz, jint recoil) {
    options.recCompe1 = recoil;
}

extern "C"
JNIEXPORT void JNICALL
Java_org_bearmod_container_floating_FloatingService_recoil3(JNIEnv *env, jobject thiz, jint recoil) {
    options.recCompe2 = recoil;
}

extern "C"
JNIEXPORT void JNICALL
Java_org_bearmod_container_floating_FloatingService_Bulletspeed(JNIEnv *env, jobject thiz, jint bulletspeed) {
    options.aimingSpeed = bulletspeed;
}

extern "C"
JNIEXPORT void JNICALL
Java_org_bearmod_container_floating_FloatingService_AimingSpeed(JNIEnv *env, jobject thiz, jint aimingspeed) {
    options.touchSpeed = aimingspeed;
}

extern "C"
JNIEXPORT void JNICALL
Java_org_bearmod_container_floating_FloatingService_Smoothness(JNIEnv *env, jobject thiz, jint smoothness) {
    options.Smoothing = smoothness;
}

extern "C"
JNIEXPORT void JNICALL
Java_org_bearmod_container_floating_FloatingService_TouchSize(JNIEnv *env, jobject thiz, jint touchsize) {
    options.touchSize = touchsize;
}

extern "C"
JNIEXPORT void JNICALL
Java_org_bearmod_container_floating_FloatingService_TouchPosX(JNIEnv *env, jobject thiz, jint touchposx) {
    options.touchX = touchposx;
}

extern "C"
JNIEXPORT void JNICALL
Java_org_bearmod_container_floating_FloatingService_TouchPosY(JNIEnv *env, jobject thiz, jint touchposy) {
    options.touchY = touchposy;
}


extern "C"
JNIEXPORT void JNICALL
Java_org_bearmod_container_floating_FloatingService_WideView(JNIEnv *env, jobject thiz, jint wideview) {
    otherFeature.WideView = wideview;
}

extern "C" JNIEXPORT void JNICALL
Java_org_bearmod_container_floating_FloatingService_Target(JNIEnv *, jobject, jint target) {
    options.aimbotmode = target;
}
extern "C" JNIEXPORT void JNICALL
Java_org_bearmod_container_floating_FloatingService_AimWhen(JNIEnv *, jobject, jint state) {
    options.aimingState = state;
}
extern "C" JNIEXPORT void JNICALL
Java_org_bearmod_container_floating_FloatingService_AimBy(JNIEnv *, jobject, jint aimby) {
    options.priority = aimby;
}
// ====================== Main Activity ====================== //


extern "C"
JNIEXPORT jstring JNICALL
Java_org_bearmod_container_server_ApiServer_getOwner(JNIEnv *env, jclass clazz) {
    return env->NewStringUTF("https://t.me/bear_mod");
}

extern "C"
JNIEXPORT jstring JNICALL
Java_org_bearmod_container_server_ApiServer_getTelegram(JNIEnv *env, jclass clazz) {
    return env->NewStringUTF("https://t.me/BearModChat");
}


extern "C"
JNIEXPORT jstring JNICALL
Java_org_bearmod_container_server_ApiServer_activity(JNIEnv *env, jclass clazz) {
    // Authentication handled by Java KeyAuth API - always return MainActivity
    return env->NewStringUTF("org.bearmod.container.activity.MainActivity");
}
extern "C"
JNIEXPORT jstring JNICALL
Java_org_bearmod_container_server_ApiServer_mainURL(JNIEnv *env, jclass clazz) {
    return env->NewStringUTF("https://uploadvip.top/shivam/download.php?file=YadavxModer.zip");
}

extern "C"
JNIEXPORT jstring JNICALL
Java_org_bearmod_container_server_ApiServer_URLJSON(JNIEnv *env, jclass clazz) {
    return env->NewStringUTF("https://uploadvip.top/shivam/download.php?file=yadav.json");
}

extern "C" JNIEXPORT jstring JNICALL
Java_org_bearmod_container_server_ApiServer_ApiKeyBox(JNIEnv *env, jclass clazz) {
    return env->NewStringUTF("0fcf16068e3c343f85d1abfb761c5609");
}

const std::string EXPECTED_APP_LABEL = "BEAR_LOADER";
extern "C"
JNIEXPORT jboolean JNICALL
Java_org_bearmod_container_server_ApiServer_nativeVerifyAppLabel(JNIEnv *env, jclass clazz, jstring appLabel) {
    const char *label = env->GetStringUTFChars(appLabel, nullptr);

    if (EXPECTED_APP_LABEL != std::string(label)) {
        env->ReleaseStringUTFChars(appLabel, label);
        return JNI_FALSE;
    }

    env->ReleaseStringUTFChars(appLabel, label);
    return JNI_TRUE;
}

extern "C"
JNIEXPORT void JNICALL
Java_org_bearmod_container_floating_FightMod_SettingValue(JNIEnv *,  jobject ,jint code,jboolean jboolean1) {
    switch((int)code){
        case 1:
            isHideItem = jboolean1;
            break;
    }
}
