package xyz.hyli.connect.bean

import xyz.hyli.connect.proto.SocketMessage

data class MessageQueue(
    val messageBody: SocketMessage.Body.Builder,
    val dropTime: Long,
    val onMessageSend: (() -> Unit)
)
