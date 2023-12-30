package xyz.hyli.connect.ui

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import xyz.hyli.connect.bean.DeviceInfo

class HyliConnectViewModel: ViewModel() {
    val nsdDeviceMap = mutableStateMapOf<String,DeviceInfo>()
    val connectDeviceVisibilityMap = mutableStateMapOf<String, MutableState<Boolean>>()
    val connectedDeviceMap = mutableStateMapOf<String, DeviceInfo>()
}

class HyliConnectViewModelFactory(): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return HyliConnectViewModel() as T
    }
}