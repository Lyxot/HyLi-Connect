package xyz.hyli.connect.socket

import android.util.Log
import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import xyz.hyli.connect.BuildConfig
import xyz.hyli.connect.ui.main.ConfigHelper
import xyz.hyli.connect.utils.PackageUtils
import java.io.OutputStreamWriter
import java.io.PrintWriter

object MessageHandler {
    fun messageHandler(ip: String, message: String) {
        if (message == "") return

        val messageJson = JSONObject.parseObject(message)
        val messageType = messageJson.getString("message_type")?:""

        if ( messageType == "" ) return

        when (messageType) {
            "request" -> requestHandler(ip, messageJson)
            "response" -> responseHandler(ip, messageJson)
            "broadcast" -> broadcastHandler(ip, messageJson)
            "forward" -> forwardHandler(ip, messageJson)
        }
    }
    fun requestHandler(ip: String, messageJson: JSONObject) {
        val command = messageJson.getString("command")?:""
        val data = messageJson.getJSONObject("data")?:JSONObject()
        val uuid = data.getString("uuid")?:""
        if ( command == "" || data == JSONObject() || uuid == "" ) return

        val responseJson = JSONObject()
        responseJson["message_type"] = "response"
        responseJson["command"] = command
        responseJson["uuid"] = ConfigHelper.uuid
        val responseData = JSONObject()

        if ( command in SocketConfig.NON_AUTH_COMMAND ) {
            if ( command == COMMAND_GET_INFO ) {
                responseData["api_version"] = API_VERSION
                responseData["app_version"] = BuildConfig.VERSION_CODE
                responseData["app_version_name"] = BuildConfig.VERSION_NAME
                responseData["platform"] = PLATFORM
                responseData["uuid"] = ConfigHelper.uuid
                responseData["nickname"] = ConfigHelper.NICKNAME
                sendMessage(ip, responseJson, responseData)
                SocketConfig.uuidMap.remove(uuid)
                SocketConfig.nicknameMap.remove(uuid)
                SocketConfig.clientMap.remove(ip)
                SocketConfig.outputStreamMap.remove(ip)
            }
        } else if ( command in SocketConfig.AUTH_COMMAND ) {
            if ( command == COMMAND_CONNECT) {
                responseData["uuid"] = ConfigHelper.uuid
                responseData["ip_address"] = ConfigHelper.IP_ADDRESS
                responseData["nickname"] = ConfigHelper.NICKNAME

                // temporary
                SocketConfig.uuidMap[uuid] = ip
                SocketConfig.nicknameMap[uuid] = data.getString("nickname")?:""
                sendMessage(ip, responseJson, responseData)
            } else if ( uuid in SocketConfig.uuidMap ) {
                when (command) {
                    COMMAND_DISCONNECT -> {
                        sendMessage(ip, responseJson, responseData)
                        SocketConfig.uuidMap.remove(uuid)
                        SocketConfig.nicknameMap.remove(uuid)
                        SocketConfig.clientMap.remove(ip)
                        SocketConfig.outputStreamMap.remove(ip)
                    }
                    COMMAND_CLIENT_LIST -> {
                        val clientList = JSONArray()
                        SocketConfig.uuidMap.forEach{ (key, value) ->
                            val clientInfo = JSONObject()
                            clientInfo["ip_address"] = value
                            clientInfo["nickname"] = SocketConfig.nicknameMap[key]
                            clientList.add(clientInfo)
                        }
                        responseData["client_list"] = clientList
                        sendMessage(ip, responseJson, responseData)
                    }
                    COMMAND_APP_LIST -> {
                        val appList = JSONObject()
                        PackageUtils.appNameMap.forEach{ (key, value) ->
                            val appInfo = JSONObject()
                            appInfo["name"] = value
                            appInfo["width"] = PackageUtils.appWidthMap[key]
                            appInfo["height"] = PackageUtils.appHeightMap[key]
                            appInfo["icon"] = PackageUtils.appIconByteMap[key]
                            appList[key] = value
                        }
                        responseData["app_list"] = appList
                        sendMessage(ip, responseJson, responseData)
                    }
                }
            }
        }
    }
    fun responseHandler(ip: String, messageJson: JSONObject) {

    }
    fun broadcastHandler(ip: String, messageJson: JSONObject) {

    }
    fun forwardHandler(ip: String, messageJson: JSONObject) {

    }
    fun sendMessage(ip: String, messageJson: JSONObject, messageData: JSONObject) {
        messageJson["data"] = messageData
        messageJson["status"] = "success"
        val writerPrinter = PrintWriter(OutputStreamWriter(SocketConfig.outputStreamMap[ip]), true)
        writerPrinter.println(messageJson.toString())
        Log.i("MessageHandler", "Send message: ${messageJson.toString()}")
    }
}