package xyz.hyli.connect.socket

import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import xyz.hyli.connect.BuildConfig
import xyz.hyli.connect.HyliConnect
import xyz.hyli.connect.bean.DeviceInfo
import xyz.hyli.connect.datastore.PreferencesDataStore
import xyz.hyli.connect.proto.ClientListProto
import xyz.hyli.connect.proto.ConnectProto
import xyz.hyli.connect.proto.InfoProto
import xyz.hyli.connect.proto.SocketMessage
import xyz.hyli.connect.ui.dialog.RequestConnectionActivity

object MessageHandler {
    fun messageHandler(
        ip: String,
        message: SocketMessage.Message,
        broadcastManager: LocalBroadcastManager? = null
    ) {
        if (!message.hasHeader() || !message.hasBody()) return
        val header = message.header
        val body = message.body
//        if (header.length != body.serializedSize) {
//            Log.e("MessageHandler", "Message length not match")
//            return
//        }

        Log.i("MessageHandler", "Received message from $ip: ${body.type} ${body.cmd}")
        Log.i("MessageHandler", "${HyliConnect.receiveMessageListenerMap}")
        HyliConnect.receiveMessageListenerMap[ip]?.filter {
            it.type == body.type && it.command == body.cmd
        }?.forEach {
            it.onMessageReceive(body)
            if (it.unregisterAfterReceived) {
                HyliConnect.receiveMessageListenerMap[ip]?.remove(it)
            }
        }

        when (body.type) {
            SocketMessage.TYPE.HEARTBEAT -> heartbeatHandler(ip, body, broadcastManager)
            SocketMessage.TYPE.REQUEST -> requestHandler(ip, body, broadcastManager)
            SocketMessage.TYPE.RESPONSE -> responseHandler(ip, body, broadcastManager)
            SocketMessage.TYPE.BROADCAST -> broadcastHandler(ip, body, broadcastManager)
            SocketMessage.TYPE.FORWARD -> forwardHandler(ip, body, broadcastManager)
            else -> return
        }
    }

