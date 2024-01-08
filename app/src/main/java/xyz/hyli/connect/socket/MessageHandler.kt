package xyz.hyli.connect.socket

import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import xyz.hyli.connect.BuildConfig
import xyz.hyli.connect.bean.DeviceInfo
import xyz.hyli.connect.datastore.PreferencesDataStore
import xyz.hyli.connect.socket.utils.SocketUtils
import xyz.hyli.connect.utils.PackageUtils

object MessageHandler {
    fun messageHandler(
        ip: String,
        message: String,
        broadcastManager: LocalBroadcastManager? = null
    ) {
        if (message == "") return

        val messageJson = JSONObject.parseObject(message)
        val messageType = messageJson.getString("message_type") ?: ""

        if (messageType == "") return

        when (messageType) {
            "request" -> requestHandler(ip, messageJson, broadcastManager)
            "response" -> responseHandler(ip, messageJson, broadcastManager)
            "broadcast" -> broadcastHandler(ip, messageJson, broadcastManager)
            "forward" -> forwardHandler(ip, messageJson, broadcastManager)
            "heartbeat" -> heartbeatHandler(ip, messageJson, broadcastManager)
        }
    }

    private fun requestHandler(
        ip: String,
        messageJson: JSONObject,
        broadcastManager: LocalBroadcastManager? = null
    ) {
        val command = messageJson.getString("command") ?: ""
        val data = messageJson.getJSONObject("data") ?: JSONObject()
        val uuid = data.getString("uuid") ?: ""
        if (command == "" || data == JSONObject() || uuid == "") return

        val responseJson = JSONObject()
        responseJson["message_type"] = "response"
        responseJson["command"] = command
        responseJson["uuid"] = PreferencesDataStore.getConfigMap()["uuid"].toString()
        val responseData = JSONObject()

        if (command in SocketConfig.NON_AUTH_COMMAND) {
            if (command == COMMAND_GET_INFO) {
                responseData["api_version"] = API_VERSION
                responseData["app_version"] = BuildConfig.VERSION_CODE
                responseData["app_version_name"] = BuildConfig.VERSION_NAME
                responseData["platform"] = PLATFORM
                responseData["uuid"] = PreferencesDataStore.getConfigMap()["uuid"].toString()
                responseData["nickname"] = PreferencesDataStore.getConfigMap()["nickname"].toString()
                responseJson["data"] = responseData
                SocketUtils.sendMessage(ip, responseJson)
                SocketUtils.closeConnection(ip)
            }
        } else if (command in SocketConfig.AUTH_COMMAND) {
            when (command) {
                COMMAND_CONNECT -> {
                    responseData["uuid"] = PreferencesDataStore.getConfigMap()["uuid"].toString()
                    responseData["nickname"] = PreferencesDataStore.getConfigMap()["nickname"].toString()

                    val intent = Intent("xyz.hyli.connect.service.SocketService.action.CONNECT_REQUEST").apply {
                        putExtra("command", "connect")
                        putExtra("ip", ip)
                        putExtra("nickname", data.getString("nickname")?:"")
                        putExtra("uuid", uuid)
                        putExtra("api_version", data.getIntValue("api_version"))
                        putExtra("app_version", data.getIntValue("app_version"))
                        putExtra("app_version_name", data.getString("app_version_name")?:"")
                        putExtra("platform", data.getString("platform")?:"")
                    }
                    broadcastManager?.sendBroadcast(intent)
                }

                COMMAND_DISCONNECT -> {
                    SocketUtils.closeConnection(ip)
                }
            }
        } else if (uuid in SocketData.uuidMap.values) {
            when (command) {
                COMMAND_CLIENT_LIST -> {
                    val clientList = JSONArray()
                    SocketData.uuidMap.forEach { (key, value) ->
                        val clientInfo = JSONObject()
                        clientInfo["uuid"] = value
                        clientInfo["ip_address"] = key
                        clientList.add(clientInfo)
                    }
                    responseData["client_list"] = clientList
                    responseJson["data"] = responseData
                    SocketUtils.sendMessage(ip, responseJson)
                }

                COMMAND_APP_LIST -> {
                    val appList = JSONObject()
                    PackageUtils.appNameMap.forEach { (key, value) ->
                        val appInfo = JSONObject()
                        appInfo["name"] = value
                        appInfo["width"] = PackageUtils.appWidthMap[key]
                        appInfo["height"] = PackageUtils.appHeightMap[key]
                        appInfo["icon"] = PackageUtils.appIconByteMap[key]
                        appList[key] = value
                    }
                    responseData["app_list"] = appList
                    responseJson["data"] = responseData
                    SocketUtils.sendMessage(ip, responseJson)
                }
            }
        }
    }

    private fun responseHandler(
        ip: String,
        messageJson: JSONObject,
        broadcastManager: LocalBroadcastManager? = null
    ) {
        val command = messageJson.getString("command") ?: ""
        val data = messageJson.getJSONObject("data") ?: JSONObject()
        val uuid = data.getString("uuid") ?: ""
        if (command == "" || data == JSONObject() || uuid == "") return

        when (command) {
            COMMAND_CONNECT -> {
                val status = messageJson.getString("status") ?: ""
                if (status == "accept") {
                    SocketData.uuidMap[ip] = uuid
                    val ip_address = ip.substring(1, ip.length).split(":")[0]
                    val port = ip.substring(1, ip.length).split(":").last().toInt()
                    val deviceInfo = DeviceInfo(
                        data.getIntValue("api_version"),
                        data.getIntValue("app_version"),
                        data.getString("app_version_name") ?: "",
                        data.getString("platform") ?: "",
                        uuid,
                        data.getString("nickname") ?: "",
                        mutableListOf(ip_address),
                        port
                    )
                    SocketData.deviceInfoMap[uuid] = deviceInfo
                    SocketUtils.sendHeartbeat(ip)
                } else if (status == "reject") {
                    SocketUtils.closeConnection(ip)
                }
            }
        }

    }

    private fun broadcastHandler(
        ip: String,
        messageJson: JSONObject,
        broadcastManager: LocalBroadcastManager? = null
    ) {

    }

    private fun forwardHandler(
        ip: String,
        messageJson: JSONObject,
        broadcastManager: LocalBroadcastManager? = null
    ) {

    }

    private fun heartbeatHandler(
        ip: String,
        messageJson: JSONObject,
        broadcastManager: LocalBroadcastManager? = null
    ) {
        SocketData.connectionMap[ip] = System.currentTimeMillis()
        Thread.sleep(3000)
        if (System.currentTimeMillis() - SocketData.connectionMap[ip]!! >= 3000 || SocketData.connectionMap.containsKey(ip).not() || SocketData.connectionMap[ip] == null) {
            if ( SocketData.uuidMap.containsKey(ip) ) {
                try {
                    SocketUtils.sendHeartbeat(ip)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
