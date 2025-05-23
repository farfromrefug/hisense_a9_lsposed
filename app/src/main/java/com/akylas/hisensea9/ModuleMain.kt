package com.akylas.hisensea9

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AndroidAppHelper
import android.content.Context
import android.content.res.ColorStateList
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Message
import android.os.PowerManager
import android.os.SystemClock
import android.provider.MediaStore
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.KeyEvent
import android.view.MenuItem
import android.view.View
import com.akylas.hisensea9.utils.SystemProperties
import com.akylas.hisensea9.utils.Preferences
import com.akylas.hisensea9.utils.registerReceiver

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import de.robv.android.xposed.XposedHelpers.findClass;
import android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
import android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBar
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment

val TAG = "com.akylas.hisensea9.lsposed";

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
        
        var mEinkPressDownTime: Long = -1

        @RequiresApi(Build.VERSION_CODES.O)
        fun tintMenuIcon(item: MenuItem, color: Int) {
//            Log.i("tintMenuIcon $item ${item.icon}")
            item.setIconTintList(
                ColorStateList(
                    arrayOf(
                        intArrayOf(android.R.attr.state_enabled)
                    ), intArrayOf(
                        Color.BLACK
                    )
                )
            )
//            item.setIconTintMode(PorterDuff.Mode.)
//            val normalDrawable = item.icon;
//            if (normalDrawable != null) {
//                val wrapDrawable = DrawableCompat.wrap(normalDrawable!!);
//                DrawableCompat.setTint(wrapDrawable, (color));
//                item.setIcon(wrapDrawable);
//            }
        }
    }


    fun forceHideKeyguard() {
        val keyguardServiceDelegate =
            XposedHelpers.getObjectField(mPhoneWindowManager, "mKeyguardDelegate")
//        Log.i("forceHideKeyguard " + keyguardServiceDelegate)
        if (keyguardServiceDelegate != null) XposedHelpers.callMethod(
            keyguardServiceDelegate, "startKeyguardExitAnimation", SystemClock.uptimeMillis()
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
            val prefs = Preferences()
            val delay = prefs.getInt("sleep_delay", 550)
            val cleanup_delay = prefs.getInt("volume_key_cleanup_delay", 1400)
            val key_up_delay = prefs.getInt("volume_key_up_delay", 160)
            Log.i("delay delay:$delay cleanup_delay:$cleanup_delay key_up_delay:$key_up_delay")
            mPhoneWindowManagerHandler?.sendEmptyMessageDelayed(595, delay.toLong())
            mPhoneWindowManagerHandler?.sendEmptyMessageDelayed(
                596, (delay + cleanup_delay).toLong()
            )
            mPhoneWindowManagerHandler?.sendEmptyMessageDelayed(601, key_up_delay.toLong());
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
//        Log.i("handleVolumeKeyEventDown " + paramKeyEvent.keyCode + " " + paramKeyEvent.action  + " " + isLocked)
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
                            "sys.linevibrator_touch", stringBuilder.toString()
                        );
                    }
                } finally {
                }
                forceHideKeyguard()
                XposedHelpers.callMethod(
                    mPhoneWindowManager, "wakeUpFromWakeKey", SystemClock.uptimeMillis(), 26, false
                )
                wakeLock.acquire(2300L)
                mLastDownKeyEvent = KeyEvent(paramKeyEvent)
                val prefs = Preferences()
                val key_down_delay = prefs.getInt("volume_key_down_delay", 300)
