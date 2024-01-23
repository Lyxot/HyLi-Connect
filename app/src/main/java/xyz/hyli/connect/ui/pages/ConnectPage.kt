package xyz.hyli.connect.ui.pages

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.getString
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import compose.icons.CssGgIcons
import compose.icons.LineAwesomeIcons
import compose.icons.cssggicons.AppleWatch
import compose.icons.cssggicons.GlobeAlt
import compose.icons.cssggicons.Laptop
import compose.icons.lineawesomeicons.DesktopSolid
import compose.icons.lineawesomeicons.MobileAltSolid
import compose.icons.lineawesomeicons.QuestionCircleSolid
import compose.icons.lineawesomeicons.TvSolid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku
import xyz.hyli.connect.BuildConfig
import xyz.hyli.connect.HyliConnect
import xyz.hyli.connect.R
import xyz.hyli.connect.bean.DeviceInfo
import xyz.hyli.connect.bean.ServiceState
import xyz.hyli.connect.datastore.PreferencesDataStore
import xyz.hyli.connect.ui.theme.HyliConnectColorScheme
import xyz.hyli.connect.ui.theme.HyliConnectTypography
import xyz.hyli.connect.ui.viewmodel.HyliConnectViewModel
import xyz.hyli.connect.utils.NetworkUtils
import xyz.hyli.connect.utils.ServiceUtils
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Semaphore
import kotlin.concurrent.thread

