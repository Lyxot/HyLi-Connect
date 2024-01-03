package xyz.hyli.connect.ui

import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import xyz.hyli.connect.R
import xyz.hyli.connect.bean.DeviceInfo
import xyz.hyli.connect.ui.state.HyliConnectState
import xyz.hyli.connect.utils.PermissionUtils

class HyliConnectViewModel: ViewModel() {
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
        "ADB" to R.string.permission_adb,
        "Root" to R.string.permission_root,
        "Xposed" to R.string.permission_xposed,
        "Accessibility" to R.string.permission_accessibility,
        "NotificationListener" to R.string.permission_notification_listener,
    )
    val keyPermissionList = listOf(
        "Overlay"
    )
    val streamPermissionList = listOf(
        "Shizuku",
        "ADB",
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
        if ( ConfigHelper.getConfigMap()["is_stream"] == true ) {
            var streamPermissionState = false
            streamPermissionList.forEach {
                if ( HyliConnectState.permissionStateMap[it] == true ) {
                    streamPermissionState = true
                }
            }
            var refuseFullScreenPermissionState = false
            refuseFullScreenPermissionList.forEach {
                if ( HyliConnectState.permissionStateMap[it] == true ) {
                    refuseFullScreenPermissionState = true
                }
            }
            state2 = streamPermissionState && refuseFullScreenPermissionState
        }
        // TODO: optional permission
        permissionState.value = state1 && state2
    }
    private fun checkPermission(context: Context) {
        HyliConnectState.permissionStateMap["Overlay"] = PermissionUtils.checkOverlayPermission(context)
    }
}

class HyliConnectViewModelFactory(): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return HyliConnectViewModel() as T
    }
}