//                Log.i("sendEmptyMessageDelayed 600 with delay $key_down_delay")
                mPhoneWindowManagerHandler?.sendEmptyMessageDelayed(600, key_down_delay.toLong())
                return true;
            }
        }
        return false;
    }

    fun handleVolumeKeyEventUp(paramKeyEvent: KeyEvent, isLocked: Boolean): Boolean {
        val wakeLock = mVolumeWakeLock!!
//        Log.i("handleVolumeKeyEventUp " + paramKeyEvent.keyCode + " " + paramKeyEvent.action  + " " + isLocked + " " + wakeLock.isHeld + " " + mLastUpKeyEvent)
        if (wakeLock.isHeld) {
            if (mLastUpKeyEvent == null) {
                mLastUpKeyEvent = KeyEvent(paramKeyEvent);
            }
            return true;
        }
        return false;
    }
    
    fun refreshScreen( delay: Int, cleanup_delay: Int) {
        val isLocked = mPowerManager?.isInteractive != true
        if (isLocked) {
                val wakeLock = mVolumeWakeLock!!
                if (!wakeLock.isHeld) {
                    forceHideKeyguard()
                    XposedHelpers.callMethod(
                        mPhoneWindowManager,
                        "wakeUpFromWakeKey",
                        SystemClock.uptimeMillis(),
                        26,
                        false
                    )
                    
//                    Log.i("delay delay:$delay cleanup_delay:$cleanup_delay")
                    if (delay > 0) {
                        wakeLock.acquire(2300L)
                        mPhoneWindowManagerHandler?.sendEmptyMessageDelayed(595, delay.toLong())
                        mPhoneWindowManagerHandler?.sendEmptyMessageDelayed(
                            596, (delay + cleanup_delay).toLong()
                        )
                    }
                    
                }
            }
    }
    
    fun sleepScreen(){
        XposedHelpers.callMethod(
            mPowerManager, "goToSleep", SystemClock.uptimeMillis()
        )
        forceHideKeyguard()
      }

    fun handleWakeUpOnVolume(paramKeyEvent: KeyEvent): Boolean {
//        Log.i("interceptKeyBeforeQueueing " + paramKeyEvent.keyCode + " " + paramKeyEvent.action  + " ")
        val wakeOnVolume = SystemProperties.get("sys.wakeup_on_volume")
        if ("1" == wakeOnVolume) {
            val isLocked = mPowerManager?.isInteractive != true
            var keyCode = paramKeyEvent.keyCode;
            var action = paramKeyEvent.action;
//            Log.i("handleWakeUpOnVolume " + paramKeyEvent.keyCode + " " + paramKeyEvent.action  + " " + isLocked)
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
//                        Log.i("sys.linevibrator_touch " + stringBuilder.toString())
                        SystemProperties.set(
                            "sys.linevibrator_touch", stringBuilder.toString()
                        );
                    }
                } finally {
                }
            } else if (keyCode == 0) {
                
                if (action == KeyEvent.ACTION_DOWN) {
                    mEinkPressDownTime = SystemClock.uptimeMillis() 
                } else if (action == KeyEvent.ACTION_UP) {
                    val prefs = Preferences()
                    if (SystemClock.uptimeMillis() - mEinkPressDownTime > 1000) {
                       if (prefs.getBoolean("eink_longpress_camera", false) && isLocked) {
                          val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE)
                          intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                          try {
                            refreshScreen(0,0)
                            appContext.startActivity(intent)
                          } finally {
                          }
                      } 
                    } else {
                        // Handle normal press            
                      val delay = prefs.getInt("eink_button_sleep_delay", 4000)
                      val cleanup_delay = prefs.getInt("volume_key_cleanup_delay", 1400)
                      refreshScreen(delay, cleanup_delay)
                    }              
                }
            }
        }
        return false;
    }

    @SuppressLint("NewApi")
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
//        Log.i("handleLoadPackage " + lpparam.packageName)
        val pref = Preferences()
        if (lpparam.packageName == "com.android.systemui") {
//            Log.i("patching  com.android.systemui" )
            if (pref.getBoolean("disable_lockscreen_overlay", true)) {
                findMethod(
                    findClass(
                        "com.android.systemui.statusbar.phone.ScrimController", lpparam.classLoader
                    )
                ) { name == "applyState\$1\$1" }.hookBefore {
                    val state = XposedHelpers.getObjectField(it.thisObject, "mState")
                    XposedHelpers.setFloatField(state, "mBehindAlpha", 0F)
                    XposedHelpers.setIntField(state, "mBehindTint", 0)
                    XposedHelpers.setFloatField(state, "mNotifAlpha", 0F)
                }
            }
            var wakingForNotif = false;
            findMethod(
                findClass(
                    "com.android.systemui.doze.DozeScreenState", lpparam.classLoader
                )
            ) { name == "transitionTo" }.hookBefore {
                Log.i("doze transitionTo " + it.args[0] + " " + it.args[1])
                if (it.args[0] == 2) {
                    wakingForNotif = true
                }
            }
        } else if (lpparam.packageName == "com.android.messaging") {
            findMethod(
                findClass(
                    "com.android.messaging.ui.ConversationDrawables", lpparam.classLoader
                )
            ) { name == "getBubbleDrawable" }.hookBefore() {
                if (it.args[1] == true) {
                    it.result = (XposedHelpers.getObjectField(
                        it.thisObject,
                        "mIncomingBubbleNoArrowDrawable"
                    ) as Drawable).constantState!!.newDrawable().mutate()
                } else {
                    it.result = (XposedHelpers.getObjectField(
                        it.thisObject,
                        "mOutgoingBubbleNoArrowDrawable"
                    ) as Drawable).constantState!!.newDrawable().mutate()
                }
            }
//                findMethod(
//                    findClass(
//                        "com.android.messaging.ui.BugleActionBarActivity", lpparam.classLoader
//                    )
//                ) { name == "updateActionBar" }.hookAfter() {
//                    Log.i("BugleActionBarActivity updateActionBar ${it.thisObject}")
////                    XposedHelpers.callMethod(it.args[0], "setBackgroundDrawable", ColorDrawable(Color.WHITE))
//                    val title = XposedHelpers.callMethod(it.args[0], "getTitle") as CharSequence?
//                    if (title != null) {
//                        Log.i("updateActionBar $title")
//                        val text = SpannableString(if (title is SpannableString) title.toString() else title);
//                        text.setSpan(ForegroundColorSpan(Color.BLACK), 0, text.length, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
//                        XposedHelpers.callMethod(it.args[0], "setTitle", text)
//                    }
//
//                }
            findMethod(
                findClass(
                    "com.android.messaging.ui.BugleActionBarActivity", lpparam.classLoader
                )
            ) { name == "onCreateOptionsMenu" }.hookAfter() {
                val count = XposedHelpers.callMethod(it.args[0], "size") as Int
                for (i in 0..count - 1) {
                    val menuItem =
                        (XposedHelpers.callMethod(it.args[0], "getItem", i) as MenuItem)
                    tintMenuIcon(menuItem, Color.BLACK)
                }
//                    it.result = false
            }
//                findMethod(
//                    findClass(
//                        "com.android.messaging.ui.conversation.ConversationFragment", lpparam.classLoader
//                    )
//                ) { name == "updateActionBar" }.hookAfter() {
//                    Log.i("ConversationFragment updateActionBar")
//                    XposedHelpers.callMethod(it.args[0], "setBackgroundDrawable", ColorDrawable(Color.WHITE))
//                    val title = XposedHelpers.callMethod(it.args[0], "getTitle") as CharSequence?
//                    if (title != null) {
//                        val text = SpannableString(if (title is SpannableString) title.toString() else title);
//                        text.setSpan(ForegroundColorSpan(Color.BLACK), 0, text.length, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
//                        XposedHelpers.callMethod(it.args[0], "setTitle", text)
//                    }
//
//                }
//                findMethod(
//                    findClass(
//                        "com.android.messaging.ui.conversation.ConversationFragment", lpparam.classLoader
//                    )
//                ) { name == "updateActionAndStatusBarColor" }.hookBefore() {
//                    (XposedHelpers.callMethod(it.thisObject, "getActivity") as Activity).window?.statusBarColor = 0
//                    it.result = false
//                }
            findMethod(
                findClass(
                    "com.android.messaging.ui.conversation.ConversationFragment",
                    lpparam.classLoader
                )
            ) { name == "onCreateOptionsMenu" }.hookAfter() {
                val count = XposedHelpers.callMethod(it.args[0], "size") as Int
                for (i in 0..count - 1) {
                    val menuItem =
                        (XposedHelpers.callMethod(it.args[0], "getItem", i) as MenuItem)
                    tintMenuIcon(menuItem, Color.BLACK)
                }
//                    (XposedHelpers.callMethod(it.thisObject, "getActivity") as Activity).window?.statusBarColor = 0
//                    it.result = false
            }
            findMethod(
                findClass(
                    "com.android.messaging.ui.conversation.ConversationMessageView",
                    lpparam.classLoader
                )
            ) { name == "updateTextAppearance" }.hookAfter {
                val blackColor = Color.BLACK
                XposedHelpers.callMethod(
                    XposedHelpers.getObjectField(
                        it.thisObject,
                        "mStatusTextView"
                    ), "setTextColor", blackColor
                )
                XposedHelpers.callMethod(
                    XposedHelpers.getObjectField(
                        it.thisObject,
                        "mSubjectLabel"
                    ), "setTextColor", blackColor
                )
                XposedHelpers.callMethod(
                    XposedHelpers.getObjectField(
                        it.thisObject,
                        "mSenderNameTextView"
                    ), "setTextColor", blackColor
                )
                XposedHelpers.callMethod(
                    XposedHelpers.getObjectField(
                        it.thisObject,
                        "mMessageTextView"
                    ), "setTextColor", blackColor
                )
                XposedHelpers.callMethod(
                    XposedHelpers.getObjectField(
                        it.thisObject,
                        "mMessageTextView"
                    ), "setLinkTextColor", blackColor
                )
                XposedHelpers.callMethod(
                    XposedHelpers.getObjectField(
                        it.thisObject,
                        "mSubjectText"
                    ), "setTextColor", blackColor
                )
            }
        }
        if (lpparam.packageName == "android") {
            //            Log.i("patching  android" + lpparam.packageName + " " + mPowerManager + " " + mAlarmService + " " + enableLightIntent)

            findMethod(
                findClass(
                    "com.android.server.policy.PhoneWindowManager", lpparam.classLoader
                )
            ) { name == "interceptKeyBeforeQueueing" }.hookBefore {
                if (mPhoneWindowManager == null) {
                    //                        Log.i("interceptKeyBeforeQueueing  init")
                    mPhoneWindowManager = it.thisObject
                    mPhoneWindowManagerHandler = XposedHelpers.getObjectField(
                        mPhoneWindowManager, "mHandler"
                    ) as Handler?
                    mInputManager =
                        XposedHelpers.getObjectField(mPhoneWindowManager, "mInputManager")
                    mPowerManager =
                        appContext.getSystemService(Context.POWER_SERVICE) as PowerManager
                    mVolumeWakeLock =
                        mPowerManager!!.newWakeLock(268435462, "Sys::VolumeWakeLock")
                    val intentFilter = IntentFilter()
                    intentFilter.addAction("com.akylas.A9_REFRESH_SCREEN")
                    intentFilter.addAction("com.akylas.A9_SLEEP_SCREEN")
                    appContext.registerReceiver(intentFilter) { intent ->
                            if (intent?.action == "com.akylas.A9_REFRESH_SCREEN") {
                                val prefs = Preferences()
                                val delay = prefs.getInt("eink_button_sleep_delay", 4000)
                                val cleanup_delay = prefs.getInt("volume_key_cleanup_delay", 1400)
                                refreshScreen(intent.getIntExtra("sleep_delay", delay), cleanup_delay)
                            }
                            else if (intent?.action == "com.akylas.A9_SLEEP_SCREEN") {
                                sleepScreen()
                            }
                        }
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
            ) { name == "handleMessage" }.hookBefore {
                val message = it.args[0] as Message
                val what = message.what

                if (what == 595) {
                    //                        Log.i("handleMessage 595")
                    sleepScreen()
                    it.result = true
                } else if (what == 596) {
                    //                        Log.i("handleMessage 596")
                    // fix backlight
                    try {
                        val str = SystemProperties.get("sys.linevibrator_touch");
                        val i = Integer.parseInt(str);
                        if (i < 0) {
                            val stringBuilder = StringBuilder();
                            stringBuilder.append(0 - i);
                            //                                Log.i("sys.linevibrator_touch " + stringBuilder.toString())
                            SystemProperties.set(
                                "sys.linevibrator_touch", stringBuilder.toString()
                            );
                        }
                    } finally {
                    }
                    it.result = true
                } else if (what == 600) {
                    //                        Log.i("handleMessage 600")
                    sendPastKeyDownEvent()
                    it.result = true
                } else if (what == 601) {
                    //                        Log.i("handleMessage 601")
                    sendPastKeyUpEvent()
                    it.result = true
                }
            }
        }

        val PhoneWindowClass = XposedHelpers.findClassIfExists(
            "com.android.internal.policy.PhoneWindow", lpparam.classLoader
        )
        if (PhoneWindowClass != null) {
            findMethod(PhoneWindowClass) { name == "setNavigationBarColor" }.hookBefore {
//                    Log.i("setNavigationBarColor " + it.args[0])
                it.args[0] = 0xFFFFFFFF.toInt()
            }
            findMethod(PhoneWindowClass) { name == "setNavigationBarDividerColor" }.hookBefore {
//                    Log.i("setNavigationBarDividerColor " + it.args[0])
                it.args[0] = -1;
            }
            findMethod(PhoneWindowClass) { name == "setStatusBarColor" }.hookBefore {
//                    Log.i("setStatusBarColor " + it.args[0])
                it.args[0] = 0xFFFFFFFF.toInt()
            }
            findMethod(PhoneWindowClass) { name == "generateLayout" }.hookAfter() {
                // Here still sets the light bar flags via setSystemUiVisibility (even it is deprecated) to
                // make the light bar state be able to be read from the legacy method.
                val decor = it.args[0] as View
                decor.systemUiVisibility =
                    decor.systemUiVisibility or (View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR)
                decor.windowInsetsController?.setSystemBarsAppearance(
                    APPEARANCE_LIGHT_STATUS_BARS or APPEARANCE_LIGHT_NAVIGATION_BARS,
                    APPEARANCE_LIGHT_STATUS_BARS or APPEARANCE_LIGHT_NAVIGATION_BARS
                )
                XposedHelpers.setIntField(
                    it.thisObject, "mNavigationBarColor", 0xFFFFFFFF.toInt()
                )
                XposedHelpers.setIntField(
                    it.thisObject, "mStatusBarColor", 0xFFFFFFFF.toInt()
                )
                XposedHelpers.setIntField(it.thisObject, "mNavigationBarDividerColor", -1)
                Log.i("generateLayout")
                val params =(XposedHelpers.callMethod(
                    it.thisObject,
                    "getAttributes"
                ) as WindowManager.LayoutParams)
                params.flags = params.flags and (WindowManager.LayoutParams.FLAG_DIM_BEHIND).inv()
                params.dimAmount = 0.0F
            }
        }

    }
}
