package xyz.hyli.connect.utils

import android.content.Context
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
import android.os.UserHandle
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toLowerCase
import com.google.protobuf.ByteString
import com.google.protobuf.kotlin.toByteString
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.ks.chan.c2pinyin.LetterCase
import org.ks.chan.c2pinyin.pinyin
import xyz.hyli.connect.BuildConfig
import xyz.hyli.connect.bean.ApplicationInfo
import xyz.hyli.connect.bean.ApplicationIcon
import java.util.concurrent.ConcurrentHashMap

object PackageUtils {

    // 获取应用列表
    @OptIn(DelicateCoroutinesApi::class)
    fun getAppList(context: Context, returnIcon: Boolean = false): List<ApplicationInfo> {
        val packageManager = context.packageManager
        val intent = Intent()
        intent.action = Intent.ACTION_MAIN
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        val resolveInfos: List<ResolveInfo> = packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)

        val appMap = ConcurrentHashMap<String, ApplicationInfo>()
        val set = mutableSetOf<String>()

        for (info in resolveInfos) {
            val packageName = info.activityInfo.packageName
            if (packageName != BuildConfig.APPLICATION_ID) {
                set.add(packageName)
                GlobalScope.launch {
                    val appName = info.loadLabel(packageManager).toString()
                    val icon = info.loadIcon(packageManager)
                    var iconByteArray: ApplicationIcon? = null
                    if (returnIcon) iconByteArray = ApplicationIcon(icon.intrinsicWidth, icon.intrinsicHeight, drawableToByteString(icon))
                    appMap[appName] = ApplicationInfo(
                        packageName,
                        appName,
                        packageManager.getPackageInfo(packageName, 0).versionName,
                        getMainActivityName(packageManager, packageName),
                        iconByteArray
                    )
                    set.remove(packageName)
                }
            }
        }
        while (set.isNotEmpty()) {
            Thread.sleep(20)
        }
        return appMap.toList().sortedBy { (value, _) -> value.pinyin(LetterCase.Lower).joinToString(" ").lowercase() }.toMap().toList().map { (_, key) -> key }
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
