package xyz.hyli.connect.ui.state

import xyz.hyli.connect.bean.ServiceState

object HyliConnectState {
    // service name, state
    val serviceStateMap = mutableMapOf<String, ServiceState>()
    val permissionStateMap = mutableMapOf<String, Boolean>()
}