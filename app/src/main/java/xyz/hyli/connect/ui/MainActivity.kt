package xyz.hyli.connect.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import xyz.hyli.connect.bean.DeviceInfo
import xyz.hyli.connect.service.SocketService
import xyz.hyli.connect.socket.SocketData
import xyz.hyli.connect.ui.navigation.compactScreen
import xyz.hyli.connect.ui.navigation.expandedScreen
import xyz.hyli.connect.ui.navigation.mediumScreen
import xyz.hyli.connect.ui.theme.HyliConnectTheme
import xyz.hyli.connect.utils.NetworkUtils
import xyz.hyli.connect.utils.PackageUtils
import kotlin.concurrent.thread

class MainActivity: ComponentActivity() {
    private var appList: Deferred<List<String>>? = null
    private lateinit var viewModel: HyliConnectViewModel
    private val connectDeviceThread = Thread {
        while (true) {
            try {
                SocketData.uuidMap.forEach {
                    if ( viewModel.nsdDeviceMap.containsKey(it.value) && viewModel.connectedDeviceMap.containsKey(it.value).not() ) {
                        viewModel.connectedDeviceMap[it.value] = viewModel.nsdDeviceMap[it.value]!!
                        viewModel.nsdDeviceMap.remove(it.value)
                        viewModel.connectDeviceVisibilityMap[it.value]!!.value = false
                    } else if ( SocketData.deviceInfoMap.containsKey(it.value) && viewModel.connectedDeviceMap.containsKey(it.value).not() ) {
                        viewModel.connectedDeviceMap[it.value] = SocketData.deviceInfoMap[it.value]!!
                        viewModel.connectDeviceVisibilityMap[it.value]!!.value = false
                    }
                }
                val uuid_list = viewModel.connectedDeviceMap.keys.toMutableList()
                uuid_list.forEach {
                    if ( SocketData.deviceInfoMap.containsKey(it).not() ) {
                        viewModel.connectedDeviceMap.remove(it)
                        viewModel.connectDeviceVisibilityMap.remove(it)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
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

        viewModel = ViewModelProvider(this, HyliConnectViewModelFactory()).get(HyliConnectViewModel::class.java)
        setContent {
            HyliConnectTheme {
                val widthSizeClass = calculateWindowSizeClass(this).widthSizeClass
//                TestScreen()
                MainScreen(widthSizeClass, viewModel)
            }
        }
        if (connectDeviceThread.isAlive.not()) {
            connectDeviceThread.start()
        }
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