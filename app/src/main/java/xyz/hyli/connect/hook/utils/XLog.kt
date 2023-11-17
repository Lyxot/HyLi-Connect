package xyz.hyli.connect.hook.utils

import de.robv.android.xposed.XposedBridge

object XLog {
    fun d(s: Any) {
        XposedBridge.log("[HyliConnect/D] $s")
    }
}