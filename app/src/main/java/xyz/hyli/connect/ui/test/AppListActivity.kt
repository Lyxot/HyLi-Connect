package xyz.hyli.connect.ui.test

import android.content.Intent
import android.os.Bundle
import android.widget.ListView
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import xyz.hyli.connect.HyliConnect
import xyz.hyli.connect.R
import xyz.hyli.connect.utils.PackageUtils
import xyz.hyli.connect.utils.VirtualDisplayUtils

class AppListActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_app_list)
        var listView = findViewById<ListView>(R.id.applist)
        val appList: List<String> = intent.getStringArrayExtra("appList")!!.toList()
        val adapter = AppListAdapter(this, appList)
        listView.adapter = adapter
        listView.setOnItemClickListener { parent, view, position, id ->
            val packageName = appList[position]
            val mainActivityName = PackageUtils.GetMainActivityName(packageManager, packageName)
            val displayID = VirtualDisplayUtils(
                this
            ).createDisplay(packageName, 720, 1440, this.resources.displayMetrics.densityDpi)
            HyliConnect.me.getControlService()
                ?.execShell("am start --display $displayID -n $packageName/$mainActivityName", false)
//            ShellUtils.execCommand("am start --display $displayID -n $packageName/$mainActivityName", false)
            val intent = Intent(this, DisplayActivity::class.java)
            intent.putExtra("displayID", displayID)
            startActivity(intent)
        }
    }
}