    private fun requestHandler(
        ip: String,
        messageBody: SocketMessage.Body,
        broadcastManager: LocalBroadcastManager? = null
    ) {
        val command = messageBody.cmd
        val data = messageBody.data
        val uuid = messageBody.uuid
        if (data.isEmpty || uuid.isNullOrEmpty()) return
        val responseBody = SocketMessage.Body.newBuilder()
            .setType(SocketMessage.TYPE.RESPONSE)
            .setCmd(command)
            .setUuid(PreferencesDataStore.uuid.getBlocking()!!)

        if (HyliConnect.uuidMap.containsKey(ip).not()) {
            when (command) {
                SocketMessage.COMMAND.GET_INFO -> {
                    val responseData = InfoProto.Info.newBuilder()
                        .setApiVersion(API_VERSION)
                        .setAppVersion(BuildConfig.VERSION_CODE)
                        .setAppVersionName(BuildConfig.VERSION_NAME)
                        .setPlatform(PreferencesDataStore.platformMap[PreferencesDataStore.platform.getBlocking()!!])
                        .setUuid(PreferencesDataStore.uuid.getBlocking()!!)
                        .setNickname(PreferencesDataStore.nickname.getBlocking()!!)
                        .build()
                    responseBody.setData(responseData.toByteString())
                    SocketUtils.sendMessage(ip, responseBody)
                }
                SocketMessage.COMMAND.CONNECT -> {
                    HyliConnect().getContext().let {
                        val dataProto = ConnectProto.ConnectRequest.parseFrom(data)
                        it.startActivity(
                            Intent(it, RequestConnectionActivity::class.java)
                                .apply {
                                    putExtra("ip", ip)
                                    putExtra("nickname", dataProto.nickname)
                                    putExtra("uuid", dataProto.uuid)
                                    putExtra("api_version", dataProto.apiVersion)
                                    putExtra("app_version", dataProto.appVersion)
                                    putExtra("app_version_name", dataProto.appVersionName)
                                    putExtra("platform", dataProto.platform)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                        )
                    }
                }
                else -> {
                    return
                }
            }
        } else {
            when (command) {
                SocketMessage.COMMAND.GET_INFO -> {
                    val clientList = ClientListProto.ClientList.newBuilder()
                    HyliConnect.uuidMap.forEach {
                        clientList.addClients(
                            ClientListProto.Client.newBuilder()
                                .setUuid(it.value)
                                .setIp(it.key)
                                .setPort(HyliConnect.deviceInfoMap[it.value]?.port ?: 0)
                                .build()
                        )
                    }
                    val responseData = InfoProto.Info.newBuilder()
                        .setApiVersion(API_VERSION)
                        .setAppVersion(BuildConfig.VERSION_CODE)
                        .setAppVersionName(BuildConfig.VERSION_NAME)
                        .setPlatform(PreferencesDataStore.platformMap[PreferencesDataStore.platform.getBlocking()!!])
                        .setUuid(PreferencesDataStore.uuid.getBlocking()!!)
                        .setNickname(PreferencesDataStore.nickname.getBlocking()!!)
                        .setClientList(clientList.build())
                        .build()
                    responseBody.setData(responseData.toByteString())
                    SocketUtils.sendMessage(ip, responseBody)
                }
                SocketMessage.COMMAND.DISCONNECT -> {
                    SocketUtils.closeConnection(ip)
                }
                SocketMessage.COMMAND.GET_CLIENTS -> {
                    val clientList = ClientListProto.ClientList.newBuilder()
                    HyliConnect.uuidMap.forEach {
                        clientList.addClients(
                            ClientListProto.Client.newBuilder()
                                .setUuid(it.value)
                                .setIp(it.key)
                                .setPort(HyliConnect.deviceInfoMap[it.value]?.port ?: 0)
                                .build()
                        )
                    }
                    responseBody.setData(clientList.build().toByteString())
                    SocketUtils.sendMessage(ip, responseBody)
                }
                else -> {
                    return
                }
            }
        }
//        if (uuid in HyliConnect.uuidMap.values) {
//            when (command) {
//                COMMAND_APP_LIST -> {
//                    val appList = JSONObject()
//                    PackageUtils.appNameMap.forEach { (key, value) ->
//                        val appInfo = JSONObject()
//                        appInfo["name"] = value
//                        appInfo["width"] = PackageUtils.appWidthMap[key]
//                        appInfo["height"] = PackageUtils.appHeightMap[key]
//                        appInfo["icon"] = PackageUtils.appIconByteMap[key]
//                        appList[key] = value
//                    }
//                    responseData["app_list"] = appList
//                    responseJson["data"] = responseData
//                    SocketUtils.sendMessage(ip, responseJson)
//                }
//            }
//        }
    }

    private fun responseHandler(
        ip: String,
        messageBody: SocketMessage.Body,
        broadcastManager: LocalBroadcastManager? = null
    ) {
        val command = messageBody.cmd
        val data = messageBody.data
        val uuid = messageBody.uuid
        if (data.isEmpty || uuid.isNullOrEmpty()) return

        when (command) {
            SocketMessage.COMMAND.CONNECT -> {
                val dataProto = ConnectProto.ConnectResponse.parseFrom(data)
                val success = dataProto.success
                if (success) {
                    HyliConnect.uuidMap[ip] = uuid
                    val ip_address = ip.substring(1, ip.length).split(":")[0]
                    val port = ip.substring(1, ip.length).split(":").last().toInt()
                    val deviceInfo = DeviceInfo(
                        dataProto.info.apiVersion,
                        dataProto.info.appVersion,
                        dataProto.info.appVersionName,
                        dataProto.info.platform,
                        uuid,
                        dataProto.info.nickname,
                        mutableListOf(ip_address),
                        port
                    )
                    HyliConnect.deviceInfoMap[uuid] = deviceInfo
                    SocketUtils.sendHeartbeat(ip)
                } else {
                    SocketUtils.closeConnection(ip)
                }
            }
            else -> {
                return
            }
        }
    }

    private fun broadcastHandler(
        ip: String,
        messageBody: SocketMessage.Body,
        broadcastManager: LocalBroadcastManager? = null
    ) {
    }

    private fun forwardHandler(
        ip: String,
        messageBody: SocketMessage.Body,
        broadcastManager: LocalBroadcastManager? = null
    ) {
    }

    private fun heartbeatHandler(
        ip: String,
        messageBody: SocketMessage.Body,
        broadcastManager: LocalBroadcastManager? = null
    ) {
        HyliConnect.connectionMap[ip] = System.currentTimeMillis()
        Thread.sleep(3000)
        try {
            if (System.currentTimeMillis() - HyliConnect.connectionMap[ip]!! >= 3000 && HyliConnect.socketMap.containsKey(ip)) {
                SocketUtils.sendHeartbeat(ip)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
