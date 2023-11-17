package xyz.hyli.connect.ui.main

import AppListAdapter
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.widget.ListView
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import rikka.shizuku.Shizuku
import xyz.hyli.connect.R
import xyz.hyli.connect.utils.PackageUtils


class MainActivity : ComponentActivity() {
    private val binding: MainActivityBinding? = null
    private val BINDER_RECEIVED_LISTENER = Shizuku.OnBinderReceivedListener {
        if (Shizuku.isPreV11()) {
            binding.text1.setText("Shizuku pre-v11 is not supported")
        } else {
            binding.text1.setText("Binder received")
        }
    }
    private val BINDER_DEAD_LISTENER = OnBinderDeadListener {
        binding.text1.setText(
            "Binder dead"
        )
    }
    private val REQUEST_PERMISSION_RESULT_LISTENER: OnRequestPermissionResultListener = this::onRequestPermissionsResult

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        var listView = findViewById<ListView>(R.id.app_list)
        val appList: MutableList<PackageUtils.AppInfo> = PackageUtils.GetAppList(packageManager)
        val adapter = AppListAdapter(this, appList)
        listView.adapter = adapter
        listView.setOnItemClickListener { parent, view, position, id ->
            val appInfo = appList[position]
            val packageName = appInfo.packageName
            val mainActivityName = appInfo.mainActivityName
            val intent = Intent()
            intent.component = ComponentName(packageName, mainActivityName)
            startActivity(intent)

        }


//        setContent {
//            HyliConnectTheme() {
//                // A surface container using the 'background' color from the theme
//                Surface(
//                    modifier = Modifier.fillMaxSize(),
//                    color = MaterialTheme.colorScheme.background
//                ) {
//                    Greeting(1)
//                }
//            }
//        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "$name",
        modifier = modifier
    )
}

