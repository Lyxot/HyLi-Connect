package xyz.hyli.connect.ui.pages

import android.content.Context
import android.content.Intent
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import compose.icons.CssGgIcons
import compose.icons.LineAwesomeIcons
import compose.icons.cssggicons.GlobeAlt
import compose.icons.cssggicons.Laptop
import compose.icons.lineawesomeicons.DesktopSolid
import compose.icons.lineawesomeicons.MobileAltSolid
import compose.icons.lineawesomeicons.QuestionCircleSolid
import compose.icons.lineawesomeicons.TvSolid
import xyz.hyli.connect.BuildConfig
import xyz.hyli.connect.R
import xyz.hyli.connect.bean.DeviceInfo
import xyz.hyli.connect.socket.SERVICE_TYPE
import xyz.hyli.connect.socket.SocketConfig
import xyz.hyli.connect.ui.ConfigHelper
import xyz.hyli.connect.ui.HyliConnectViewModel
import xyz.hyli.connect.ui.theme.HyliConnectColorScheme
import xyz.hyli.connect.ui.theme.HyliConnectTypography
import xyz.hyli.connect.utils.NetworkUtils
import java.util.concurrent.Semaphore
import kotlin.concurrent.thread

private val mNsdManagerState = mutableStateOf<NsdManager?>(null)
private lateinit var localBroadcastManager: LocalBroadcastManager
private lateinit var nsdDeviceMap: MutableMap<String,DeviceInfo>
private lateinit var connectDeviceVisibilityMap: MutableMap<String,MutableState<Boolean>>
private lateinit var connectedDeviceMap: MutableMap<String,DeviceInfo>

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun connectScreen(viewModel: HyliConnectViewModel, navController: NavHostController, currentSelect: MutableState<Int>) {
    val context = LocalContext.current
    nsdDeviceMap = viewModel.nsdDeviceMap
    connectDeviceVisibilityMap = viewModel.connectDeviceVisibilityMap
    connectedDeviceMap = viewModel.connectedDeviceMap

    val sharedPreferences = remember { context.getSharedPreferences("config", Context.MODE_PRIVATE) }
    val NICKNAME = remember { ConfigHelper().getNickname(sharedPreferences, sharedPreferences.edit()) }
    val IP_ADDRESS = remember { NetworkUtils.getLocalIPInfo(context) }
    val UUID = remember { ConfigHelper().getUUID(sharedPreferences, sharedPreferences.edit()) }

    val semaphore = remember { Semaphore(1) }
    mNsdManagerState.value = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    val mNsdManager = mNsdManagerState.value
    val mResolverListener = object : NsdManager.ResolveListener {
        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            // Called when the resolve fails. Use the error code to debug.
            Log.e("mResolverListener","Resolve failed $errorCode")
            semaphore.release()
        }
        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
            Log.i("mResolverListener","Resolve Succeeded. $serviceInfo")
            val host = serviceInfo.host.hostAddress ?: ""
            val port = serviceInfo.port
            val attributes = serviceInfo.attributes
            val api_version = String(attributes["api"] ?: "0".encodeToByteArray()).toInt()
            val app_version = String(attributes["app"] ?: "0".encodeToByteArray()).toInt()
            val app_version_name = String(attributes["app_name"] ?: byteArrayOf())
            val platform = String(attributes["platform"] ?: byteArrayOf())
            val uuid = String(attributes["uuid"] ?: byteArrayOf())
            val nickname = String(attributes["nickname"] ?: byteArrayOf())
            val ip_address = String(attributes["ip_addr"] ?: byteArrayOf())

            // Filter out self
            if ( uuid == UUID && BuildConfig.DEBUG.not() ) {
                return
            }
            // Filter out connected
            if ( uuid in connectedDeviceMap.keys ) {
                return
            }
            if ( uuid in nsdDeviceMap.keys ) {
                if ( host in nsdDeviceMap[uuid]!!.ip_address ) {
                    return
                }
                val newMap = nsdDeviceMap
                if ( host == ip_address ) {
                    newMap[uuid]!!.ip_address.add(0, host)
                } else {
                    newMap[uuid]!!.ip_address.add(host)
                }
                nsdDeviceMap = newMap
            } else {
                nsdDeviceMap[uuid] = DeviceInfo(
                    api_version = api_version,
                    app_version = app_version,
                    app_version_name = app_version_name,
                    platform = platform,
                    uuid = uuid,
                    nickname = nickname,
                    ip_address = mutableListOf(host),
                    port = port
                )
            }
            semaphore.release()
        }
    }
    val mDiscoveryListener = object : NsdManager.DiscoveryListener {
        override fun onDiscoveryStarted(regType: String) {
            Log.d("mDiscoveryListener", "Service discovery started")
        }
        override fun onServiceFound(service: NsdServiceInfo) {
            Log.d("mDiscoveryListener", "Service discovery success $service")
            thread {
                semaphore.acquire()
                mNsdManager!!.resolveService(service, mResolverListener)
            }
        }
        override fun onServiceLost(service: NsdServiceInfo) {
            Log.e("mDiscoveryListener", "service lost $service")
        }
        override fun onDiscoveryStopped(serviceType: String) {
            Log.i("mDiscoveryListener", "Discovery stopped: $serviceType")
        }
        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e("mDiscoveryListener", "Discovery failed: Error code:$errorCode")
            mNsdManager!!.stopServiceDiscovery(this)
        }
        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e("mDiscoveryListener", "Discovery failed: Error code:$errorCode")
            mNsdManager!!.stopServiceDiscovery(this)
        }
    }
