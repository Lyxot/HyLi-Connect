package xyz.hyli.connect.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.PixelFormat
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import com.google.protobuf.ByteString
import com.google.protobuf.kotlin.toByteString
import org.ks.chan.c2pinyin.LetterCase
import org.ks.chan.c2pinyin.pinyin
import xyz.hyli.connect.BuildConfig
import xyz.hyli.connect.bean.ApplicationInfo
import java.util.concurrent.ConcurrentHashMap

object PackageUtils {
    fun getPackageMap(packageManager: PackageManager): Map<String, ResolveInfo> = mutableMapOf<String, ResolveInfo>().let {
        packageManager.queryIntentActivities(
            Intent().apply {
                action = Intent.ACTION_MAIN
                addCategory(Intent.CATEGORY_LAUNCHER)
            },
            PackageManager.MATCH_ALL
        ).forEach { resolveInfo ->
            resolveInfo.activityInfo.packageName?.let { packageName ->
                if (packageName != BuildConfig.APPLICATION_ID) {
                    it[packageName] = resolveInfo
                }
            }
        }
        it
    }

    fun getAppInfo(packageManager: PackageManager, resolveInfo: ResolveInfo, returnIcon: Boolean = false): ApplicationInfo = ApplicationInfo(
        resolveInfo.activityInfo.packageName,
        resolveInfo.loadLabel(packageManager).toString(),
        packageManager.getPackageInfo(resolveInfo.activityInfo.packageName, 0).versionName,
        getMainActivityName(packageManager, resolveInfo.activityInfo.packageName),
        if (returnIcon) {
            drawableToByteString(resolveInfo.loadIcon(packageManager))
        } else null
    )

    // 获取应用列表
    fun getAppList(context: Context, returnIcon: Boolean = false, sortByName: Boolean = false): List<ApplicationInfo> {
        val packageManager = context.packageManager

        val resolveInfos = getPackageMap(packageManager)
        val appMap = ConcurrentHashMap<String, ApplicationInfo>()

        resolveInfos.forEach { (_, resolveInfo) ->
            appMap[resolveInfo.loadLabel(packageManager).toString()] = getAppInfo(packageManager, resolveInfo, returnIcon)
        }
        return if (sortByName) appMap.toList().sortedBy { (value, _) -> value.pinyin(LetterCase.Lower).joinToString(" ").lowercase() }.toMap().toList().map { (_, key) -> key }
        else appMap.toList().map { (_, key) -> key }
    }

    // 获取主Activity
    fun getMainActivityName(packageManager: PackageManager, packageName: String): String {
        var mainActivityName: String = ""
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        val ac = intent?.component
        val classname = ac?.className
        if (classname != null) {
            if (classname.isNotEmpty()) {
                mainActivityName = classname
            }
        }
        return mainActivityName
    }

    private fun drawableToByteString(drawable: Drawable): ByteString {
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            if (drawable.opacity !== PixelFormat.OPAQUE) Bitmap.Config.ARGB_8888 else Bitmap.Config.RGB_565
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        val stream = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.WEBP, 100, stream)
        return stream.toByteArray().toByteString()
    }
    fun byteStringToBitmap(byteString: ByteString): Bitmap {
        val byteArray = byteString.toByteArray()
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    }
    fun bitmapToDrawable(context: Context, bitmap: Bitmap): Drawable {
        return BitmapDrawable(context.resources, bitmap)
    }
}
