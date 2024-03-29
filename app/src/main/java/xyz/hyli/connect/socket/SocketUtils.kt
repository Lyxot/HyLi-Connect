package xyz.hyli.connect.socket

import android.util.Log
import xyz.hyli.connect.BuildConfig
import xyz.hyli.connect.HyLiConnect
import xyz.hyli.connect.bean.DeviceInfo
import xyz.hyli.connect.bean.MessageQueue
import xyz.hyli.connect.bean.MessageReceiveListener
import xyz.hyli.connect.datastore.PreferencesDataStore
import xyz.hyli.connect.proto.ConnectProto
import xyz.hyli.connect.proto.InfoProto
import xyz.hyli.connect.proto.SocketMessage

object SocketUtils {
    fun closeConnection(ip: String) {
        HyLiConnect.socketMap[ip]?.close()
        HyLiConnect.deviceInfoMap.remove(HyLiConnect.uuidMap[ip] ?: "")
        HyLiConnect.uuidMap.remove(ip)
        HyLiConnect.socketMap.remove(ip)
        HyLiConnect.inputStreamMap.remove(ip)
        HyLiConnect.outputStreamMap.remove(ip)
        HyLiConnect.sendMessageQueueMap.remove(ip)
        HyLiConnect.receiveMessageListenerMap.remove(ip)
        HyLiConnect.connectionMap.remove(ip)
    }
    fun closeAllConnection() {
        HyLiConnect.socketMap.forEach {
            closeConnection(it.key)
        }
        HyLiConnect.socketMap.clear()
        HyLiConnect.deviceInfoMap.clear()
        HyLiConnect.uuidMap.clear()
        HyLiConnect.socketMap.clear()
        HyLiConnect.inputStreamMap.clear()
        HyLiConnect.outputStreamMap.clear()
        HyLiConnect.sendMessageQueueMap.clear()
        HyLiConnect.receiveMessageListenerMap.clear()
        HyLiConnect.connectionMap.clear()
    }
    fun acceptConnection(ip: String, deviceInfo: DeviceInfo) {
        HyLiConnect.uuidMap[ip] = deviceInfo.uuid
        HyLiConnect.deviceInfoMap[deviceInfo.uuid] = deviceInfo
        val messageData = ConnectProto.ConnectResponse.newBuilder()
            .setSuccess(true)
            .setInfo(
                InfoProto.Info.newBuilder()
                    .setApiVersion(API_VERSION)
                    .setAppVersion(BuildConfig.VERSION_CODE)
                    .setAppVersionName(BuildConfig.VERSION_NAME)
                    .setPlatform(PreferencesDataStore.platformMap[PreferencesDataStore.platform.getBlocking()!!])
                    .setUuid(PreferencesDataStore.uuid.getBlocking()!!)
                    .setNickname(PreferencesDataStore.nickname.getBlocking()!!)
                    .setServerPort(PreferencesDataStore.server_port.getBlocking()!!)
                    .build()
            )
            .build()
        val messageBody = SocketMessage.Body.newBuilder()
            .setType(SocketMessage.TYPE.RESPONSE)
            .setCmd(SocketMessage.COMMAND.CONNECT)
            .setUuid(PreferencesDataStore.uuid.getBlocking()!!)
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
            .setUuid(PreferencesDataStore.uuid.getBlocking()!!)
            .setSTATUS(SocketMessage.STATUS.FAILED)
            .setData(messageData.toByteString())
        sendMessage(
            ip,
            messageBody,
            onMessageSend = {
                disconnectRequest(ip)
                closeConnection(ip)
            }
        )
    }
    fun sendHeartbeat(ip: String) {
        val messageBody = SocketMessage.Body.newBuilder()
            .setType(SocketMessage.TYPE.HEARTBEAT)
        sendMessage(ip, messageBody)
    }
    fun sendRequest(ip: String, command: SocketMessage.COMMAND) {
        val messageBody = SocketMessage.Body.newBuilder()
            .setType(SocketMessage.TYPE.REQUEST)
            .setCmd(command)
            .setUuid(PreferencesDataStore.uuid.getBlocking()!!)
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
            .setPlatform(PreferencesDataStore.platformMap[PreferencesDataStore.platform.getBlocking()!!])
            .setUuid(PreferencesDataStore.uuid.getBlocking()!!)
            .setNickname(PreferencesDataStore.nickname.getBlocking()!!)
            .setServerPort(PreferencesDataStore.server_port.getBlocking()!!)
            .build()
        val messageBody = SocketMessage.Body.newBuilder()
            .setType(SocketMessage.TYPE.REQUEST)
            .setCmd(SocketMessage.COMMAND.CONNECT)
            .setUuid(PreferencesDataStore.uuid.getBlocking()!!)
            .setSTATUS(SocketMessage.STATUS.SUCCESS)
            .setData(messageData.toByteString())
        while (HyLiConnect.socketMap[IPAddress] == null && System.currentTimeMillis() - t < 4800) {
            Thread.sleep(20)
        }
        if (HyLiConnect.socketMap[IPAddress] == null) {
            Log.e("SocketUtils", "Connect timeout")
            return
        }
        sendMessage(IPAddress, messageBody)
    }
    fun disconnectRequest(ip: String) {
        val messageBody = SocketMessage.Body.newBuilder()
            .setType(SocketMessage.TYPE.REQUEST)
            .setCmd(SocketMessage.COMMAND.DISCONNECT)
            .setUuid(PreferencesDataStore.uuid.getBlocking()!!)
            .setSTATUS(SocketMessage.STATUS.SUCCESS)
        sendMessage(ip, messageBody, onMessageSend = { closeConnection(ip) })
    }
    fun sendMessage(
        ip: String,
        messageBody: SocketMessage.Body.Builder,
        dropTime: Long = 0,
        onMessageSend: (() -> Unit) = {
        }
    ) {
        HyLiConnect.sendMessageQueueMap[ip]?.put(
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
            message.writeDelimitedTo(HyLiConnect.outputStreamMap[ip])
            onMessageSend()
            Log.i("SocketUtils", "Send message: $ip $message")
        } catch (e: Exception) {
            Log.e("SocketUtils", "Send message error: $ip ${e.message}")
//            closeConnection(ip)
        }
    }
    fun registerReceiveMessageListener(
        ip: String,
        className: String,
        type: SocketMessage.TYPE,
        command: SocketMessage.COMMAND,
        unregisterAfterReceived: Boolean = false,
        onMessageReceive: (SocketMessage.Body) -> Unit
    ): MessageReceiveListener {
        if (HyLiConnect.receiveMessageListenerMap[ip] == null) {
            HyLiConnect.receiveMessageListenerMap[ip] = mutableSetOf()
        }
        MessageReceiveListener(
            className,
            type,
            command,
            onMessageReceive,
            unregisterAfterReceived
        ).let {
            HyLiConnect.receiveMessageListenerMap[ip]?.add(it)
            Log.i("SocketUtils", "Register receive message listener: $ip $className $type $command")
            return it
        }
    }

    fun unregisterReceiveMessageListener(
        ip: String,
        className: String,
        type: SocketMessage.TYPE,
        command: SocketMessage.COMMAND,
        unregisterAfterReceived: Boolean = false,
        onMessageReceive: (SocketMessage.Body) -> Unit
    ) {
        HyLiConnect.receiveMessageListenerMap[ip]?.remove(
            MessageReceiveListener(
                className,
                type,
                command,
                onMessageReceive,
                unregisterAfterReceived
            )
        )
        Log.i("SocketUtils", "Unregister receive message listener: $ip $className $type $command")
    }

    fun unregisterReceiveMessageListener(ip: String, listener: MessageReceiveListener) {
        HyLiConnect.receiveMessageListenerMap[ip]?.remove(listener)
        Log.i("SocketUtils", "Unregister receive message listener: $ip ${listener.className} ${listener.type} ${listener.command}")
    }

    fun unregisterReceiveMessageListener(ip: String, className: String) {
        HyLiConnect.receiveMessageListenerMap[ip]?.filter { it.className == className }?.forEach {
            unregisterReceiveMessageListener(ip, it)
        }
    }
}
