package com.akylas.hisensea9.utils;

import android.annotation.SuppressLint;
import android.text.TextUtils;

import de.robv.android.xposed.XposedHelpers;

@SuppressLint("PrivateApi")
public class SystemProperties {
    private SystemProperties() {}

    private static Class<?> PROPS = null;

    static {
        try {
            PROPS = Class.forName("android.os.SystemProperties");
        } catch (Throwable ignored) {}
    }

    public static String get(String key) {
        return (String) XposedHelpers.callStaticMethod(PROPS, "get", key);
    }
    public static String set(String key, String value) {
        return (String) XposedHelpers.callStaticMethod(PROPS, "set", key, value);
    }

}