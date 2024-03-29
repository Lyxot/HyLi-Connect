package xyz.hyli.connect.ui.test

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import xyz.hyli.connect.R
import xyz.hyli.connect.bean.ApplicationInfo
import xyz.hyli.connect.utils.PackageUtils

class AppListAdapter(private val context: Context, private val appList: List<ApplicationInfo>) :
    ArrayAdapter<ApplicationInfo?>(context, 0, appList) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.applist_item, parent, false)
        }
        val packageManager = context.packageManager

        val applicationInfo = appList[position]
        // 获取应用的图标和名称
        val packageName = applicationInfo.packageName
        var appName = applicationInfo.appName
        var appIcon: Drawable? = applicationInfo.icon?.let {
            PackageUtils.bitmapToDrawable(context, PackageUtils.byteStringToBitmap(it))
        }
        //        Log.i("xyz.hyli.connect.ui.test.AppListAdapter", "packageName: " + packageName);

        // 显示应用的图标和名称
        if (appIcon == null) {
            appIcon = context.getDrawable(android.R.drawable.sym_def_app_icon)
        }
        if (appName == null) {
            appName = packageName
        }
        val iconView = convertView!!.findViewById<ImageView>(R.id.appIcon)
        val nameView = convertView.findViewById<TextView>(R.id.appName)
        iconView.setImageDrawable(appIcon)
        nameView.text = appName
        return convertView
    }
}
