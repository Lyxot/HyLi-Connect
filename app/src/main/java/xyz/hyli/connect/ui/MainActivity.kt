package xyz.hyli.connect.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import xyz.hyli.connect.service.SocketService
import xyz.hyli.connect.ui.navigation.compactScreen
import xyz.hyli.connect.ui.navigation.expandedScreen
import xyz.hyli.connect.ui.navigation.mediumScreen
import xyz.hyli.connect.ui.theme.HyliConnectTheme
import xyz.hyli.connect.utils.PackageUtils

class MainActivity: ComponentActivity() {
    private var appList: Deferred<List<String>>? = null
    var IP_ADDRESS: Deferred<String>? = null
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.navigationBarColor = Color.TRANSPARENT
        enableEdgeToEdge()

        startForegroundService(Intent(this, SocketService::class.java))

        GlobalScope.launch(Dispatchers.IO) {
            IP_ADDRESS = async { ConfigHelper().getIPAddress(this@MainActivity) }
            appList = async { PackageUtils.GetAppList(packageManager) }
        }

        setContent {
            HyliConnectTheme {
                val widthSizeClass = calculateWindowSizeClass(this).widthSizeClass
//                TestScreen()
                MainScreen(widthSizeClass)
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainScreen(widthSizeClass: WindowWidthSizeClass) {
    val currentSelect = remember { mutableStateOf(0) }
    val navController = rememberAnimatedNavController()
    when (widthSizeClass) {
        WindowWidthSizeClass.Compact -> { compactScreen(navController,currentSelect) }
        WindowWidthSizeClass.Medium -> { mediumScreen(navController,currentSelect) }
        WindowWidthSizeClass.Expanded -> { expandedScreen(navController,currentSelect) }
        else -> { compactScreen(navController,currentSelect) }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MainScreen(widthSizeClass = WindowWidthSizeClass.Compact)
}