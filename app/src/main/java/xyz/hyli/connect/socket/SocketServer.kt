package xyz.hyli.connect.socket

import android.util.Log
import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import xyz.hyli.connect.BuildConfig.VERSION_CODE
import xyz.hyli.connect.BuildConfig.VERSION_NAME
import xyz.hyli.connect.ui.main.ConfigHelper
import xyz.hyli.connect.utils.PackageUtils
import xyz.hyli.connect.socket.MessageHandler
import xyz.hyli.connect.socket.MessageHandler.messageHandler
import java.io.IOException
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

object SocketServer {
    private val TAG = "SocketServer"
    private var serverSocket: ServerSocket? = null

    fun start() {
        if ( serverSocket == null ) {
            thread {
                serverSocket = ServerSocket(SERVER_PORT)
                Log.i(TAG, "Start server: $serverSocket")
                while (true) {
                    val socket = serverSocket!!.accept()
                    val IPAddress = socket.remoteSocketAddress.toString()
                    Log.i(TAG, "Accept connection: $IPAddress")
                    SocketConfig.clientMap[IPAddress] = socket
                    thread {
                        try {
                            val inputStream = socket.getInputStream()
                            val bufferedReader = inputStream.bufferedReader()
                            val outputStream = socket.getOutputStream()

                            SocketConfig.inputStreamMap[IPAddress] = inputStream
                            SocketConfig.outputStreamMap[IPAddress] = outputStream

                            var message: String? = null
                            while (socket.isConnected) {
                                message = bufferedReader.readLine()
                                if ( message.isNullOrEmpty().not() ) {
                                    Log.i(TAG, "Receive message: $message")
                                    messageHandler(IPAddress, message?:"")
                                }
                            }
                        } catch (e: IOException) {
                            Log.e(TAG, "Error: ${e.message}")
                        } finally {
                            socket.close()
                            Log.i(TAG, "Close connection: $IPAddress")
                        }

                    }
                }
            }
        }
    }
    fun stop() {
        serverSocket?.close()
    }
}