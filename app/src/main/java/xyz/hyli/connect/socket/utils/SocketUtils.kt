package xyz.hyli.connect.socket.utils

import android.util.Log
import com.alibaba.fastjson2.JSONObject
import xyz.hyli.connect.BuildConfig
import xyz.hyli.connect.bean.DeviceInfo
import xyz.hyli.connect.socket.API_VERSION
import xyz.hyli.connect.socket.COMMAND_CONNECT
import xyz.hyli.connect.socket.PLATFORM
import xyz.hyli.connect.socket.SERVER_PORT
import xyz.hyli.connect.socket.SocketConfig
import xyz.hyli.connect.ui.ConfigHelper
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.util.UUID.randomUUID
import kotlin.concurrent.thread

object SocketUtils {
    fun closeConnection(ip: String) {
        SocketConfig.socketMap[ip]?.close()
        SocketConfig.deviceInfoMap.remove(SocketConfig.uuidMap[ip] ?: "")
        SocketConfig.uuidMap.remove(ip)
        SocketConfig.socketMap.remove(ip)
        SocketConfig.inputStreamMap.remove(ip)
        SocketConfig.outputStreamMap.remove(ip)
        SocketConfig.connectionMap.remove(ip)
    }
    fun closeAllConnection() {
        SocketConfig.socketMap.forEach {
            it.value.close()
        }
        SocketConfig.uuidMap.clear()
        SocketConfig.socketMap.clear()
        SocketConfig.inputStreamMap.clear()
        SocketConfig.outputStreamMap.clear()
        SocketConfig.connectionMap.clear()
        SocketConfig.deviceInfoMap.clear()
    }
    fun acceptConnection(ip: String, data: JSONObject) {
        SocketConfig.uuidMap[ip] = data.getString("uuid") ?: ""
        val messageJson = JSONObject()
        val messageData = JSONObject()
        messageJson["message_type"] = "response"
        messageJson["command"] = COMMAND_CONNECT
        messageData["api_version"] = API_VERSION
        messageData["app_version"] = BuildConfig.VERSION_CODE
        messageData["app_version_name"] = BuildConfig.VERSION_NAME
        messageData["platform"] = PLATFORM
        messageData["uuid"] = ConfigHelper.uuid
        messageData["nickname"] = ConfigHelper.NICKNAME
        messageJson["data"] = messageData
        messageJson["uuid"] = ConfigHelper.uuid
        SocketConfig.deviceInfoMap[data.getString("uuid") ?: ""] = DeviceInfo(
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
        messageData["uuid"] = ConfigHelper.uuid
        messageData["nickname"] = ConfigHelper.NICKNAME
        messageJson["data"] = messageData
        messageJson["uuid"] = ConfigHelper.uuid
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
        messageJson["uuid"] = ConfigHelper.uuid
        thread { sendMessage(ip, messageJson) }
    }
    fun connect(ip: String, port: Int = SERVER_PORT) {
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
        messageData["platform"] = PLATFORM
        messageData["uuid"] = ConfigHelper.uuid
        messageData["nickname"] = ConfigHelper.NICKNAME
        messageJson["data"] = messageData
        messageJson["uuid"] = ConfigHelper.uuid
        while ( SocketConfig.socketMap[IPAddress] == null && System.currentTimeMillis() - t < 4800 ) {
            Thread.sleep(20)
        }
        if ( SocketConfig.socketMap[IPAddress] == null ) {
            Log.e("SocketUtils", "Connect timeout")
            return
        }
        thread { sendMessage(IPAddress, messageJson) }
    }
    fun sendMessage(ip: String, messageJson: JSONObject, status: String = "success") {
        messageJson["message_id"] = randomUUID().toString()
        messageJson["status"] = status
        try {
            val outputStream = SocketConfig.outputStreamMap[ip]
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