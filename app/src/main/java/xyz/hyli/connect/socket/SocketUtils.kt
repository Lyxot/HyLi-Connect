package xyz.hyli.connect.socket

import android.util.Log
import xyz.hyli.connect.BuildConfig
import xyz.hyli.connect.HyliConnect
import xyz.hyli.connect.bean.DeviceInfo
import xyz.hyli.connect.bean.MessageQueue
import xyz.hyli.connect.datastore.PreferencesDataStore
import xyz.hyli.connect.proto.ConnectProto
import xyz.hyli.connect.proto.InfoProto
import xyz.hyli.connect.proto.SocketMessage
import kotlin.concurrent.thread

object SocketUtils {
    fun closeConnection(ip: String) {
        HyliConnect.socketMap[ip]?.close()
        HyliConnect.deviceInfoMap.remove(HyliConnect.uuidMap[ip] ?: "")
        HyliConnect.uuidMap.remove(ip)
        HyliConnect.socketMap.remove(ip)
        HyliConnect.inputStreamMap.remove(ip)
        HyliConnect.outputStreamMap.remove(ip)
        HyliConnect.blockingQueueMap.remove(ip)
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
    fun acceptConnection(ip: String, deviceInfo: DeviceInfo) {
        HyliConnect.uuidMap[ip] = deviceInfo.uuid
        HyliConnect.deviceInfoMap[deviceInfo.uuid] = deviceInfo
        val messageData = ConnectProto.ConnectResponse.newBuilder()
            .setSuccess(true)
            .setInfo(
                InfoProto.Info.newBuilder()
                    .setApiVersion(API_VERSION)
                    .setAppVersion(BuildConfig.VERSION_CODE)
                    .setAppVersionName(BuildConfig.VERSION_NAME)
                    .setPlatform(PreferencesDataStore.getConfigMap()["platform"].toString())
                    .setUuid(PreferencesDataStore.getConfigMap()["uuid"].toString())
                    .setNickname(PreferencesDataStore.getConfigMap()["nickname"].toString())
                    .build()
            )
            .build()
        val messageBody = SocketMessage.Body.newBuilder()
            .setType(SocketMessage.TYPE.RESPONSE)
            .setCmd(SocketMessage.COMMAND.CONNECT)
            .setUuid(PreferencesDataStore.getConfigMap()["uuid"].toString())
            .setSTATUS(SocketMessage.STATUS.SUCCESS)
            .setData(messageData.toByteString())
        sendMessage(ip, messageBody)
    }
    fun rejectConnection(ip: String) {
        val messageData = ConnectProto.ConnectResponse.newBuilder()
            .setSuccess(false)
            .build()
        val messageBody = SocketMessage.Body.newBuilder()
            .setType(SocketMessage.TYPE.RESPONSE)
            .setCmd(SocketMessage.COMMAND.CONNECT)
            .setUuid(PreferencesDataStore.getConfigMap()["uuid"].toString())
            .setSTATUS(SocketMessage.STATUS.FAILED)
            .setData(messageData.toByteString())
        sendMessage(ip, messageBody, onMessageSend = { closeConnection(ip) })
    }
    fun sendHeartbeat(ip: String) {
        val messageBody = SocketMessage.Body.newBuilder()
            .setType(SocketMessage.TYPE.HEARTBEAT)
        sendMessage(ip, messageBody)
    }
    fun getInfoRequest(ip: String) {
        val messageBody = SocketMessage.Body.newBuilder()
            .setType(SocketMessage.TYPE.REQUEST)
            .setCmd(SocketMessage.COMMAND.GET_INFO)
            .setUuid(PreferencesDataStore.getConfigMap()["uuid"].toString())
            .setSTATUS(SocketMessage.STATUS.SUCCESS)
        sendMessage(ip, messageBody)
    }
    fun connectRequest(ip: String, port: Int) {
        val t = System.currentTimeMillis()
        val IPAddress = "/$ip:$port"
        val messageData = ConnectProto.ConnectRequest.newBuilder()
            .setApiVersion(API_VERSION)
            .setAppVersion(BuildConfig.VERSION_CODE)
            .setAppVersionName(BuildConfig.VERSION_NAME)
            .setPlatform(PreferencesDataStore.getConfigMap()["platform"].toString())
            .setUuid(PreferencesDataStore.getConfigMap()["uuid"].toString())
            .setNickname(PreferencesDataStore.getConfigMap()["nickname"].toString())
            .build()
        val messageBody = SocketMessage.Body.newBuilder()
            .setType(SocketMessage.TYPE.REQUEST)
            .setCmd(SocketMessage.COMMAND.CONNECT)
            .setUuid(PreferencesDataStore.getConfigMap()["uuid"].toString())
            .setSTATUS(SocketMessage.STATUS.SUCCESS)
            .setData(messageData.toByteString())
        while ( HyliConnect.socketMap[IPAddress] == null && System.currentTimeMillis() - t < 4800 ) {
            Thread.sleep(20)
        }
        if ( HyliConnect.socketMap[IPAddress] == null ) {
            Log.e("SocketUtils", "Connect timeout")
            return
        }
        sendMessage(IPAddress, messageBody)
    }
    fun disconnectRequest(ip: String) {
        val messageBody = SocketMessage.Body.newBuilder()
            .setType(SocketMessage.TYPE.REQUEST)
            .setCmd(SocketMessage.COMMAND.DISCONNECT)
            .setUuid(PreferencesDataStore.getConfigMap()["uuid"].toString())
            .setSTATUS(SocketMessage.STATUS.SUCCESS)
        sendMessage(ip, messageBody, onMessageSend = { closeConnection(ip) })
    }
    fun getClientsRequest(ip: String) {
        val messageBody = SocketMessage.Body.newBuilder()
            .setType(SocketMessage.TYPE.REQUEST)
            .setCmd(SocketMessage.COMMAND.GET_CLIENTS)
            .setUuid(PreferencesDataStore.getConfigMap()["uuid"].toString())
            .setSTATUS(SocketMessage.STATUS.SUCCESS)
        sendMessage(ip, messageBody)
    }
    fun sendMessage(ip: String, messageBody: SocketMessage.Body.Builder, dropTime: Long = 0, onMessageSend: (() -> Unit) = { }) {
        HyliConnect.blockingQueueMap[ip]?.put(
            MessageQueue(
                messageBody,
                dropTime,
                onMessageSend
            )
        )
    }
    fun sendQueueMessage(ip: String, messageBody: SocketMessage.Body.Builder, onMessageSend: (() -> Unit) = { }) {
        var message: SocketMessage.Message
        messageBody.build().let {
            message = SocketMessage.Message.newBuilder()
                .setHeader(
                    SocketMessage.Header.newBuilder()
//                    .setLength(it.serializedSize)
                        .build()
                )
                .setBody(it)
                .build()
        }
        try {
            message.writeDelimitedTo(HyliConnect.outputStreamMap[ip])
            onMessageSend()
            Log.i("SocketUtils", "Send message: $ip $message")
        } catch (e: Exception) {
            Log.e("SocketUtils", "Send message error: $ip ${e.message}")
//            closeConnection(ip)
        }
    }
}