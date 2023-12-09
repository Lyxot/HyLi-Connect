package xyz.hyli.connect.utils

import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.PixelFormat
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.UserHandle
import android.util.Log
import androidx.annotation.RequiresApi
import com.github.promeg.pinyinhelper.Pinyin
import xyz.hyli.connect.BuildConfig


object PackageUtils {
    var packageList: List<String> = listOf()
    var appNameMap: MutableMap<String, String> = mutableMapOf()
    var appIconMap: MutableMap<String, AppIcon> = mutableMapOf()
    data class AppIcon(
        val icon: Drawable,
        val width: Int,
        val height: Int,
        val iconByteArray: ByteArray
    )
    //获取应用列表
    fun GetAppList(packageManager: PackageManager): List<String> {
        val intent = Intent()
        intent.action = Intent.ACTION_MAIN
        intent.addCategory(Intent.CATEGORY_LAUNCHER)

        val resolveInfos: List<ResolveInfo> = packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)

        for (info in resolveInfos) {
            val packageName = info.activityInfo.packageName
            val appName = info.loadLabel(packageManager).toString()
            if ( hasInstallThisPackage(packageName, packageManager) ) {
                if ( packageName != BuildConfig.APPLICATION_ID ) {
                    appNameMap.put(packageName, appName)
                    val icon = info.loadIcon(packageManager)
                    appIconMap.put(packageName, AppIcon(icon, icon.intrinsicWidth, icon.intrinsicHeight, drawalbeToByte(info.loadIcon(packageManager))))
                }
            }
        }
        appNameMap = appNameMap.toList().sortedBy { (_, value) -> getPinYin(value) }.toMap().toMutableMap()
        packageList = appNameMap.toList().map { (key, _) -> key }
//        Log.i("GetAppList", launcherIconPackageList.toString())
        return packageList
    }
    private fun getPinYin(content: String): String {
        val stringBuilder = StringBuilder()
        stringBuilder.setLength(0)
        for (i in content) {
            stringBuilder.append(Pinyin.toPinyin(i))
        }
        return stringBuilder.toString().toLowerCase()
    }
    //判断是否安装了这个应用
    fun hasInstallThisPackage(packageName: String, packageManager: PackageManager): Boolean {
        return try {
            packageManager.getApplicationInfo(packageName, 0)
            true
        } catch (e: Exception) {
            false
        }
    }
    //判断某个用户是否安装了这个应用
    @RequiresApi(Build.VERSION_CODES.O)
    fun hasInstallThisPackageWithUserId(
        packageName: String,
        launcherApps: LauncherApps,
        userHandle: UserHandle
    ): Boolean {
        return try {
            launcherApps.getApplicationInfo(packageName, 0, userHandle)
            true
        } catch (e: Exception) {
            false
        }
    }
    //获取主Activity
    fun GetMainActivityName(packageManager: PackageManager, packageName: String): String {
        var mainActivityName: String = ""
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        val ac = intent?.component
        val classname = ac?.getClassName()
        if (classname != null) {
            if (classname.isNotEmpty()) {
                mainActivityName = classname
            }
        }
        return mainActivityName
    }

    fun drawalbeToByte(drawable: Drawable): ByteArray {
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            if (drawable.opacity !== PixelFormat.OPAQUE) Bitmap.Config.ARGB_8888 else Bitmap.Config.RGB_565
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        val stream = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }
    fun byteToDrawable(byteArray: ByteArray): Drawable {
        val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
        return BitmapDrawable(bitmap)
    }
}