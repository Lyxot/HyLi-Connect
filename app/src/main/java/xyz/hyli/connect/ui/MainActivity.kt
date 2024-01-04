package xyz.hyli.connect.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku
import xyz.hyli.connect.R
import xyz.hyli.connect.bean.ServiceState
import xyz.hyli.connect.service.SocketService
import xyz.hyli.connect.socket.SocketData
import xyz.hyli.connect.ui.navigation.compactScreen
import xyz.hyli.connect.ui.navigation.expandedScreen
import xyz.hyli.connect.ui.navigation.mediumScreen
import xyz.hyli.connect.ui.state.HyliConnectState
import xyz.hyli.connect.ui.theme.HyliConnectTheme
import xyz.hyli.connect.utils.PackageUtils
import xyz.hyli.connect.utils.ServiceUtils
import java.util.concurrent.CompletableFuture

class MainActivity: ComponentActivity() {
    private val SHIZUKU_CODE = 0x3CE9A
    private var shizukuPermissionFuture = CompletableFuture<Boolean>()
    private var appList: Deferred<List<String>>? = null
    private lateinit var viewModel: HyliConnectViewModel
    private lateinit var localBroadcastManager: LocalBroadcastManager
    private val observeVariableThread = Thread {
        while (true) {
            try {
                SocketData.uuidMap.forEach {
                    if ( viewModel.nsdDeviceMap.containsKey(it.value) && SocketData.deviceInfoMap.containsKey(it.value) && viewModel.connectedDeviceMap.containsKey(it.value).not() ) {
                        viewModel.connectedDeviceMap[it.value] = SocketData.deviceInfoMap[it.value]!!
                        viewModel.nsdDeviceMap.remove(it.value)
                        viewModel.connectDeviceVisibilityMap[it.value]!!.value = false
                    } else if ( SocketData.deviceInfoMap.containsKey(it.value) && viewModel.connectedDeviceMap.containsKey(it.value).not() ) {
                        viewModel.connectedDeviceMap[it.value] = SocketData.deviceInfoMap[it.value]!!
                        viewModel.connectDeviceVisibilityMap[it.value]!!.value = false
                    }
                }
            } catch (_: Exception) { }
            try {
                viewModel.connectedDeviceMap.keys.toMutableList().forEach {
                    if ( SocketData.deviceInfoMap.containsKey(it).not() ) {
                        viewModel.connectedDeviceMap.remove(it)
                        viewModel.connectDeviceVisibilityMap.remove(it)
                    }
                }
            } catch (_: Exception) { }
            try {
                viewModel.updateApplicationState()
            } catch (_: Exception) { }
            Thread.sleep(1000)
        }
    }
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class, DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.navigationBarColor = Color.TRANSPARENT
        window.statusBarColor = Color.TRANSPARENT
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()

        startForegroundService(Intent(this, SocketService::class.java))

        GlobalScope.launch(Dispatchers.IO) {
            appList = async { PackageUtils.GetAppList(packageManager) }
        }

        HyliConnectState.serviceStateMap["SocketService"] = if (ServiceUtils.isServiceWork(this, "xyz.hyli.connect.service.SocketService")) {
            ServiceState("running", getString(R.string.state_service_running, getString(R.string.service_socket_service)))
        } else {
            ServiceState("stopped", getString(R.string.state_service_stopped, getString(R.string.service_socket_service)))
        }
        viewModel = ViewModelProvider(this, HyliConnectViewModelFactory()).get(HyliConnectViewModel::class.java)
        localBroadcastManager = LocalBroadcastManager.getInstance(this)
        viewModel.localBroadcastManager.value = localBroadcastManager

        Shizuku.addRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == SHIZUKU_CODE) {
                val granted = grantResult == PackageManager.PERMISSION_GRANTED
                shizukuPermissionFuture.complete(granted)
            }
        }
        if (ConfigHelper.getConfigMap(this)["is_stream"] == true && (ConfigHelper.getConfigMap(this)["stream_method"] == "Shizuku" || ConfigHelper.getConfigMap(this)["refuse_fullscreen_method"] == "Shizuku")) {
            HyliConnectState.permissionStateMap["Shizuku"] = checkShizukuPermission()
        }

        setContent {
            HyliConnectTheme {
                val widthSizeClass = calculateWindowSizeClass(this).widthSizeClass
//                TestScreen()
                MainScreen(widthSizeClass, viewModel)
            }
        }
        if (observeVariableThread.isAlive.not()) {
            observeVariableThread.start()
        }
    }
    private fun checkShizukuPermission(): Boolean {
        val b = if (!Shizuku.pingBinder()) {
            Toast.makeText(this, "Shizuku is not available", Toast.LENGTH_LONG).show()
            false
        } else if (Shizuku.isPreV11()) {
            Toast.makeText(this, "Shizuku < 11 is not supported!", Toast.LENGTH_LONG).show()
            false
        } else if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            true
        } else if (Shizuku.shouldShowRequestPermissionRationale()) {
            Toast.makeText(
                this,
                "You denied the permission for Shizuku. Please enable it in app.",
                Toast.LENGTH_LONG
            ).show()
            false
        } else {
            Shizuku.requestPermission(SHIZUKU_CODE)

            val result = shizukuPermissionFuture.get()
            shizukuPermissionFuture = CompletableFuture<Boolean>()

            result
        }

        return b
    }
}

@Composable
private fun MainScreen(widthSizeClass: WindowWidthSizeClass, viewModel: HyliConnectViewModel) {
    val currentSelect = remember { mutableStateOf(0) }
    val navController = rememberNavController()
    when (widthSizeClass) {
        WindowWidthSizeClass.Compact -> { compactScreen(viewModel,navController,currentSelect) }
        WindowWidthSizeClass.Medium -> { mediumScreen(viewModel,navController,currentSelect) }
        WindowWidthSizeClass.Expanded -> { expandedScreen(viewModel,navController,currentSelect) }
        else -> { compactScreen(viewModel,navController,currentSelect) }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    MainScreen(WindowWidthSizeClass.Compact,HyliConnectViewModel())
}