package xyz.hyli.connect.ui

import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import xyz.hyli.connect.R
import xyz.hyli.connect.bean.DeviceInfo
import xyz.hyli.connect.datastore.PreferencesDataStore
import xyz.hyli.connect.hook.utils.HookTest
import xyz.hyli.connect.socket.SocketData
import xyz.hyli.connect.ui.state.HyliConnectState
import xyz.hyli.connect.utils.PermissionUtils

class HyliConnectViewModel: ViewModel() {
    init {
        viewModelScope.launch(context = Dispatchers.Main) {
            delay(1000)
            while (true) {
                try {
                    SocketData.uuidMap.forEach {
                        if ( nsdDeviceMap.containsKey(it.value) && SocketData.deviceInfoMap.containsKey(it.value) && connectedDeviceMap.containsKey(it.value).not() ) {
                            connectedDeviceMap[it.value] = SocketData.deviceInfoMap[it.value]!!
                            nsdDeviceMap.remove(it.value)
                            connectDeviceVisibilityMap[it.value]!!.value = false
                        } else if ( SocketData.deviceInfoMap.containsKey(it.value) && connectedDeviceMap.containsKey(it.value).not() ) {
                            connectedDeviceMap[it.value] = SocketData.deviceInfoMap[it.value]!!
                            connectDeviceVisibilityMap[it.value]!!.value = false
                        }
                    }
                } catch (_: Exception) { }
                try {
                    connectedDeviceMap.keys.toMutableList().forEach {
                        if ( SocketData.deviceInfoMap.containsKey(it).not() ) {
                            connectDeviceVisibilityMap[it]!!.value = false
                            delay(500)
                            connectedDeviceMap.remove(it)
                            connectDeviceVisibilityMap.remove(it)
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
    val applicationState = mutableStateOf("error")
    val permissionState = mutableStateOf(false)
    val nsdDeviceMap = mutableStateMapOf<String,DeviceInfo>()
    val connectDeviceVisibilityMap = mutableStateMapOf<String, MutableState<Boolean>>()
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
    val keyPermissionList = mutableListOf(
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

    fun updateApplicationState() {
        val serviceStateList: MutableList<String> = mutableListOf()
        serviceMap.keys.forEach {
            if ( HyliConnectState.serviceStateMap.containsKey(it) ) {
                serviceStateList.add(HyliConnectState.serviceStateMap[it]!!.state)
            } else {
//                HyliConnectState.serviceStateMap[it] = ServiceState("stopped", getString(R.string.state_service_stopped, getString(serviceMap[it]!!)))
                serviceStateList.add("stopped")
            }
        }
        if ( serviceStateList.contains("stopped") ) {
            applicationState.value = "stopped"
        } else if ( serviceStateList.contains("error") ) {
            applicationState.value = "error"
        } else {
            applicationState.value = "running"
        }
    }
    fun updatePermissionState(context: Context) {
        checkPermission(context)
        var state1 = true
        keyPermissionList.forEach {
            if ( HyliConnectState.permissionStateMap[it] != true ) {
                HyliConnectState.permissionStateMap[it] = false
                state1 = false
            }
        }
        var state2 = true
        if (PreferencesDataStore.getConfigMap()["is_stream"] == true) {
            state2 = (HyliConnectState.permissionStateMap[PreferencesDataStore.getConfigMap()["stream_method"]] == true) &&
                    (HyliConnectState.permissionStateMap[PreferencesDataStore.getConfigMap()["refuse_full_screen_method"]] == true)
        }
        // TODO: optional permission
        permissionState.value = state1 && state2
    }
    private fun checkPermission(context: Context) {
        HyliConnectState.permissionStateMap["Overlay"] = PermissionUtils.checkOverlayPermission(context)
        if (PreferencesDataStore.getConfigMap()["is_stream"] == true) {
            keyPermissionList.add(PreferencesDataStore.getConfigMap()["stream_method"].toString())
            keyPermissionList.add(PreferencesDataStore.getConfigMap()["refuse_full_screen_method"].toString())
            if (PreferencesDataStore.getConfigMap()["refuse_full_screen_method"] == "Xposed") {
                HyliConnectState.permissionStateMap["Xposed"] = HookTest().checkXposed()
            }
        }
    }
}

class HyliConnectViewModelFactory(): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return HyliConnectViewModel() as T
    }
}