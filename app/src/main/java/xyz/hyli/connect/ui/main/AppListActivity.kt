package xyz.hyli.connect.ui.main

import xyz.hyli.connect.ui.applist.AppListAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.ListView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import rikka.shizuku.Shizuku
import xyz.hyli.connect.R
import xyz.hyli.connect.utils.PackageUtils
import xyz.hyli.connect.utils.ShellUtils
import xyz.hyli.connect.utils.VirtualDisplayUtils
import java.util.concurrent.CompletableFuture


class AppListActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_app_list)
        var listView = findViewById<ListView>(R.id.applist)
        val appList: MutableList<PackageUtils.AppInfo> = PackageUtils.GetAppList(packageManager)
        val adapter = AppListAdapter(this, appList)
        listView.adapter = adapter
        listView.setOnItemClickListener { parent, view, position, id ->
            val appInfo = appList[position]
            val packageName = appInfo.packageName
            val mainActivityName = appInfo.mainActivityName
            val displayID = VirtualDisplayUtils(this).createDisplay(packageName, 720, 1440, this.resources.displayMetrics.densityDpi)
            ShellUtils.execCommand("am start --display $displayID -n $packageName/$mainActivityName", "Root")
            val intent = Intent(this, DisplayActivity::class.java)
            intent.putExtra("displayID", displayID)
            startActivity(intent)
        }
    }
}
