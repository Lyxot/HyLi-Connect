package xyz.hyli.connect.bean

import xyz.hyli.connect.proto.SocketMessage

data class MessageReceiveListener(
    val className: String,
    val type: SocketMessage.TYPE,
    val command: SocketMessage.COMMAND,
    val onMessageReceive: (SocketMessage.Body) -> Unit,
    val unregisterAfterReceived: Boolean = false
)
