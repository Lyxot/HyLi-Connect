package xyz.hyli.connect.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import xyz.hyli.connect.R
import xyz.hyli.connect.socket.MessageHandler
import xyz.hyli.connect.socket.SERVER_PORT
import xyz.hyli.connect.socket.SocketConfig
import xyz.hyli.connect.socket.utils.SocketUtils
import xyz.hyli.connect.ui.test.TestActivity
import java.io.IOException
import java.net.ServerSocket
import kotlin.concurrent.thread

class SocketService : Service() {
    private var serverSocket: ServerSocket? = null
    private val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: android.content.Context, intent: Intent) {
            val action = intent.action
            if ( action == "xyz.hyli.connect.service.SocketService.action.SOCKET_CLIENT" ) {
                val command = intent.getStringExtra("command")
                val ip = intent.getStringExtra("ip")
                val port = intent.getIntExtra("port", 0)
                if ( command.isNullOrEmpty().not() && ip.isNullOrEmpty().not() && port != 0 ) {
                    if ( command == "stop" ) {
                        SocketUtils.closeConnection("/$ip:$port")
                    } else if ( command == "start" ) {
                        if (ip != null) {
                            SocketUtils.connect(ip, port)
                        }
                    }
                }
            } else if ( action == "xyz.hyli.connect.service.SocketService.action.CONNECT_REQUEST" ) {
                val command = intent.getStringExtra("command")
                val ip = intent.getStringExtra("ip")
                val nickname = intent.getStringExtra("nickname")
                val uuid = intent.getStringExtra("uuid")
                if ( command.isNullOrEmpty().not() && ip.isNullOrEmpty().not() && nickname.isNullOrEmpty().not() && uuid.isNullOrEmpty().not() ) {
                    if ( command == "connect" ) {
                        createDialog(ip!!, nickname!!, uuid!!)
                    }
                }
            } else if ( action == "xyz.hyli.connect.service.SocketService.action.SOCKET_SERVER" ) {
                val command = intent.getStringExtra("command")
                if ( command.isNullOrEmpty().not() ) {
                    if ( command == "start" ) {
                        startForegroundService(Intent(this@SocketService, SocketService::class.java))
                    } else if ( command == "stop" ) {
                        stop()
                    }
                }
            }
        }
    }
    private lateinit var localBroadcastManager: LocalBroadcastManager
    private val checkConnectionThread = thread {
        while (true) {
            val map = SocketConfig.connectionMap
            map.forEach { (ip, time) ->
                if (System.currentTimeMillis() - time > 12000) {
                    SocketUtils.closeConnection(ip)
                    Log.i("SocketService", "Close connection: $ip (timeout)")
                } else if (System.currentTimeMillis() - time > 6000) {
                    SocketUtils.sendHeartbeat(ip)
                }
            }
            Thread.sleep(3000)
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate() {
        super.onCreate()
        setForeground()
        startServer()
        checkConnection()
        val filter = IntentFilter()
        filter.addAction("xyz.hyli.connect.service.SocketService.action.SOCKET_CLIENT")
        filter.addAction("xyz.hyli.connect.service.SocketService.action.CONNECT_REQUEST")
        filter.addAction("xyz.hyli.connect.service.SocketService.action.SOCKET_SERVER")
        localBroadcastManager = LocalBroadcastManager.getInstance(this)
        localBroadcastManager.registerReceiver(broadcastReceiver, filter)
    }
    override fun onBind(intent: Intent): IBinder {
        startServer()
        checkConnection()
        return Binder()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        checkConnectionThread.stop()
        stop()
        stopForeground(true)
        localBroadcastManager.sendBroadcast(Intent("xyz.hyli.connect.service.SocketService.action.SOCKET_SERVER").apply {
            putExtra("command", "start")
        })
        unregisterReceiver(broadcastReceiver)
    }

    private fun startServer(port: Int = SERVER_PORT) {
        val TAG = "SocketServer(SERVICE)"
        if ( serverSocket == null ) {
            thread {
                serverSocket = ServerSocket(port)
                Log.i(TAG, "Start server: $serverSocket")
                while (true) {
                    val socket = serverSocket!!.accept()
                    val IPAddress = socket.remoteSocketAddress.toString()
                    Log.i(TAG, "Accept connection: $IPAddress")
                    SocketConfig.socketMap[IPAddress] = socket
                    SocketConfig.connectionMap[IPAddress] = System.currentTimeMillis()
                    thread {
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
                                    MessageHandler.messageHandler(IPAddress, message, localBroadcastManager)
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
            }
        }
    }
    private fun stop() {
        SocketUtils.closeAllConnection()
    }
    private fun checkConnection() {
        if ( checkConnectionThread.isAlive.not() ) {
            checkConnectionThread.start()
        }
    }
    private fun setForeground() {
        val CHANNEL_ID = 1
        val channelName = getString(R.string.channel_1_name)
        val importance = NotificationManager.IMPORTANCE_LOW
        val channel = NotificationChannel(CHANNEL_ID.toString(), channelName, importance)
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        val pendingIntent: PendingIntent =
            Intent(this, TestActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_ONE_SHOT)
            }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID.toString())
            .setContentTitle(getText(R.string.channel_1_notification_title))
            .setContentText(getText(R.string.channel_1_notification_message))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setTicker(getText(R.string.channel_1_notification_title))
            .build()
        startForeground(1, notification)
    }
    private fun createDialog(ip: String, nickname: String, uuid: String) {
        val dialog = MaterialAlertDialogBuilder(this, R.style.Theme_HyLiConnect)
            .setTitle(getString(R.string.dialog_connect_request_title))
            .setMessage("$nickname($ip) ${getString(R.string.dialog_connect_request_message)}")
            .setPositiveButton("Accept") { dialog, which ->
                SocketUtils.acceptConnection(ip, nickname, uuid)
                dialog.dismiss()
            }
            .setNegativeButton("Reject") { dialog, which ->
                SocketUtils.rejectConnection(ip)
                dialog.dismiss()
            }
            .setCancelable(true)
            .create()
        dialog.window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        dialog.show()
    }
}