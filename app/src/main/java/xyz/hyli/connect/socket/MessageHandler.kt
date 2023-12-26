package xyz.hyli.connect.socket

import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import xyz.hyli.connect.BuildConfig
import xyz.hyli.connect.socket.utils.SocketUtils
import xyz.hyli.connect.ui.ConfigHelper
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
        responseJson["uuid"] = ConfigHelper.uuid
        val responseData = JSONObject()

        if (command in SocketConfig.NON_AUTH_COMMAND) {
            if (command == COMMAND_GET_INFO) {
                responseData["api_version"] = API_VERSION
                responseData["app_version"] = BuildConfig.VERSION_CODE
                responseData["app_version_name"] = BuildConfig.VERSION_NAME
                responseData["platform"] = PLATFORM
                responseData["uuid"] = ConfigHelper.uuid
                responseData["nickname"] = ConfigHelper.NICKNAME
                responseJson["data"] = responseData
                SocketUtils.sendMessage(ip, responseJson)
                SocketUtils.closeConnection(ip)
            }
        } else if (command in SocketConfig.AUTH_COMMAND) {
            when (command) {
                COMMAND_CONNECT -> {
                    responseData["uuid"] = ConfigHelper.uuid
                    responseData["nickname"] = ConfigHelper.NICKNAME

                    // temporary
                    SocketUtils.acceptConnection(ip, data.getString("nickname") ?: "", uuid)
//                val intent = Intent("xyz.hyli.connect.service.SocketService.action.CONNECT_REQUEST")
//                intent.putExtra("command", "connect")
//                intent.putExtra("ip", ip)
//                intent.putExtra("nickname", data.getString("nickname")?:"")
//                intent.putExtra("uuid", uuid)
//                broadcastManager?.sendBroadcast(intent)
                }

                COMMAND_DISCONNECT -> {
                    SocketUtils.closeConnection(ip)
                }
            }
        } else if (uuid in SocketConfig.uuidMap.values) {
            when (command) {
                COMMAND_CLIENT_LIST -> {
                    val clientList = JSONArray()
                    SocketConfig.uuidMap.forEach { (key, value) ->
                        val clientInfo = JSONObject()
                        clientInfo["uuid"] = value
                        clientInfo["ip_address"] = key
                        clientInfo["nickname"] = SocketConfig.nicknameMap[key]
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
        SocketConfig.connectionMap[ip] = System.currentTimeMillis()
        Thread.sleep(3000)
        if (System.currentTimeMillis() - SocketConfig.connectionMap[ip]!! >= 3000 || SocketConfig.connectionMap.containsKey(ip).not() || SocketConfig.connectionMap[ip] == null) {
            SocketUtils.sendHeartbeat(ip)
        }
    }
}
