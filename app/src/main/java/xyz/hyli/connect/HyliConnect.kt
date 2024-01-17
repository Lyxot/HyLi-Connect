package xyz.hyli.connect

import android.app.Application
import android.content.Context
import rikka.sui.Sui
import xyz.hyli.connect.bean.DeviceInfo
import xyz.hyli.connect.bean.ServiceState
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket

open class HyliConnect : Application() {
    companion object {
        lateinit var me: HyliConnect

        // service name, state
        lateinit var serviceStateMap: MutableMap<String, ServiceState>
        lateinit var permissionStateMap: MutableMap<String, Boolean>

        // ip: data
        lateinit var uuidMap: MutableMap<String, String>
        lateinit var socketMap: MutableMap<String, Socket>
        lateinit var inputStreamMap: MutableMap<String, InputStream>
        lateinit var outputStreamMap: MutableMap<String, OutputStream>
        lateinit var connectionMap: MutableMap<String, Long>
        // uuid: deviceInfo
        lateinit var deviceInfoMap: MutableMap<String, DeviceInfo>
    }
    override fun onCreate() {
        serviceStateMap = mutableMapOf()
        permissionStateMap = mutableMapOf()
        uuidMap = mutableMapOf()
        socketMap = mutableMapOf()
        inputStreamMap = mutableMapOf()
        outputStreamMap = mutableMapOf()
        connectionMap = mutableMapOf()
        deviceInfoMap = mutableMapOf()
        me = this
        Sui.init(BuildConfig.APPLICATION_ID)
        super.onCreate()
    }
    fun getContext(): Context {
        return me.applicationContext
    }
}