package xyz.hyli.connect.socket

import android.util.Log
import com.alibaba.fastjson2.JSONObject
import xyz.hyli.connect.BuildConfig
import xyz.hyli.connect.socket.MessageHandler.messageHandler
import xyz.hyli.connect.ui.main.ConfigHelper
import java.io.IOException
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.Socket
import kotlin.concurrent.thread

object SocketClient {
    private val TAG = "SocketClient"
    fun start(ip: String) {
        val socket = Socket(ip, SERVER_PORT)

        Log.i(TAG, "Start client = $socket")
        thread {
            try {
                val inputStream = socket.getInputStream()
                val bufferedReader = inputStream.bufferedReader()
                val outputStream = socket.getOutputStream()
                val printWriter = PrintWriter(OutputStreamWriter(outputStream))

                var message: String? = null
                while ((message == bufferedReader.readLine()) != null) {
                    Log.i(TAG, "Receive message: $message")
                    messageHandler(ip, message?:"")
                }
            } catch (e: IOException) {
                Log.e(TAG, "Error: ${e.message}")
            } finally {
                socket.close()
            }
        }
    }
    fun getInfo(ip: String): JSONObject? {
        try {
            val socket = Socket(ip, SERVER_PORT)
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
            printWriter.println(messageJson.toJSONString())
            var message: String
            val t1 = System.currentTimeMillis()
            while (socket.isConnected) {
                message = bufferedReader.readLine()
                if ( message.isNullOrEmpty().not() ) {
                    Log.i(TAG, "Receive message: $message")
                    messageJson = JSONObject.parseObject(message)
                    if ( messageJson["message_type"] == "response" ) {
                        if ( messageJson["command"] == COMMAND_GET_INFO ) {
                            val data = messageJson.getJSONObject("data")
                            socket.close()
                            return data
                        }
                    }
                }

                if ( System.currentTimeMillis() - t1 > 10000 ) break
            }
            socket.close()
            Log.i(TAG, "Failed to get info: $ip")
            return null
        } catch (e: IOException) {
            return null
        }
    }
}