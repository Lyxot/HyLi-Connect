package xyz.hyli.connect.ui.pages

import android.content.Context
import android.content.Intent
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateListOf
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
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.hjq.permissions.XXPermissions
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import xyz.hyli.connect.HyLiConnect
import xyz.hyli.connect.R
import xyz.hyli.connect.bean.DeviceInfo
import xyz.hyli.connect.bean.ServiceState
import xyz.hyli.connect.datastore.PreferencesDataStore
import xyz.hyli.connect.ui.theme.HyLiConnectColorScheme
import xyz.hyli.connect.ui.theme.HyLiConnectTypography
import xyz.hyli.connect.ui.viewmodel.HyLiConnectViewModel
import xyz.hyli.connect.utils.NetworkUtils
import xyz.hyli.connect.utils.PermissionUtils
import xyz.hyli.connect.utils.ServiceUtils
import java.util.concurrent.Semaphore

@OptIn(DelicateCoroutinesApi::class)
@Composable
private fun InitNsd(viewModel: HyLiConnectViewModel, context: Context, UUID: State<String?>, connectToMyself: State<Boolean?>) {
    val semaphore = remember { Semaphore(1) }
    val mNsdManager = remember { context.getSystemService(Context.NSD_SERVICE) as NsdManager }
    val mResolverListener = object : NsdManager.ResolveListener {
        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            // Called when the resolve fails. Use the error code to debug.
            Log.e("mResolverListener", "Resolve failed $errorCode")
            semaphore.release()
        }
        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
            Log.i("mResolverListener", "Resolve Succeeded. $serviceInfo")
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
            if (uuid == UUID.value && !connectToMyself.value!!) {
                return
            }
            // Filter out connected
            if (uuid in viewModel.connectedDeviceMap.keys) {
                return
            }
            if (uuid in viewModel.nsdDeviceMap.keys) {
                if (host in viewModel.nsdDeviceMap[uuid]!!.ipAddress) {
                    return
                }
                val newMap = viewModel.nsdDeviceMap
                if (host == ip_address) {
                    newMap[uuid]!!.ipAddress.add(0, host)
                } else {
                    newMap[uuid]!!.ipAddress.add(host)
                }
                viewModel.nsdDeviceMap = newMap
            } else {
                viewModel.nsdDeviceMap[uuid] = DeviceInfo(
                    apiVersion = api_version,
                    appVersion = app_version,
                    appVersionName = app_version_name,
                    platform = platform,
                    uuid = uuid,
                    nickname = nickname,
                    ipAddress = mutableListOf(host),
                    serverPort = port
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
            // 在某些设备上必须使用semaphore+协程才能正常工作 未知原因
            // 例如ChromeOS上的Android11子系统
            // 在其它设备上甚至无需协程即可正常工作
            GlobalScope.launch(Dispatchers.IO) {
                semaphore.acquire()
                mNsdManager.resolveService(service, mResolverListener)
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
            mNsdManager.stopServiceDiscovery(this)
        }
        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e("mDiscoveryListener", "Discovery failed: Error code:$errorCode")
            mNsdManager.stopServiceDiscovery(this)
        }
    }
    DisposableEffect(Unit) {
        try { mNsdManager.stopServiceDiscovery(mDiscoveryListener) } catch (_: Exception) { }
        try { mNsdManager.discoverServices("_hyli-connect._tcp.", NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener) } catch (_: Exception) { }
        onDispose {
            viewModel.nsdDeviceMap.clear()
            try {
                mNsdManager.stopServiceDiscovery(mDiscoveryListener)
            } catch (e: Exception) {
                Log.e("mDiscoveryListener", "stopServiceDiscovery failed: $e")
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConnectScreen(
    viewModel: HyLiConnectViewModel,
    navController: NavHostController,
    paddingValues: PaddingValues = PaddingValues(0.dp)
) {
    val context = LocalContext.current
    val currentSelect = viewModel.currentSelect
    currentSelect.intValue = 0

    val NICKNAME = PreferencesDataStore.nickname.asFlow().collectAsState(initial = "")
    val UUID = PreferencesDataStore.uuid.asFlow().collectAsState(initial = "")
    val IP_ADDRESS = remember { NetworkUtils.getLocalIPInfo(context) }

    InitNsd(viewModel, context, UUID, PreferencesDataStore.connect_to_myself.asFlow().collectAsState(initial = false))

    LaunchedEffect(viewModel.applicationState.value) {
        try {
            HyLiConnect.serviceStateMap["SocketService"] = if (ServiceUtils.isServiceWork(context, "xyz.hyli.connect.service.SocketService")) {
                ServiceState("running", context.getString(R.string.state_service_running, context.getString(R.string.service_socket_service)))
            } else {
                ServiceState("stopped", context.getString(R.string.state_service_stopped, context.getString(R.string.service_socket_service)))
            }
        } catch (_: Exception) { }
        viewModel.updateApplicationState()
        // TODO: observe network state
        viewModel.localBroadcastManager.value?.sendBroadcast(
            Intent(
                "xyz.hyli.connect.service.SocketService.action.SERVICE_CONTROLLER"
            ).apply {
                putExtra("command", "reboot_nsd_service")
            }
        )
    }

    DisposableEffect(viewModel.applicationState.value) {
        onDispose {  }
    }
    Column(modifier = Modifier.padding(paddingValues)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Row {
                Text(
                    text = stringResource(id = R.string.app_name),
                    modifier = Modifier
//                .padding(16.dp)
                        .fillMaxWidth(),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 34.sp,
                    lineHeight = 40.sp,
                    letterSpacing = 0.sp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            LazyColumn(content = {
                item {
                    Card(
                        modifier = Modifier
                            .padding(6.dp)
                            .animateItemPlacement(animationSpec = tween(400)),
                        colors = when (viewModel.applicationState.value) {
                            "running" -> {
                                CardColors(
                                    containerColor = HyLiConnectColorScheme().secondaryContainer,
                                    contentColor = HyLiConnectColorScheme().onSecondaryContainer,
                                    disabledContainerColor = HyLiConnectColorScheme().surfaceVariant,
                                    disabledContentColor = HyLiConnectColorScheme().onSurfaceVariant
                                )
                            }
                            "rebooting" -> {
                                CardColors(
                                    containerColor = Color(0xFFdcb334),
                                    contentColor = HyLiConnectColorScheme().onError,
                                    disabledContainerColor = HyLiConnectColorScheme().surfaceVariant,
                                    disabledContentColor = HyLiConnectColorScheme().onSurfaceVariant
                                )
                            }
                            "error" -> {
                                CardColors(
                                    containerColor = Color(0xFFdcb334),
                                    contentColor = HyLiConnectColorScheme().onError,
                                    disabledContainerColor = HyLiConnectColorScheme().surfaceVariant,
                                    disabledContentColor = HyLiConnectColorScheme().onSurfaceVariant
                                )
                            }
                            "stopped" -> {
                                CardColors(
                                    containerColor = HyLiConnectColorScheme(dynamicColor = false).error,
                                    contentColor = HyLiConnectColorScheme(dynamicColor = false).onError,
                                    disabledContainerColor = HyLiConnectColorScheme().surfaceVariant,
                                    disabledContentColor = HyLiConnectColorScheme().onSurfaceVariant
                                )
                            }
                            else -> {
                                CardColors(
                                    containerColor = HyLiConnectColorScheme().secondaryContainer,
                                    contentColor = HyLiConnectColorScheme().onSecondaryContainer,
                                    disabledContainerColor = HyLiConnectColorScheme().surfaceVariant,
                                    disabledContentColor = HyLiConnectColorScheme().onSurfaceVariant
                                )
                            }
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when (viewModel.applicationState.value) {
                                    "running" -> { Icons.Default.Check }
                                    "rebooting" -> { Icons.Default.Refresh }
                                    "error" -> { Icons.AutoMirrored.Outlined.HelpOutline }
                                    "stopped" -> { Icons.Default.Close }
                                    else -> { Icons.AutoMirrored.Outlined.HelpOutline }
                                },
                                contentDescription = null,
                                modifier = Modifier
                                    .size(42.dp)
                                    .padding(6.dp)
                            )
                            Column(
                                modifier = Modifier.padding(start = 12.dp),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Row {
                                    Text(
                                        text = when (viewModel.applicationState.value) {
                                            "running" -> { stringResource(id = R.string.state_application_running) }
                                            "rebooting" -> { stringResource(id = R.string.state_application_rebooting) }
                                            "error" -> { stringResource(id = R.string.state_application_error) }
                                            "stopped" -> { stringResource(id = R.string.state_application_stopped) }
                                            else -> { "" }
                                        },
                                        style = HyLiConnectTypography.titleLarge
                                    )
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .padding(start = 12.dp)
                                            .align(Alignment.CenterVertically)
                                            .clickable {
                                                val map = viewModel.connectedDeviceMap.toMap()
                                                HyLiConnectViewModel().applicationState.value =
                                                    "rebooting"
                                                viewModel.applicationState.value = "rebooting"
                                                viewModel.localBroadcastManager.value?.sendBroadcast(
                                                    Intent(
                                                        "xyz.hyli.connect.service.SocketService.action.SERVICE_CONTROLLER"
                                                    ).apply {
                                                        putExtra("command", "reboot_service")
                                                    }
                                                )
                                                Toast
                                                    .makeText(
                                                        context,
                                                        getString(
                                                            context,
                                                            R.string.state_application_rebooting
                                                        ),
                                                        Toast.LENGTH_SHORT
                                                    )
                                                    .show()
                                                MainScope().launch {
                                                    val newMap = mutableMapOf<String, DeviceInfo>().apply {
                                                        map.forEach {
                                                            this[it.key] = DeviceInfo(
                                                                apiVersion = it.value.apiVersion,
                                                                appVersion = it.value.appVersion,
                                                                appVersionName = it.value.appVersionName,
                                                                platform = it.value.platform,
                                                                uuid = it.value.uuid,
                                                                nickname = it.value.nickname,
                                                                ipAddress = it.value.ipAddress,
                                                                serverPort = it.value.serverPort
                                                            )
                                                        }
                                                    }
                                                    val t = System.currentTimeMillis()
                                                    while (viewModel.applicationState.value != "running") {
                                                        delay(500)
                                                        if (System.currentTimeMillis() - t > 20000) {
                                                            return@launch
                                                        }
                                                    }
                                                    newMap.forEach {
                                                        viewModel.nsdDeviceMap[it.key] = it.value
                                                    }
                                                }
                                            }
                                    )
                                }
                                if (viewModel.applicationState.value != "stopped") {
                                    if (viewModel.applicationState.value == "error") {
                                        HyLiConnect.serviceStateMap.forEach {
                                            if (it.value.state == "error") {
                                                it.value.message?.let { it1 ->
                                                    Text(
                                                        text = it1,
                                                        style = HyLiConnectTypography.bodySmall,
                                                        color = HyLiConnectColorScheme().onError
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row {
                                        Text(
                                            text = "${stringResource(
                                                id = R.string.page_connect_nickname
                                            )}: ${NICKNAME.value}"
                                        )
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(24.dp)
                                                .padding(horizontal = 2.dp)
                                                .align(Alignment.CenterVertically)
                                                .clickable {
                                                    currentSelect.intValue = 2
                                                    navController.navigate("settingsScreen") {
                                                        popUpTo(navController.graph.findStartDestination().id) {
                                                            saveState = true
                                                        }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                }
                                        )
                                    }
                                    Row {
                                        if (IP_ADDRESS.isEmpty().not()) {
                                            Text(text = "IP: ")
                                            Column {
                                                if (IP_ADDRESS.containsKey("wlan0") && IP_ADDRESS.containsKey("wlan1")) {
                                                    Text(text = "[ wlan0 ]:\t${IP_ADDRESS["wlan0"]}")
                                                    Text(text = "[ wlan1 ]:\t${IP_ADDRESS["wlan1"]}")
                                                } else if (IP_ADDRESS.containsKey("wlan0") && IP_ADDRESS.containsKey("wlan1").not()) {
                                                    Text(text = "${IP_ADDRESS["wlan0"]}")
                                                } else if (IP_ADDRESS.containsKey("wlan0").not() && IP_ADDRESS.containsKey("wlan1").not() && IP_ADDRESS.size == 1) {
                                                    Text(
                                                        text = "[ ${IP_ADDRESS.keys.first()} ]:${IP_ADDRESS.values.first()}"
                                                    )
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
                                    Text(
                                        text = UUID.value ?: "",
                                        style = HyLiConnectTypography.bodySmall,
                                        color = HyLiConnectColorScheme().outline
                                    )
                                } else {
                                    HyLiConnect.serviceStateMap.forEach {
                                        if (it.value.state != "running") {
                                            it.value.message?.let { it1 ->
                                                Text(
                                                    text = it1,
                                                    style = HyLiConnectTypography.bodySmall,
                                                    color = HyLiConnectColorScheme().onError
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
                    val permissions = remember { mutableStateOf(viewModel.getRequiredPermissions()) }
                    val thirdPartyPermissions = remember { mutableStateOf(viewModel.getThirdPartyPermissions()) }
                    val deniedPermissions = remember { mutableStateListOf(listOf<String>()) }
                    LaunchedEffect(viewModel.applicationState.value) {
                        permissions.value = viewModel.getRequiredPermissions()
                        deniedPermissions.clear()
                        permissions.value.forEach {
                            if (!XXPermissions.isGranted(context, it)) deniedPermissions.add(it)
                            else deniedPermissions.remove(it)
                        }
                        try {
                            if (thirdPartyPermissions.value.contains(viewModel.permissionShizuku)) {
                                if (!PermissionUtils.checkShizukuPermission(context)) deniedPermissions.add(viewModel.permissionShizuku)
                                else deniedPermissions.remove(viewModel.permissionShizuku)
                            }
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                    if (deniedPermissions.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .padding(6.dp)
                                .animateItemPlacement(animationSpec = tween(400)),
                            colors = CardColors(
                                containerColor = Color(0xFFdcb334),
                                contentColor = HyLiConnectColorScheme().onError,
                                disabledContainerColor = HyLiConnectColorScheme().surfaceVariant,
                                disabledContentColor = HyLiConnectColorScheme().onSurfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp)
                            ) {
                                deniedPermissions.forEach {
                                    if (it.isEmpty()) return@forEach
                                    Row(modifier = Modifier
                                        .padding(horizontal = 12.dp)
                                        .clickable {
                                            if (it !in viewModel.thirdPartyPermissionSet) {
                                                XXPermissions.with(context)
                                                    .permission(it)
                                                    .request { _, allGranted ->
                                                        if (allGranted) deniedPermissions.remove(it)
                                                    }
                                            } else if (it == viewModel.permissionShizuku) {
                                                try {
                                                    HyLiConnect().initShizuku()
                                                } catch (e: Exception) { e.printStackTrace() }
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = null)
                                        Text(stringResource(id = R.string.state_permission_false, stringResource(id = viewModel.permissionMap[it]!!)))
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    Row(
                        modifier = Modifier
                            .animateItemPlacement(
                                animationSpec = tween(400)
                            )
                            .clickable {
                                currentSelect.intValue = 1
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
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(horizontal = 2.dp)
                                .align(Alignment.CenterVertically)
                        )
                    }
                }
                items(viewModel.connectedDeviceMap.values.toList()) { deviceInfo ->
                    DeviceCard(deviceInfo, true, navController, viewModel)
                }
                if (viewModel.connectedDeviceMap.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(6.dp)
                                .animateItemPlacement(animationSpec = tween(400)),
                            colors = CardColors(
                                containerColor = HyLiConnectColorScheme().secondaryContainer,
                                contentColor = HyLiConnectColorScheme().onSecondaryContainer,
                                disabledContainerColor = HyLiConnectColorScheme().surfaceVariant,
                                disabledContentColor = HyLiConnectColorScheme().onSurfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Outlined.HelpOutline,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(84.dp)
                                        .padding(top = 12.dp, bottom = 12.dp)
                                )
                                Text(
                                    text = stringResource(id = R.string.page_connect_no_device_connected),
                                    style = HyLiConnectTypography.titleMedium
                                )
                            }
                        }
                    }
                }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItemPlacement(animationSpec = tween(300))
                    ) {
                        Text(text = stringResource(id = R.string.page_connect_available_devices))
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(horizontal = 2.dp)
                                .align(Alignment.CenterVertically)
                                .clickable {
                                    /*TODO(Manual Connect)*/
                                }
                        )
                    }
                }
                if (viewModel.nsdDeviceMap.isEmpty()) {
                    item {
                        EmptyDeviceCard(viewModel)
                    }
                }
                items(viewModel.nsdDeviceMap.values.toList()) { deviceInfo ->
                    DeviceCard(deviceInfo, false, navController, viewModel)
                }
            })
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DeviceCard(
    deviceInfo: DeviceInfo,
    connected: Boolean = false,
    navController: NavHostController? = null,
    viewModel: HyLiConnectViewModel? = null
) {
    val currentSelect = viewModel?.currentSelect
    val visibility = remember { mutableStateOf(false) }
    AnimatedVisibility(
        visible = visibility.value,
        enter = if (connected) {
            fadeIn(animationSpec = tween(400)) + expandVertically(animationSpec = tween(300, easing = LinearEasing), expandFrom = Alignment.Top)
        } else {
            fadeIn(animationSpec = tween(400))
        },
        exit = if (connected) {
            fadeOut(animationSpec = tween(400))
        } else {
            shrinkVertically(animationSpec = tween(300, easing = LinearEasing), shrinkTowards = Alignment.Top) + fadeOut(animationSpec = tween(400))
        }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (connected && navController != null && currentSelect != null) {
                        currentSelect.intValue = 1
                        navController.navigate("DevicesScreen") {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    } else {
                        viewModel?.localBroadcastManager?.value?.sendBroadcast(
                            Intent(
                                "xyz.hyli.connect.service.SocketService.action.SOCKET_CLIENT"
                            ).apply {
                                putExtra("command", "start")
                                putExtra("ip", deviceInfo.ipAddress[0])
                                putExtra("port", deviceInfo.serverPort)
                            }
                        )
                    }
                }
                .padding(6.dp),
            colors = CardColors(
                containerColor = HyLiConnectColorScheme().secondaryContainer,
                contentColor = HyLiConnectColorScheme().onSecondaryContainer,
                disabledContainerColor = HyLiConnectColorScheme().surfaceVariant,
                disabledContentColor = HyLiConnectColorScheme().onSurfaceVariant
            )
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    viewModel!!.platformMap[deviceInfo.platform] ?: Icons.AutoMirrored.Outlined.HelpOutline,
                    contentDescription = null,
                    modifier = Modifier
                        .size(84.dp)
                        .padding(top = 12.dp, bottom = 12.dp)
                )
                Column(
                    modifier = Modifier.padding(top = 12.dp, bottom = 12.dp, end = 12.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = when (deviceInfo.uuid) {
                                PreferencesDataStore.uuid.getBlocking()!! -> {
                                    deviceInfo.nickname + " (" + stringResource(id = R.string.page_connect_this_device) + ")"
                                }
                                else -> { deviceInfo.nickname }
                            },
                            style = HyLiConnectTypography.titleMedium
                        )
                    }
                    FlowRow {
                        if (deviceInfo.platform != "") {
                            Card(
                                modifier = Modifier.padding(end = 6.dp, top = 4.dp),
                                colors = CardColors(
                                    containerColor = HyLiConnectColorScheme().tertiaryContainer,
                                    contentColor = HyLiConnectColorScheme().onTertiaryContainer,
                                    disabledContainerColor = HyLiConnectColorScheme().surfaceVariant,
                                    disabledContentColor = HyLiConnectColorScheme().onSurfaceVariant
                                )
                            ) {
                                Text(
                                    text = deviceInfo.platform,
                                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp),
                                    style = HyLiConnectTypography.labelMedium
                                )
                            }
                        }
                        if (deviceInfo.ipAddress.isNotEmpty()) {
                            deviceInfo.ipAddress.forEach {
                                Card(
                                    modifier = Modifier.padding(end = 6.dp, top = 4.dp),
                                    colors = CardColors(
                                        containerColor = HyLiConnectColorScheme().tertiaryContainer,
                                        contentColor = HyLiConnectColorScheme().onTertiaryContainer,
                                        disabledContainerColor = HyLiConnectColorScheme().surfaceVariant,
                                        disabledContentColor = HyLiConnectColorScheme().onSurfaceVariant
                                    )
                                ) {
                                    Text(
                                        text = it,
                                        modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp),
                                        style = HyLiConnectTypography.labelMedium
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        text = deviceInfo.uuid,
                        style = HyLiConnectTypography.bodySmall,
                        color = HyLiConnectColorScheme().outline,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
    LaunchedEffect(deviceInfo) {
        visibility.value = true
        if (connected) {
            while (HyLiConnect.deviceInfoMap.containsKey(deviceInfo.uuid)) delay(500)
            visibility.value = false
            delay(400)
            viewModel?.connectedDeviceMap?.remove(deviceInfo.uuid)
        } else {
            while (HyLiConnect.deviceInfoMap.containsKey(deviceInfo.uuid).not()) delay(500)
            visibility.value = false
            viewModel!!.connectedDeviceMap[deviceInfo.uuid] = HyLiConnect.deviceInfoMap[deviceInfo.uuid]!!
            delay(350)
            viewModel?.nsdDeviceMap?.remove(deviceInfo.uuid)
        }
    }
}

@Composable
private fun EmptyDeviceCard(viewModel: HyLiConnectViewModel) {
    val visibility = remember { mutableStateOf(false) }
    val isIconVisible = remember { mutableStateOf(true) }
    val iconList = viewModel.platformMap.toList().map { it.second }.distinct()
    val icon = remember { mutableStateOf(iconList[0]) }
    LaunchedEffect(iconList) {
        MainScope().launch {
            visibility.value = true
            delay(1000)
            while (viewModel.nsdDeviceMap.isEmpty()) {
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
    AnimatedVisibility(
        visible = visibility.value,
        enter = fadeIn(animationSpec = tween(400)),
        exit = fadeOut(animationSpec = tween(400))
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            colors = CardColors(
                containerColor = HyLiConnectColorScheme().secondaryContainer,
                contentColor = HyLiConnectColorScheme().onSecondaryContainer,
                disabledContainerColor = HyLiConnectColorScheme().surfaceVariant,
                disabledContentColor = HyLiConnectColorScheme().onSurfaceVariant
            )
        ) {
            BoxWithConstraints {
                this.maxWidth
                Box {
                    Row {
                        AnimatedVisibility(
                            visible = isIconVisible.value,
                            enter = fadeIn(animationSpec = tween(400)),
                            exit = fadeOut(animationSpec = tween(400))
                        ) {
                            Row(modifier = Modifier.fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    icon.value,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(84.dp)
                                        .padding(top = 12.dp, bottom = 12.dp)
                                )
                            }
                        }
                    }
                    Column(
                        modifier = Modifier
                            .padding(top = 12.dp, bottom = 12.dp, end = 12.dp)
                            .align(Alignment.TopStart)
                            .offset(x = 84.dp)
                    ) {
                        Card(
                            modifier = Modifier.padding(end = 6.dp, top = 4.dp),
                            colors = CardColors(
                                containerColor = HyLiConnectColorScheme().tertiaryContainer,
                                contentColor = HyLiConnectColorScheme().onTertiaryContainer,
                                disabledContainerColor = HyLiConnectColorScheme().surfaceVariant,
                                disabledContentColor = HyLiConnectColorScheme().onSurfaceVariant
                            )
                        ) {
                            Text(text = "                           ", style = HyLiConnectTypography.titleMedium)
                        }
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                            Card(
                                modifier = Modifier.padding(end = 6.dp, top = 4.dp),
                                colors = CardColors(
                                    containerColor = HyLiConnectColorScheme().tertiaryContainer,
                                    contentColor = HyLiConnectColorScheme().onTertiaryContainer,
                                    disabledContainerColor = HyLiConnectColorScheme().surfaceVariant,
                                    disabledContentColor = HyLiConnectColorScheme().onSurfaceVariant
                                )
                            ) {
                                Text(
                                    text = "              ",
                                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp),
                                    style = HyLiConnectTypography.labelMedium
                                )
                            }
                            Card(
                                modifier = Modifier.padding(end = 6.dp, top = 4.dp),
                                colors = CardColors(
                                    containerColor = HyLiConnectColorScheme().tertiaryContainer,
                                    contentColor = HyLiConnectColorScheme().onTertiaryContainer,
                                    disabledContainerColor = HyLiConnectColorScheme().surfaceVariant,
                                    disabledContentColor = HyLiConnectColorScheme().onSurfaceVariant
                                )
                            ) {
                                Text(
                                    text = "                      ",
                                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp),
                                    style = HyLiConnectTypography.labelMedium
                                )
                            }
                        }
                        Text(
                            text = " ",
                            style = HyLiConnectTypography.bodySmall,
                            color = HyLiConnectColorScheme().outline,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DeviceCardPreview() {
    DeviceCard(
        DeviceInfo(
            apiVersion = 1,
            appVersion = 10000,
            appVersionName = "1.0.0",
            platform = "Android Phone",
            uuid = "38299469-a3bc-48e9-b2c8-be8410650d87",
            nickname = "Windows Subsystem for Android(TM)",
            ipAddress = mutableListOf("172.30.166.176", "127.0.0.1"),
            serverPort = 15372
        )
    )
}
