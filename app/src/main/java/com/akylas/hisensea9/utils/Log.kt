package com.akylas.hisensea9

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.akylas.hisensea9.lsposed.BuildConfig
import de.robv.android.xposed.XposedBridge
import java.lang.ref.WeakReference

object Log {
  private var lastToast: WeakReference<Toast>? = null

  fun i(msg: String) {
    Log.i(TAG, msg)
    XposedBridge.log("A9Lsposed logging: " + msg)
  }

  fun d(msg: String, full: Boolean = false) {
    if (BuildConfig.DEBUG) {
      if (!full && msg.length > 300) {
        Log.d(TAG, msg.take(300) + " ...")
      } else {
        Log.d(TAG, msg)
      }
    }
  }

  fun w(msg: String) {
    Log.w(TAG, msg)
  }

  fun e(msg: String) {
    Log.e(TAG, msg)
    XposedBridge.log("A9Lsposed error: " + msg)
  }

  fun ex(thr: Throwable) {
    Log.e(TAG, "", thr)
    XposedBridge.log("A9Lsposed backtrace: " + thr.toString())
  }

  fun toast(context: Context, msg: String) {
    this.lastToast?.get()?.cancel()
    val duration = Toast.LENGTH_SHORT
    val toast = Toast.makeText(context, msg, duration)
    toast.show()
    this.lastToast = WeakReference(toast)
  }
}
