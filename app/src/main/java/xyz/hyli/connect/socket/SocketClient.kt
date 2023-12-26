package xyz.hyli.connect.socket

import android.util.Log
import com.alibaba.fastjson2.JSONObject
import xyz.hyli.connect.BuildConfig
import xyz.hyli.connect.socket.MessageHandler.messageHandler
import xyz.hyli.connect.socket.utils.SocketUtils
import xyz.hyli.connect.ui.ConfigHelper
import java.io.IOException
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.Socket
import kotlin.concurrent.thread

object SocketClient {
    private val TAG = "SocketClient"
    fun start(ip: String, port: Int = SERVER_PORT) {
        if ( SocketConfig.socketMap["/$ip:$port"] != null ) return
        thread {
            val socket = Socket(ip, port)
            val IPAddress = socket.remoteSocketAddress.toString()
            Log.i(TAG, "Start client = $socket")
            Log.i(TAG, "Connect to: $IPAddress")
            SocketConfig.socketMap[IPAddress] = socket
            SocketConfig.connectionMap[IPAddress] = System.currentTimeMillis()
            try {
                val inputStream = socket.getInputStream()
                val bufferedReader = inputStream.bufferedReader()
                val outputStream = socket.getOutputStream()

                SocketConfig.inputStreamMap[IPAddress] = inputStream
                SocketConfig.outputStreamMap[IPAddress] = outputStream
                socket.keepAlive = true
                SocketUtils.sendHeartbeat(IPAddress)

                var message: String?
                while (socket.isConnected) {
                    message = bufferedReader.readLine()
                    if ( message.isNullOrEmpty().not() ) {
                        Log.i(TAG, "Receive message: $IPAddress $message")
                        messageHandler(IPAddress, message)
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "Error: ${e.message}")
            } finally {
                SocketUtils.closeConnection(IPAddress)
                Log.i(TAG, "Close connection: $IPAddress")
            }
        }
    }
    fun getInfo(ip: String, port: Int = SERVER_PORT): JSONObject? {
        try {
            val socket = Socket(ip, port)
            Log.i(TAG, "Start client: $socket")
            val inputStream = socket.getInputStream()
            val bufferedReader = inputStream.bufferedReader()
            val printWriter = PrintWriter(OutputStreamWriter(socket.getOutputStream()), true)
            var messageJson = JSONObject()
            val messageData = JSONObject()
            messageJson["message_type"] = "request"
            messageJson["command"] = COMMAND_GET_INFO
            messageData["api_version"] = API_VERSION
            messageData["app_version"] = BuildConfig.VERSION_CODE
            messageData["app_version_name"] = BuildConfig.VERSION_NAME
            messageData["platform"] = PLATFORM
            messageData["uuid"] = ConfigHelper.uuid
            messageData["nickname"] = ConfigHelper.NICKNAME
            messageJson["data"] = messageData
            messageJson["uuid"] = ConfigHelper.uuid
            messageJson["status"] = "success"
            printWriter.println(messageJson.toJSONString())
            Log.i(TAG, "Send message: ${messageJson.toJSONString()}")
            var message: String?
            val t1 = System.currentTimeMillis()
            while (socket.isConnected) {
                message = bufferedReader.readLine()
                if ( message.isNullOrEmpty().not() ) {
                    Log.i(TAG, "Receive message: $message")
                    messageJson = JSONObject.parseObject(message)
                    if ( messageJson["message_type"] == "response" ) {
                        if ( messageJson["command"] == COMMAND_GET_INFO ) {
                            val data = messageJson.getJSONObject("data")
                            Log.i(TAG, "Get info: $data")
                            printWriter.close()
                            bufferedReader.close()
                            socket.close()
                            Log.i(TAG, "Close client: $socket")
                            return data
                        }
                    }
                }

                if ( System.currentTimeMillis() - t1 > 10000 ) break
            }
            printWriter.close()
            bufferedReader.close()
            socket.close()
            Log.i(TAG, "Close client: $socket")
            Log.i(TAG, "Failed to get info: $ip")
            return null
        } catch (e: IOException) {
            return null
        }
    }
}