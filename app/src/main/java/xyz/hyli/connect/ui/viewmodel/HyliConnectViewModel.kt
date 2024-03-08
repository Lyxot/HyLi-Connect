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
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import xyz.hyli.connect.HyliConnect
import xyz.hyli.connect.R
import xyz.hyli.connect.bean.DeviceInfo
import xyz.hyli.connect.datastore.PreferencesDataStore
import xyz.hyli.connect.hook.utils.HookTest
import xyz.hyli.connect.utils.PermissionUtils
import java.io.Serializable

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
                delay(1000)
            }
        }
    }
    val localBroadcastManager = mutableStateOf<LocalBroadcastManager?>(null)
    val currentSelect = mutableIntStateOf(0)
    val applicationState = mutableStateOf("error")
    var nsdDeviceMap = mutableStateMapOf<String, DeviceInfo>()
    val connectedDeviceMap = mutableStateMapOf<String, DeviceInfo>()

    val serviceMap = mapOf(
        "SocketServer" to R.string.service_socket_server,
        "NsdService" to R.string.service_nsd_service,
        "SocketService" to R.string.service_socket_service
    )
    val permissionOverlay = listOf(Permission.SYSTEM_ALERT_WINDOW)
    val permissionNotificationListener = listOf(Permission.BIND_NOTIFICATION_LISTENER_SERVICE)
    val permissionShizuku = listOf("Shizuku")
    val permissionRoot = listOf("Root")
    val permissionXposed = listOf("Xposed")
    val permissionMap = mapOf(
        permissionOverlay to R.string.permission_overlay,
        permissionNotificationListener to R.string.permission_notification_listener,
        permissionShizuku to R.string.permission_shizuku,
        permissionRoot to R.string.permission_root,
        permissionXposed to R.string.permission_xposed,
    )
    val thirdPartyPermissionSet = setOf(permissionShizuku, permissionRoot, permissionXposed)

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
            if (applicationState.value != "rebooting") applicationState.value = "error"
        } else {
            applicationState.value = "running"
        }
        return applicationState.value
    }
    fun getRequiredPermissions(): Set<List<String>> {
        val nativePermissionSet = mutableSetOf<List<String>>()
        nativePermissionSet.add(permissionOverlay)
        if (PreferencesDataStore.function_notification_forward.getBlocking() == true) nativePermissionSet.add(permissionNotificationListener)
        return nativePermissionSet
    }
    fun getThirdPartyPermissions(): Set<List<String>> {
        val thirdPartyPermissionSet = mutableSetOf<List<String>>()
        when (PreferencesDataStore.working_mode.getBlocking()) {
            1 -> thirdPartyPermissionSet.add(permissionShizuku)
            2 -> thirdPartyPermissionSet.add(permissionRoot)
        }
        return thirdPartyPermissionSet
    }
}

class HyliConnectViewModelFactory() : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return HyliConnectViewModel() as T
    }
}
