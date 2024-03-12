package xyz.hyli.connect.utils

import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import android.text.TextUtils
import android.widget.Toast
import androidx.core.content.ContextCompat
import rikka.shizuku.Shizuku
import xyz.hyli.connect.HyLiConnect
import xyz.hyli.connect.R
import xyz.hyli.connect.datastore.PreferencesDataStore
import java.util.concurrent.CompletableFuture

object PermissionUtils {
    var shizukuPermissionFuture = CompletableFuture<Boolean>()
    fun checkShizukuPermission(context: Context): Boolean {
        var toast: Toast? = null
        val b = if (!Shizuku.pingBinder()) {
            toast = Toast.makeText(context,
                ContextCompat.getString(context, R.string.toast_shizuku_not_available), Toast.LENGTH_LONG)
            false
        } else if (Shizuku.isPreV11()) {
            toast = Toast.makeText(context,
                ContextCompat.getString(context, R.string.toast_shizuku_not_support), Toast.LENGTH_LONG)
            false
        } else if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            true
        } else if (Shizuku.shouldShowRequestPermissionRationale()) {
            toast = Toast.makeText(
                context,
                ContextCompat.getString(context, R.string.toast_shizuku_denied),
                Toast.LENGTH_LONG
            )
            false
        } else {
            Shizuku.requestPermission(HyLiConnect.SHIZUKU_CODE)

            val result = shizukuPermissionFuture.get()
            shizukuPermissionFuture = CompletableFuture<Boolean>()

            result
        }
        if (PreferencesDataStore.function_app_streaming.getBlocking()!! && PreferencesDataStore.working_mode.getBlocking()!! in 1..2) {
            toast?.show()
        }
        return b
    }
    // 无障碍权限
    fun isAccessibilitySettingsOn(context: Context): Boolean {
        var accessibilityEnabled = 0
        val service = context.packageName + "/xyz.hyli.connect.service.KeepAliveService"
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                context.applicationContext.contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED
            )
        } catch (e: Settings.SettingNotFoundException) {
        }
        val mStringColonSplitter = TextUtils.SimpleStringSplitter(':')
        if (accessibilityEnabled == 1) {
            val settingValue = Settings.Secure.getString(
                context.applicationContext.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            if (settingValue != null) {
                mStringColonSplitter.setString(settingValue)
                while (mStringColonSplitter.hasNext()) {
                    val accessibilityService = mStringColonSplitter.next()
                    if (accessibilityService.equals(service, ignoreCase = true)) {
                        return true
                    }
                }
            }
        }
        return false
    }
}
