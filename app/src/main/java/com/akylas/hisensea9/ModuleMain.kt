package com.akylas.hisensea9

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlarmManager
import android.app.AndroidAppHelper
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.PowerManager
import android.os.SystemClock
import android.view.KeyEvent
import androidx.core.content.IntentCompat
import com.akylas.hisensea9.utils.SystemProperties
import com.akylas.hisensea9.utils.registerReceiver

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import de.robv.android.xposed.XposedHelpers.findClass;
import java.lang.reflect.Constructor

val rootPackage = "com.onyx";
val TAG = "com.akylas.hisensea9.lsposed";


val supportedPackages =
    arrayOf(
        "android",
        "com.onyx",
        "com.onyx.kreader"
    )

class ModuleMain : IXposedHookLoadPackage {

    var _appContext: Context? = null
    val appContext: Context
        get() {
            if (_appContext == null) {
                _appContext = AndroidAppHelper.currentApplication()

            }

            return _appContext!!
        }

    companion object {
        var mPowerManager: android.os.PowerManager? = null;
        var mPhoneWindowManager: Any? = null;
        var mPhoneWindowManagerHandler: Handler? = null;
        var mVolumeWakeLock: android.os.PowerManager.WakeLock? = null;
        var mLastUpKeyEvent: android.view.KeyEvent? = null;
        var mLastDownKeyEvent: android.view.KeyEvent? = null;
        var mInputManager: Any? = null
    }

    fun forceHideKeyguard() {
        val keyguardServiceDelegate = XposedHelpers.getObjectField(mPhoneWindowManager, "mKeyguardDelegate")
//        Log.i("forceHideKeyguard " + keyguardServiceDelegate)
        if (keyguardServiceDelegate != null)
            XposedHelpers.callMethod(
                keyguardServiceDelegate,
                "startKeyguardExitAnimation",
                SystemClock.uptimeMillis()
            )
    }

    fun sendPastKeyDownEvent() {
        if (mLastDownKeyEvent != null) {
            Log.i("sendPastKeyDownEvent")
            XposedHelpers.callMethod(mInputManager, "injectInputEvent", mLastDownKeyEvent, 0)
            mLastDownKeyEvent = null;

            if (mVolumeWakeLock?.isHeld == true) {
                mVolumeWakeLock?.release()
            }
            Log.i("sendEmptyMessageDelayed 595/596")
            mPhoneWindowManagerHandler?.sendEmptyMessageDelayed(595, 250L)
            mPhoneWindowManagerHandler?.sendEmptyMessageDelayed(596, 1650L)
            Log.i("sendEmptyMessageDelayed 601")
            mPhoneWindowManagerHandler?.sendEmptyMessageDelayed(601, 200L);
        }
    }

    fun sendPastKeyUpEvent() {
        if (mLastUpKeyEvent != null) {
            Log.i("sendPastKeyUpEvent")
            XposedHelpers.callMethod(mInputManager, "injectInputEvent", mLastUpKeyEvent, 0)
            mLastUpKeyEvent = null;
        }
    }

    fun handleVolumeKeyEventDown(paramKeyEvent: KeyEvent, isLocked: Boolean): Boolean {
        Log.i("handleVolumeKeyEventDown " + paramKeyEvent.keyCode + " " + paramKeyEvent.action  + " " + isLocked)
        if (isLocked) {
            val wakeLock = mVolumeWakeLock!!
            if (!wakeLock.isHeld) {

                mPhoneWindowManagerHandler?.removeMessages(595)
                mPhoneWindowManagerHandler?.removeMessages(596)
                try {
                    val str = SystemProperties.get("sys.linevibrator_touch")
                    val i = Integer.parseInt(str)
                    if (i > 0) {
                        val stringBuilder = StringBuilder()
                        stringBuilder.append(0 - i)
                        SystemProperties.set(
                            "sys.linevibrator_touch",
                            stringBuilder.toString()
                        );
                    }
                } finally { }
                forceHideKeyguard()
                XposedHelpers.callMethod(
                    mPhoneWindowManager,
                    "wakeUpFromWakeKey",
                    SystemClock.uptimeMillis(),
                    26,
                    false
                )
                wakeLock.acquire(2300L)
                mLastDownKeyEvent =  KeyEvent(paramKeyEvent)
                Log.i("sendEmptyMessageDelayed 600")
                mPhoneWindowManagerHandler?.sendEmptyMessageDelayed(600, 200L)
                return true;
            }
        }
        return false;
    }

    fun handleVolumeKeyEventUp(paramKeyEvent: KeyEvent, isLocked: Boolean): Boolean {
        val wakeLock = mVolumeWakeLock!!
        Log.i("handleVolumeKeyEventUp " + paramKeyEvent.keyCode + " " + paramKeyEvent.action  + " " + isLocked + " " + wakeLock.isHeld)
        if (wakeLock.isHeld && mLastUpKeyEvent == null) {
            mLastUpKeyEvent = KeyEvent(paramKeyEvent);
            return true;
        }
        return false;
    }