//    mNsdManager!!.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener)
    DisposableEffect(Unit) {
        localBroadcastManager = LocalBroadcastManager.getInstance(context)
        localBroadcastManager.sendBroadcast(Intent("xyz.hyli.connect.service.SocketService.action.SERVICE_CONTROLLER").apply {
            putExtra("command", "reboot_nsd_service")
        })
        mNsdManager!!.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener)
        onDispose {
            nsdDeviceMap.clear()
            try {
                mNsdManager.stopServiceDiscovery(mDiscoveryListener)
            } catch (e: Exception) {
                Log.e("mDiscoveryListener", "stopServiceDiscovery failed: $e")
            }
        }
    }
    Column(modifier = Modifier
        .fillMaxSize()
        .padding(12.dp)) {
        Row {
            Text(text = stringResource(id = R.string.app_name),
                modifier = Modifier
//                .padding(16.dp)
                    .fillMaxWidth(),
                fontWeight = FontWeight.SemiBold,
                fontSize = 34.sp,
                lineHeight = 40.sp,
                letterSpacing = 0.sp)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row {
            Text(text = NICKNAME)
            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier
                .padding(horizontal = 2.dp)
                .align(Alignment.CenterVertically)
                .clickable {
                    currentSelect.value = 2
                    navController.navigate("settingsScreen") {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                })
        }
        Row {
            if ( IP_ADDRESS.isEmpty().not() ) {
                Text(text = "IP: ")
                LazyColumn {
                    if ( IP_ADDRESS.containsKey("wlan0") && IP_ADDRESS.containsKey("wlan1") ) {
                        item {
                            Text(text = "[ wlan0 ]:\t${IP_ADDRESS["wlan0"]}")
                        }
                        item {
                            Text(text = "[ wlan1 ]:\t${IP_ADDRESS["wlan1"]}")
                        }
                    } else if ( IP_ADDRESS.containsKey("wlan0") && IP_ADDRESS.containsKey("wlan1").not() ) {
                        item {
                            Text(text = "${IP_ADDRESS["wlan0"]}")
                        }
                    } else if ( IP_ADDRESS.containsKey("wlan0").not() && IP_ADDRESS.containsKey("wlan1").not() && IP_ADDRESS.size == 1 ) {
                        item {
                            Text(text = "[ ${IP_ADDRESS.keys.first()} ]:${IP_ADDRESS.values.first()}")
                        }
                    } else {
                        IP_ADDRESS.forEach {
                            item {
                                Text(text = "[ ${it.key} ]:\t${it.value}")
                            }
                        }
                    }
                }
            } else {
                Text(text = "IP: 0.0.0.0")
            }
        }
        Text(text = UUID, style = HyliConnectTypography.bodySmall, color = HyliConnectColorScheme().outline)
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(content = {
            item {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(text = stringResource(id = R.string.page_connect_available_devices))
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier
                        .padding(horizontal = 2.dp)
                        .align(Alignment.CenterVertically)
                        .clickable {
                            /*TODO(Manual Connect)*/
                        }
                    )
                }
            }
            if ( nsdDeviceMap.isEmpty() ) {
                item {
                    EmptyDeviceCard()
                }
            }
            items(nsdDeviceMap.values.toList()) { deviceInfo ->
                deviceCard(deviceInfo)
            }
            item {
                Row(modifier = Modifier
                    .animateItemPlacement(
                        animationSpec = tween(400)
                    )
                    .clickable {
                        currentSelect.value = 1
                        navController.navigate("DevicesScreen") {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                ) {
                    Text(text = stringResource(id = R.string.page_connect_connected_devices))
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, modifier = Modifier
                        .padding(horizontal = 2.dp)
                        .align(Alignment.CenterVertically))
                }
            }
            items(connectedDeviceMap.values.toList()) { deviceInfo ->
                deviceCard(deviceInfo, navController, currentSelect)
            }
        })
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun deviceCard(deviceInfo: DeviceInfo, navController: NavHostController? = null, currentSelect: MutableState<Int>? = null) {
    AnimatedVisibility(visible = connectDeviceVisibilityMap[deviceInfo.uuid]?.value ?: remember { mutableStateOf(false) }.value,
        enter = fadeIn(animationSpec = tween(400)),
        exit = fadeOut(animationSpec = tween(400))) {
        Card(modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (deviceInfo.uuid in connectedDeviceMap.keys && navController != null && currentSelect != null) {
                    currentSelect.value = 1
                    navController.navigate("DevicesScreen") {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                } else {
                    localBroadcastManager.sendBroadcast(Intent("xyz.hyli.connect.service.SocketService.action.SOCKET_CLIENT").apply {
                        putExtra("command", "start")
                        putExtra("ip", deviceInfo.ip_address[0])
                        putExtra("port", deviceInfo.port)
                    })
                }
            }
            .padding(6.dp),
            colors = CardColors(
                containerColor = HyliConnectColorScheme().secondaryContainer,
                contentColor = HyliConnectColorScheme().onSecondaryContainer,
                disabledContainerColor = HyliConnectColorScheme().surfaceVariant,
                disabledContentColor = HyliConnectColorScheme().onSurfaceVariant
            )
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(when(deviceInfo.platform) {
                    "Android Phone" -> { LineAwesomeIcons.MobileAltSolid }
                    "Android TV" -> { LineAwesomeIcons.TvSolid }
                    "Windows" -> { LineAwesomeIcons.DesktopSolid }
                    "Linux" -> { LineAwesomeIcons.DesktopSolid }
                    "Mac" -> { CssGgIcons.Laptop }
                    "Web" -> { CssGgIcons.GlobeAlt }
                    else -> { LineAwesomeIcons.QuestionCircleSolid }
                }, contentDescription = null, modifier = Modifier
                    .size(84.dp)
                    .padding(top = 12.dp, bottom = 12.dp))
                Column(modifier = Modifier.padding(top = 12.dp, bottom = 12.dp, end = 12.dp), verticalArrangement = Arrangement.Center) {
                    Row(verticalAlignment = Alignment.CenterVertically){
                        Text(text = when(deviceInfo.uuid) {
                            ConfigHelper.uuid -> { deviceInfo.nickname + " (" + stringResource(id = R.string.page_connect_this_device) + ")" }
                            else -> { deviceInfo.nickname }
                        }, style = HyliConnectTypography.titleMedium)
                    }
                    FlowRow {
                        Card(modifier = Modifier.padding(end = 6.dp, top = 4.dp), colors = CardColors(
                            containerColor = HyliConnectColorScheme().tertiaryContainer,
                            contentColor = HyliConnectColorScheme().onTertiaryContainer,
                            disabledContainerColor = HyliConnectColorScheme().surfaceVariant,
                            disabledContentColor = HyliConnectColorScheme().onSurfaceVariant
                        )) {
                            Text(text = deviceInfo.platform, modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp), style = HyliConnectTypography.labelMedium)
                        }
                        deviceInfo.ip_address.forEach {
                            Card(modifier = Modifier.padding(end = 6.dp, top = 4.dp), colors = CardColors(
                                containerColor = HyliConnectColorScheme().tertiaryContainer,
                                contentColor = HyliConnectColorScheme().onTertiaryContainer,
                                disabledContainerColor = HyliConnectColorScheme().surfaceVariant,
                                disabledContentColor = HyliConnectColorScheme().onSurfaceVariant
                            )) {
                                Text(text = it, modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp), style = HyliConnectTypography.labelMedium)
                            }
                        }
                    }
                    Text(text = deviceInfo.uuid, style = HyliConnectTypography.bodySmall, color = HyliConnectColorScheme().outline, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
    connectDeviceVisibilityMap[deviceInfo.uuid] = remember { mutableStateOf(true) }
}

@Composable
fun EmptyDeviceCard() {
    val isIconVisible = remember { mutableStateOf(true) }
    val icon = remember { mutableStateOf(LineAwesomeIcons.MobileAltSolid) }
    val iconList = listOf(
        LineAwesomeIcons.MobileAltSolid,
        LineAwesomeIcons.TvSolid,
        LineAwesomeIcons.DesktopSolid,
        CssGgIcons.Laptop,
        CssGgIcons.GlobeAlt
    )
    AnimatedVisibility(visible = true,
        enter = fadeIn(animationSpec = tween(400)),
        exit = fadeOut(animationSpec = tween(400))) {
        Card(modifier = Modifier
            .fillMaxWidth()
            .padding(6.dp),
            colors = CardColors(
                containerColor = HyliConnectColorScheme().secondaryContainer,
                contentColor = HyliConnectColorScheme().onSecondaryContainer,
                disabledContainerColor = HyliConnectColorScheme().surfaceVariant,
                disabledContentColor = HyliConnectColorScheme().onSurfaceVariant
            )
        ) {
            BoxWithConstraints {
                val width = this.maxWidth
                Box {
                    Row {
                        AnimatedVisibility(visible = isIconVisible.value,
                            enter = fadeIn(animationSpec = tween(400)),
                            exit = fadeOut(animationSpec = tween(400))
                        ) {
                            Row(modifier = Modifier.fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
                                Icon(icon.value, contentDescription = null, modifier = Modifier
                                    .size(84.dp)
                                    .padding(top = 12.dp, bottom = 12.dp))
                            }
                        }
                    }
                    Column(modifier = Modifier
                        .padding(top = 12.dp, bottom = 12.dp, end = 12.dp)
                        .align(Alignment.TopStart)
                        .offset(x = 84.dp)) {
                        Card(modifier = Modifier.padding(end = 6.dp, top = 4.dp), colors = CardColors(
                            containerColor = HyliConnectColorScheme().tertiaryContainer,
                            contentColor = HyliConnectColorScheme().onTertiaryContainer,
                            disabledContainerColor = HyliConnectColorScheme().surfaceVariant,
                            disabledContentColor = HyliConnectColorScheme().onSurfaceVariant
                        )) {
                            Text(text = "                           ", style = HyliConnectTypography.titleMedium)
                        }
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                            Card(modifier = Modifier.padding(end = 6.dp, top = 4.dp), colors = CardColors(
                                containerColor = HyliConnectColorScheme().tertiaryContainer,
                                contentColor = HyliConnectColorScheme().onTertiaryContainer,
                                disabledContainerColor = HyliConnectColorScheme().surfaceVariant,
                                disabledContentColor = HyliConnectColorScheme().onSurfaceVariant
                            )) {
                                Text(text = "              ", modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp), style = HyliConnectTypography.labelMedium)
                            }
                            Card(modifier = Modifier.padding(end = 6.dp, top = 4.dp), colors = CardColors(
                                containerColor = HyliConnectColorScheme().tertiaryContainer,
                                contentColor = HyliConnectColorScheme().onTertiaryContainer,
                                disabledContainerColor = HyliConnectColorScheme().surfaceVariant,
                                disabledContentColor = HyliConnectColorScheme().onSurfaceVariant
                            )) {
                                Text(text = "                      ", modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp), style = HyliConnectTypography.labelMedium)
                            }
                        }
                        Text(text = " ", style = HyliConnectTypography.bodySmall, color = HyliConnectColorScheme().outline, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
    }
    thread {
        Thread.sleep(2000)
        while (nsdDeviceMap.isEmpty()) {
            isIconVisible.value = false
            Thread.sleep(400)
            icon.value = iconList[(iconList.indexOf(icon.value) + 1) % iconList.size]
            Thread.sleep(400)
            isIconVisible.value = true
            Thread.sleep(4000)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun deviceCardPreview() {
    deviceCard(DeviceInfo(
        api_version = 1,
        app_version = 10000,
        app_version_name = "1.0.0",
        platform = "Android Phone",
        uuid = "38299469-a3bc-48e9-b2c8-be8410650d87",
        nickname = "Windows Subsystem for Android(TM)",
        ip_address = mutableListOf("172.30.166.176", "127.0.0.1"),
        port = 15372
    ))
}