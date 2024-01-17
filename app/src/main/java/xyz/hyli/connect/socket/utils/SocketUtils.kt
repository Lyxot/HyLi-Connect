package xyz.hyli.connect.socket.utils

import android.util.Log
import com.alibaba.fastjson2.JSONObject
import xyz.hyli.connect.BuildConfig
import xyz.hyli.connect.HyliConnect
import xyz.hyli.connect.bean.DeviceInfo
import xyz.hyli.connect.datastore.PreferencesDataStore
import xyz.hyli.connect.socket.API_VERSION
import xyz.hyli.connect.socket.COMMAND_CONNECT
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.util.UUID.randomUUID
import kotlin.concurrent.thread

object SocketUtils {
    fun closeConnection(ip: String) {
        HyliConnect.socketMap[ip]?.close()
        HyliConnect.deviceInfoMap.remove(HyliConnect.uuidMap[ip] ?: "")
        HyliConnect.uuidMap.remove(ip)
        HyliConnect.socketMap.remove(ip)
        HyliConnect.inputStreamMap.remove(ip)
        HyliConnect.outputStreamMap.remove(ip)
        HyliConnect.connectionMap.remove(ip)
    }
    fun closeAllConnection() {
        HyliConnect.socketMap.forEach {
            it.value.close()
        }
        HyliConnect.uuidMap.clear()
        HyliConnect.socketMap.clear()
        HyliConnect.inputStreamMap.clear()
        HyliConnect.outputStreamMap.clear()
        HyliConnect.connectionMap.clear()
        HyliConnect.deviceInfoMap.clear()
    }
    fun acceptConnection(ip: String, data: JSONObject) {
        HyliConnect.uuidMap[ip] = data.getString("uuid") ?: ""
        val messageJson = JSONObject()
        val messageData = JSONObject()
        messageJson["message_type"] = "response"
        messageJson["command"] = COMMAND_CONNECT
        messageData["api_version"] = API_VERSION
        messageData["app_version"] = BuildConfig.VERSION_CODE
        messageData["app_version_name"] = BuildConfig.VERSION_NAME
        messageData["platform"] = PreferencesDataStore.getConfigMap()["platform"].toString()
        messageData["uuid"] = PreferencesDataStore.getConfigMap()["uuid"].toString()
        messageData["nickname"] = PreferencesDataStore.getConfigMap()["nickname"].toString()
        messageJson["data"] = messageData
        messageJson["uuid"] = PreferencesDataStore.getConfigMap()["uuid"].toString()
        HyliConnect.deviceInfoMap[data.getString("uuid") ?: ""] = DeviceInfo(
            data.getIntValue("api_version"),
            data.getIntValue("app_version"),
            data.getString("app_version_name") ?: "",
            data.getString("platform") ?: "",
            data.getString("uuid") ?: "",
            data.getString("nickname") ?: "",
            mutableListOf(ip.substring(1, ip.length).split(":")[0]),
            ip.substring(1, ip.length).split(":").last().toInt()
        )
        thread { sendMessage(ip, messageJson, "accept") }
    }
    fun rejectConnection(ip: String) {
        val messageJson = JSONObject()
        val messageData = JSONObject()
        messageJson["message_type"] = "response"
        messageJson["command"] = COMMAND_CONNECT
        messageData["uuid"] = PreferencesDataStore.getConfigMap()["uuid"].toString()
        messageData["nickname"] = PreferencesDataStore.getConfigMap()["nickname"].toString()
        messageJson["data"] = messageData
        messageJson["uuid"] = PreferencesDataStore.getConfigMap()["uuid"].toString()
        thread {
            sendMessage(ip, messageJson, "reject")
            closeConnection(ip)
        }
    }
    fun sendHeartbeat(ip: String) {
        val messageJson = JSONObject()
        val messageData = JSONObject()
        messageJson["message_type"] = "heartbeat"
        messageData["timestamp"] = System.currentTimeMillis()
        messageJson["data"] = messageData
        messageJson["uuid"] = PreferencesDataStore.getConfigMap()["uuid"].toString()
        thread { sendMessage(ip, messageJson) }
    }
    fun connect(ip: String, port: Int) {
        val t = System.currentTimeMillis()
//        SocketClient.start(ip, port)
        val IPAddress = "/$ip:$port"
        val messageJson = JSONObject()
        val messageData = JSONObject()
        messageJson["message_type"] = "request"
        messageJson["command"] = COMMAND_CONNECT
        messageData["api_version"] = API_VERSION
        messageData["app_version"] = BuildConfig.VERSION_CODE
        messageData["app_version_name"] = BuildConfig.VERSION_NAME
        messageData["platform"] = PreferencesDataStore.getConfigMap()["platform"].toString()
        messageData["uuid"] = PreferencesDataStore.getConfigMap()["uuid"].toString()
        messageData["nickname"] = PreferencesDataStore.getConfigMap()["nickname"].toString()
        messageJson["data"] = messageData
        messageJson["uuid"] = PreferencesDataStore.getConfigMap()["uuid"].toString()
        while ( HyliConnect.socketMap[IPAddress] == null && System.currentTimeMillis() - t < 4800 ) {
            Thread.sleep(20)
        }
        if ( HyliConnect.socketMap[IPAddress] == null ) {
            Log.e("SocketUtils", "Connect timeout")
            return
        }
        thread { sendMessage(IPAddress, messageJson) }
    }
    fun sendMessage(ip: String, messageJson: JSONObject, status: String = "success") {
        messageJson["message_id"] = randomUUID().toString()
        messageJson["status"] = status
        try {
            val outputStream = HyliConnect.outputStreamMap[ip]
            if (outputStream != null) {
                val writerPrinter = PrintWriter(OutputStreamWriter(outputStream), true)
                writerPrinter.println(messageJson.toString())
                Log.i("SocketUtils", "Send message: $ip $messageJson")
            }
        } catch (e: Exception) {
            Log.e("SocketUtils", "Send message error: $ip ${e.message}")
//            closeConnection(ip)
        }
    }
}