package xyz.hyli.connect.utils

import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.UserHandle
import android.util.Log
import androidx.annotation.RequiresApi
import com.github.promeg.pinyinhelper.Pinyin


object PackageUtils {
    data class AppInfo(
        val packageName: String,
        val appName: String,
        val mainActivityName: String,
        val appIcon: Drawable
    )
    //获取应用列表
    fun GetAppList(packageManager: PackageManager): MutableList<AppInfo> {
        val intent = Intent()
        intent.action = Intent.ACTION_MAIN
        intent.addCategory(Intent.CATEGORY_LAUNCHER)

        val resolveInfos: List<ResolveInfo> = packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        var launcherIconPackageList = mutableListOf<AppInfo>()

        for (info in resolveInfos) {
            if ( hasInstallThisPackage(info.activityInfo.packageName, packageManager) ) {
                launcherIconPackageList.add(
                    AppInfo(
                        info.activityInfo.packageName,
                        info.loadLabel(packageManager).toString(),
                        GetMainActivityName(packageManager, info.activityInfo.packageName),
                        info.loadIcon(packageManager)
                    )
                )
            }
        }
        launcherIconPackageList = launcherIconPackageList.filter { it.packageName != "xyz.hyli.connect" }.toMutableList()
//        launcherIconPackageList = launcherIconPackageList.sortedWith { o1, o2 -> Collator.getInstance(Locale.CHINESE).compare(o1.appName, o2.appName) }.toMutableList()
        launcherIconPackageList = launcherIconPackageList.sortedBy { getPinYin(it.appName) }.toMutableList()
        Log.i("GetAppList", launcherIconPackageList.toString())
        return launcherIconPackageList
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
}