private var shizukuPermissionFuture = CompletableFuture<Boolean>()
private val mNsdManagerState = mutableStateOf<NsdManager?>(null)
private lateinit var applicationState: MutableState<String>
private lateinit var permissionState: MutableState<Boolean>
private lateinit var localBroadcastManager: LocalBroadcastManager
private lateinit var nsdDeviceMap: MutableMap<String,DeviceInfo>
private lateinit var connectDeviceVisibilityMap: MutableMap<String,MutableState<Boolean>>
private lateinit var connectedDeviceMap: MutableMap<String,DeviceInfo>

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConnectScreen(viewModel: HyliConnectViewModel, navController: NavHostController, paddingValues: PaddingValues = PaddingValues(0.dp)) {
    val context = LocalContext.current
    val currentSelect = viewModel.currentSelect
    localBroadcastManager = viewModel.localBroadcastManager.value ?: LocalBroadcastManager.getInstance(context)
    applicationState = viewModel.applicationState
    permissionState = viewModel.permissionState
    nsdDeviceMap = viewModel.nsdDeviceMap
    connectDeviceVisibilityMap = viewModel.connectDeviceVisibilityMap
    connectedDeviceMap = viewModel.connectedDeviceMap

    val configMap = remember { PreferencesDataStore.getConfigMap(true)}
    val NICKNAME = PreferencesDataStore.nickname.asFlow().collectAsState(initial = configMap["nickname"].toString())
    val UUID = PreferencesDataStore.uuid.asFlow().collectAsState(initial = configMap["uuid"].toString())
    val IP_ADDRESS = remember { NetworkUtils.getLocalIPInfo(context) }

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
            if ( uuid == UUID.value && BuildConfig.DEBUG.not() ) {
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
    DisposableEffect(Unit) {
        MainScope().launch {
            try {
                HyliConnect.permissionStateMap["Shizuku"] = checkShizukuPermission(context)
            } catch (_: Exception) { }
            try {
                HyliConnect.serviceStateMap["SocketService"] = if (ServiceUtils.isServiceWork(context, "xyz.hyli.connect.service.SocketService")) {
                    ServiceState("running", context.getString(R.string.state_service_running, context.getString(R.string.service_socket_service)))
                } else {
                    ServiceState("stopped", context.getString(R.string.state_service_stopped, context.getString(R.string.service_socket_service)))
                }
            } catch (_: Exception) { }
            applicationState.value = viewModel.updateApplicationState()
            permissionState.value = viewModel.updatePermissionState(context)
        }
        localBroadcastManager.sendBroadcast(Intent("xyz.hyli.connect.service.SocketService.action.SERVICE_CONTROLLER").apply {
            putExtra("command", "reboot_nsd_service")
        })
        mNsdManager!!.discoverServices("_hyli-connect._tcp.", NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener)
        onDispose {
            nsdDeviceMap.clear()
            try {
                mNsdManager.stopServiceDiscovery(mDiscoveryListener)
            } catch (e: Exception) {
                Log.e("mDiscoveryListener", "stopServiceDiscovery failed: $e")
            }
        }
    }
    Column(modifier = Modifier.padding(paddingValues)) {
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
            LazyColumn(content = {
                item {
                    Card(modifier = Modifier
                        .padding(6.dp)
                        .animateItemPlacement(animationSpec = tween(400)),
                        colors = when(applicationState.value) {
                            "running" -> { CardColors(
                                containerColor = HyliConnectColorScheme().secondaryContainer,
                                contentColor = HyliConnectColorScheme().onSecondaryContainer,
                                disabledContainerColor = HyliConnectColorScheme().surfaceVariant,
                                disabledContentColor = HyliConnectColorScheme().onSurfaceVariant
                            ) }
                            "error" -> { CardColors(
                                containerColor = Color(0xFFdcb334),
                                contentColor = HyliConnectColorScheme().onError,
                                disabledContainerColor = HyliConnectColorScheme().surfaceVariant,
                                disabledContentColor = HyliConnectColorScheme().onSurfaceVariant
                            ) }
                            "stopped" -> { CardColors(
                                containerColor = HyliConnectColorScheme(dynamicColor = false).error,
                                contentColor = HyliConnectColorScheme(dynamicColor = false).onError,
                                disabledContainerColor = HyliConnectColorScheme().surfaceVariant,
                                disabledContentColor = HyliConnectColorScheme().onSurfaceVariant
                            ) }
                            else -> { CardColors(
                                containerColor = HyliConnectColorScheme().secondaryContainer,
                                contentColor = HyliConnectColorScheme().onSecondaryContainer,
                                disabledContainerColor = HyliConnectColorScheme().surfaceVariant,
                                disabledContentColor = HyliConnectColorScheme().onSurfaceVariant
                            ) }
                        }
                    ) {
                        Row(modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = when(applicationState.value) {
                                "running" -> { Icons.Default.Check }
                                "error" -> { LineAwesomeIcons.QuestionCircleSolid }
                                "stopped" -> { Icons.Default.Close }
                                else -> { LineAwesomeIcons.QuestionCircleSolid }
                            }, contentDescription = null, modifier = Modifier
                                .size(42.dp)
                                .padding(6.dp))
                            Column(modifier = Modifier.padding(start = 12.dp),
                                verticalArrangement = Arrangement.Center) {
                                Text(text = when(applicationState.value) {
                                    "running" -> { stringResource(id = R.string.state_application_running) }
                                    "error" -> { stringResource(id = R.string.state_application_error) }
                                    "stopped" -> { stringResource(id = R.string.state_application_stopped) }
                                    else -> { "" }
                                }, style = HyliConnectTypography.titleLarge)
                                if ( applicationState.value != "stopped" ) {
                                    if ( applicationState.value == "error" ) {
                                        HyliConnect.serviceStateMap.forEach {
                                            if ( it.value.state == "error" ) {
                                                it.value.message?.let { it1 ->
                                                    Text(
                                                        text = it1,
                                                        style = HyliConnectTypography.bodySmall,
                                                        color = HyliConnectColorScheme().onError
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row {
                                        Text(text = "${stringResource(id = R.string.page_connect_nickname)}: ${NICKNAME.value}")
                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier
                                            .size(24.dp)
                                            .padding(horizontal = 2.dp)
                                            .align(Alignment.CenterVertically)
                                            .clickable {
                                                currentSelect.value = 2
                                                navController.navigate("settingsScreen") {
                                                    popUpTo(navController.graph.findStartDestination().id) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            })
                                    }
                                    Row {
                                        if ( IP_ADDRESS.isEmpty().not() ) {
                                            Text(text = "IP: ")
                                            Column {
                                                if ( IP_ADDRESS.containsKey("wlan0") && IP_ADDRESS.containsKey("wlan1") ) {
                                                    Text(text = "[ wlan0 ]:\t${IP_ADDRESS["wlan0"]}")
                                                    Text(text = "[ wlan1 ]:\t${IP_ADDRESS["wlan1"]}")
                                                } else if ( IP_ADDRESS.containsKey("wlan0") && IP_ADDRESS.containsKey("wlan1").not() ) {
                                                    Text(text = "${IP_ADDRESS["wlan0"]}")
                                                } else if ( IP_ADDRESS.containsKey("wlan0").not() && IP_ADDRESS.containsKey("wlan1").not() && IP_ADDRESS.size == 1 ) {
                                                    Text(text = "[ ${IP_ADDRESS.keys.first()} ]:${IP_ADDRESS.values.first()}")
                                                } else {
                                                    IP_ADDRESS.forEach {
                                                        Text(text = "[ ${it.key} ]:\t${it.value}")
                                                    }
                                                }
                                            }
                                        } else {
                                            Text(text = "IP: 0.0.0.0")
                                        }
                                    }
                                    Text(text = UUID.value ?:"", style = HyliConnectTypography.bodySmall, color = HyliConnectColorScheme().outline)
                                } else {
                                    HyliConnect.serviceStateMap.forEach {
                                        if ( it.value.state != "running" ) {
                                            it.value.message?.let { it1 ->
                                                Text(
                                                    text = it1,
                                                    style = HyliConnectTypography.bodySmall,
                                                    color = HyliConnectColorScheme().onError
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    if (permissionState.value.not()) {
                        Card(modifier = Modifier
                            .padding(6.dp)
                            .animateItemPlacement(animationSpec = tween(400)),
                            colors = CardColors(
                                containerColor = Color(0xFFdcb334),
                                contentColor = HyliConnectColorScheme().onError,
                                disabledContainerColor = HyliConnectColorScheme().surfaceVariant,
                                disabledContentColor = HyliConnectColorScheme().onSurfaceVariant
                            )) {
                            Column(modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)) {
                                HyliConnect.permissionStateMap.forEach {
                                    if (it.value.not() && it.key in viewModel.keyPermissionList && it.key in viewModel.permissionMap.keys) {
                                        Row(modifier = Modifier.padding(horizontal = 12.dp)) {
                                            Icon(Icons.Default.Close, contentDescription = null)
                                            Text(stringResource(id = R.string.state_permission_false, stringResource(id = viewModel.permissionMap[it.key]!!)))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    Row(modifier = Modifier
                        .fillMaxWidth()
                        .animateItemPlacement(animationSpec = tween(400))) {
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
                    DeviceCard(deviceInfo)
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
                    DeviceCard(deviceInfo, navController, viewModel)
                }
            })
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DeviceCard(deviceInfo: DeviceInfo, navController: NavHostController? = null, viewModel: HyliConnectViewModel? = null) {
    val currentSelect = viewModel?.currentSelect
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
                    "Android Wear" -> { CssGgIcons.AppleWatch }
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
                            PreferencesDataStore.getConfigMap()["uuid"].toString() -> { deviceInfo.nickname + " (" + stringResource(id = R.string.page_connect_this_device) + ")" }
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
        CssGgIcons.AppleWatch,
        LineAwesomeIcons.DesktopSolid,
        CssGgIcons.Laptop,
        CssGgIcons.GlobeAlt
    )
    LaunchedEffect(Unit) {
        MainScope().launch(context = Dispatchers.Main) {
            delay(1000)
            while (nsdDeviceMap.isEmpty()) {
                try {
                    isIconVisible.value = false
                    delay(400)
                    icon.value = iconList[(iconList.indexOf(icon.value) + 1) % iconList.size]
                    delay(400)
                    isIconVisible.value = true
                    delay(4000)
                } catch (_: Exception) { }
            }
        }
    }
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
}
private fun checkShizukuPermission(context: Context): Boolean {
    var toast: Toast? = null
    val b = if (!Shizuku.pingBinder()) {
        toast = Toast.makeText(context, getString(context, R.string.toast_shizuku_not_available), Toast.LENGTH_LONG)
        false
    } else if (Shizuku.isPreV11()) {
        toast = Toast.makeText(context, getString(context, R.string.toast_shizuku_not_support), Toast.LENGTH_LONG)
        false
    } else if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
        true
    } else if (Shizuku.shouldShowRequestPermissionRationale()) {
        toast = Toast.makeText(
            context,
            getString(context, R.string.toast_shizuku_denied),
            Toast.LENGTH_LONG
        )
        false
    } else {
        Shizuku.requestPermission(HyliConnect.SHIZUKU_CODE)

        val result = shizukuPermissionFuture.get()
        shizukuPermissionFuture = CompletableFuture<Boolean>()

        result
    }
    if ( PreferencesDataStore.getConfigMap(true)["is_stream"] == true && "Shizuku" in PreferencesDataStore.getConfigMap()["app_stream_method"] as String ) {
        toast?.show()
    }
    return b
}

@Preview(showBackground = true)
@Composable
fun DeviceCardPreview() {
    DeviceCard(DeviceInfo(
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