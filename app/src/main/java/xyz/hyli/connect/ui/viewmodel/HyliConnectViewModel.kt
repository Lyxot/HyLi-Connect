package xyz.hyli.connect.ui.viewmodel

import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import xyz.hyli.connect.HyliConnect
import xyz.hyli.connect.R
import xyz.hyli.connect.bean.DeviceInfo
import xyz.hyli.connect.datastore.PreferencesDataStore
import xyz.hyli.connect.hook.utils.HookTest
import xyz.hyli.connect.utils.PermissionUtils

class HyliConnectViewModel : ViewModel() {
    init {
        viewModelScope.launch(context = Dispatchers.Main) {
            delay(500)
            while (true) {
                try {
                    HyliConnect.uuidMap.forEach {
                        if (HyliConnect.deviceInfoMap.containsKey(it.value) && connectedDeviceMap.containsKey(it.value).not()) {
                            connectedDeviceMap[it.value] = HyliConnect.deviceInfoMap[it.value]!!
                        }
                    }
                } catch (_: Exception) { }
                try {
                    updateApplicationState()
                } catch (_: Exception) { }
                delay(1000)
            }
        }
    }
    val localBroadcastManager = mutableStateOf<LocalBroadcastManager?>(null)
    val currentSelect = mutableIntStateOf(0)
    val applicationState = mutableStateOf("error")
    val permissionState = mutableStateOf(false)
    var nsdDeviceMap = mutableStateMapOf<String, DeviceInfo>()
    val connectedDeviceMap = mutableStateMapOf<String, DeviceInfo>()

    val serviceMap = mapOf(
        "SocketServer" to R.string.service_socket_server,
        "NsdService" to R.string.service_nsd_service,
        "SocketService" to R.string.service_socket_service
    )
    val permissionMap = mapOf(
        "Overlay" to R.string.permission_overlay,
        "Shizuku" to R.string.permission_shizuku,
        "Root" to R.string.permission_root,
        "Xposed" to R.string.permission_xposed,
        "Accessibility" to R.string.permission_accessibility,
        "NotificationListener" to R.string.permission_notification_listener,
    )
    var keyPermissionList = mutableListOf(
        "Overlay"
    )
    val streamPermissionList = listOf(
        "Shizuku",
        "Root"
    )
    val refuseFullScreenPermissionList = listOf(
        "Xposed",
        "Shizuku"
    )
    val optionalPermission = listOf(
        "Accessibility",
        "NotificationListener",
//        "UsageStats"
    )

    fun updateApplicationState(): String {
        val serviceStateList: MutableList<String> = mutableListOf()
        serviceMap.keys.forEach {
            if (HyliConnect.serviceStateMap.containsKey(it)) {
                if (it == "NsdService" && PreferencesDataStore.nsd_service.getBlocking() == false) {
                    serviceStateList.add("running")
                } else {
                    serviceStateList.add(HyliConnect.serviceStateMap[it]!!.state)
                }
            } else {
//                HyliConnectState.serviceStateMap[it] = ServiceState("stopped", getString(R.string.state_service_stopped, getString(serviceMap[it]!!)))
                serviceStateList.add("stopped")
            }
        }
        if (serviceStateList.contains("stopped")) {
            if (applicationState.value != "rebooting") applicationState.value = "stopped"
        } else if (serviceStateList.contains("error")) {
            if (applicationState.value != "rebooting") applicationState.value = "rebooting"
        } else {
            applicationState.value = "running"
        }
        return applicationState.value
    }
    fun updatePermissionState(context: Context): Boolean {
        checkPermission(context)
        var state1 = true
        keyPermissionList.forEach {
            if (HyliConnect.permissionStateMap[it] != true && permissionMap.containsKey(it)) {
                HyliConnect.permissionStateMap[it] = false
                state1 = false
            }
        }
        // TODO: optional permission
        permissionState.value = state1
        return permissionState.value
    }
    private fun checkPermission(context: Context) {
        keyPermissionList = mutableListOf(
            "Overlay"
        )
        HyliConnect.permissionStateMap["Overlay"] = PermissionUtils.checkOverlayPermission(context)
        if (PreferencesDataStore.function_app_streaming.getBlocking() == true) {
            when (PreferencesDataStore.working_mode.getBlocking()) {
                1 -> {
                    keyPermissionList.add("Shizuku")
                }
                2 -> {
                    keyPermissionList.add("Root")
                }
            }
            if (keyPermissionList.contains("Xposed")) {
                HyliConnect.permissionStateMap["Xposed"] = HookTest().checkXposed()
            }
        }
        if (PreferencesDataStore.function_notification_forward.getBlocking() == true) {
            keyPermissionList.add("NotificationListener")
            HyliConnect.permissionStateMap["NotificationListener"] = PermissionUtils.checkNotificationListenerPermission(context)
        }
    }
}

class HyliConnectViewModelFactory() : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return HyliConnectViewModel() as T
    }
}