    fun handleWakeUpOnVolume(paramKeyEvent: KeyEvent): Boolean {
        val wakeOnVolume = SystemProperties.get("sys.wakeup_on_volume")
        if ("1" == wakeOnVolume) {
            val isLocked = mPowerManager?.isInteractive != true
            var keyCode = paramKeyEvent.keyCode;
            var action = paramKeyEvent.action;
            Log.i("handleWakeUpOnVolume " + paramKeyEvent.keyCode + " " + paramKeyEvent.action  + " " + isLocked)
            if (keyCode == 24 || keyCode == 25) {
                if (action == KeyEvent.ACTION_UP) {
                    return handleVolumeKeyEventUp(paramKeyEvent, isLocked);
                } else if (action == KeyEvent.ACTION_DOWN) {
                    return handleVolumeKeyEventDown(paramKeyEvent, isLocked);
                }
            } else if (keyCode == 26) {
                try {
                    val str = SystemProperties.get("sys.linevibrator_touch");
                    val i = Integer.parseInt(str);
                    if (i < 0) {
                        val stringBuilder = StringBuilder();
                        stringBuilder.append(0 - i);
                        Log.i("sys.linevibrator_touch " + stringBuilder.toString())
                        SystemProperties.set(
                            "sys.linevibrator_touch",
                            stringBuilder.toString()
                        );
                    }
                } finally {
                }
            }
        }
        return false;
    }

    @SuppressLint("NewApi")
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        Log.i("handleLoadPackage " + lpparam.packageName)
        if (lpparam.packageName == "com.android.systemui") {
            Log.i("patching  com.android.systemui" )
            findMethod(
                findClass(
                    "com.android.systemui.statusbar.phone.ScrimController",
                    lpparam.classLoader
                )
            ) { name == "applyState\$1\$1" }
                .hookBefore {
                    val state =
                        XposedHelpers.getObjectField(it.thisObject, "mState")
                    XposedHelpers.setFloatField(state, "mBehindAlpha", 0F)
                    XposedHelpers.setIntField(state, "mBehindTint", 0)
                    XposedHelpers.setFloatField(state, "mNotifAlpha", 0F)
                }
        }
         else if (lpparam.packageName == "android") {
//            Log.i("patching  android" + lpparam.packageName + " " + mPowerManager + " " + mAlarmService + " " + enableLightIntent)

            findMethod(
                findClass(
                    "com.android.server.policy.PhoneWindowManager",
                    lpparam.classLoader
                )
            ) { name == "interceptKeyBeforeQueueing" }
                .hookBefore {
                    if (mPhoneWindowManager == null) {
                        Log.i("interceptKeyBeforeQueueing  init")
                        mPhoneWindowManager = it.thisObject
                        mPhoneWindowManagerHandler =
                            XposedHelpers.getObjectField(mPhoneWindowManager, "mHandler") as Handler?
                        mInputManager =
                            XposedHelpers.getObjectField(mPhoneWindowManager, "mInputManager")
                        mPowerManager =
                            appContext.getSystemService(Context.POWER_SERVICE) as PowerManager
                        mVolumeWakeLock =
                            mPowerManager!!.newWakeLock(268435462, "Sys::VolumeWakeLock")
                    }
                    if (handleWakeUpOnVolume(it.args[0] as KeyEvent)) {
                        it.result = 0
                    }
                }
            findMethod(
                findClass(
                    "com.android.server.policy.PhoneWindowManager.PolicyHandler",
                    lpparam.classLoader
                )
            ) { name == "handleMessage" }
                .hookBefore {
                    val message = it.args[0] as Message
                    val what = message.what

                    if (what == 595) {
                        Log.i("handleMessage 595")
                        XposedHelpers.callMethod(mPowerManager, "goToSleep", SystemClock.uptimeMillis())
                        forceHideKeyguard()
                        it.result = true
                    } else if (what == 596) {
                        Log.i("handleMessage 596")
                        // fix backlight
                        try {
                            val str = SystemProperties.get("sys.linevibrator_touch");
                            val i = Integer.parseInt(str);
                            if (i < 0) {
                                val stringBuilder = StringBuilder();
                                stringBuilder.append(0 - i);
                                Log.i("sys.linevibrator_touch " + stringBuilder.toString())
                                SystemProperties.set(
                                    "sys.linevibrator_touch",
                                    stringBuilder.toString()
                                );
                            }
                        } finally {
                        }
                        it.result = true
                    } else if (what == 600) {
                        Log.i("handleMessage 600")
                        sendPastKeyDownEvent()
                        it.result = true
                    } else if (what == 601) {
                        Log.i("handleMessage 601")
                        sendPastKeyUpEvent()
                        it.result = true
                    }
                }
        }
    }
}
