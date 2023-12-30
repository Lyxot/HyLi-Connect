package xyz.hyli.connect.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.alibaba.fastjson2.JSONObject
import xyz.hyli.connect.BuildConfig
import xyz.hyli.connect.R
import xyz.hyli.connect.socket.API_VERSION
import xyz.hyli.connect.socket.MessageHandler
import xyz.hyli.connect.socket.PLATFORM
import xyz.hyli.connect.socket.SERVER_PORT
import xyz.hyli.connect.socket.SERVICE_TYPE
import xyz.hyli.connect.socket.SocketConfig
import xyz.hyli.connect.socket.utils.SocketUtils
import xyz.hyli.connect.ui.ConfigHelper
import xyz.hyli.connect.ui.dialog.RequestConnectionActivity
import xyz.hyli.connect.ui.test.TestActivity
import java.io.IOException
import java.net.ServerSocket
import kotlin.concurrent.thread

class SocketService : Service() {
    private var serverPort: Int = SERVER_PORT
    private var serverSocket: ServerSocket? = null
    private lateinit var mNsdManager: NsdManager
    private lateinit var NsdRegistrationListener: NsdManager.RegistrationListener
    private var mNsdServiceName: String? = null
    private var nsdRunning = false
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
                val messageData = intent.getStringExtra("data")
                if ( command.isNullOrEmpty().not() && ip.isNullOrEmpty().not() && nickname.isNullOrEmpty().not() && uuid.isNullOrEmpty().not() && messageData.isNullOrEmpty().not() ) {
                    if ( command == "connect" ) {
                        if ( SocketConfig.uuidMap.containsKey(ip).not() ) {
                            context.startActivity(Intent(context, RequestConnectionActivity::class.java)
                                .apply {
                                    putExtra("ip", ip)
                                    putExtra("data", messageData)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                            )
                        }
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
        val sharedPreferences = getSharedPreferences("config", Context.MODE_PRIVATE)
        ConfigHelper().initConfig(sharedPreferences, sharedPreferences.edit())
        serverPort = ConfigHelper.SERVER_PORT
        startServer(serverPort)
        registerNsdService(sharedPreferences)
        checkConnection()
        val filter = IntentFilter()
        filter.addAction("xyz.hyli.connect.service.SocketService.action.SOCKET_CLIENT")
        filter.addAction("xyz.hyli.connect.service.SocketService.action.CONNECT_REQUEST")
        filter.addAction("xyz.hyli.connect.service.SocketService.action.SOCKET_SERVER")
        localBroadcastManager = LocalBroadcastManager.getInstance(this)
        localBroadcastManager.registerReceiver(broadcastReceiver, filter)
    }
    override fun onBind(intent: Intent): IBinder {
        val sharedPreferences = getSharedPreferences("config", Context.MODE_PRIVATE)
        ConfigHelper().initConfig(sharedPreferences, sharedPreferences.edit())
        serverPort = ConfigHelper.SERVER_PORT
        startServer(serverPort)
        registerNsdService(sharedPreferences)
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
        unregisterNsdService()
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
                            // Authorization timeout
                            thread {
                                Thread.sleep(30000)
                                if ( SocketConfig.uuidMap.containsKey(IPAddress).not() ) {
                                    Log.i(TAG, "Authorization timeout: $IPAddress")
                                    SocketUtils.closeConnection(IPAddress)
                                }
                            }

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
    private fun registerNsdService(sharedPreferences: SharedPreferences) {
        if ( nsdRunning ) {
            return
        }

        val uuid = ConfigHelper.uuid
        val nickname = ConfigHelper.NICKNAME
        val mNsdServiceInfo = NsdServiceInfo().apply {
            serviceName = "HyliConnect@$uuid"
            serviceType = SERVICE_TYPE
            port = serverPort
        }
        mNsdServiceInfo.setAttribute("uuid", uuid)
        mNsdServiceInfo.setAttribute("nickname", nickname)
        mNsdServiceInfo.setAttribute("api", API_VERSION.toString())
        mNsdServiceInfo.setAttribute("app", BuildConfig.VERSION_CODE.toString())
        mNsdServiceInfo.setAttribute("app_name", BuildConfig.VERSION_NAME)
        mNsdServiceInfo.setAttribute("platform", PLATFORM)
        mNsdManager = getSystemService(Context.NSD_SERVICE) as NsdManager
        NsdRegistrationListener = object : NsdManager.RegistrationListener {
            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                // Registration failed! Put debugging code here to determine why.
            }

            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                // Unregistration failed. Put debugging code here to determine why.
            }

            override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
                // Save the service name. Android may have changed it in order to
                // resolve a conflict, so update the name you initially requested
                // with the name Android actually used.
                mNsdServiceName = serviceInfo.serviceName
                nsdRunning = true
                Log.i("SocketService", "Register service: $mNsdServiceName")
            }

            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
                // Service has been unregistered. This only happens when you call
                // NsdManager.unregisterService() and pass in this listener.
                nsdRunning = false
                Log.i("SocketService", "Unregister service: $mNsdServiceName")
            }
        }
        mNsdManager.registerService(mNsdServiceInfo, NsdManager.PROTOCOL_DNS_SD, NsdRegistrationListener)
    }
    private fun unregisterNsdService() {
        if ( nsdRunning.not() ) {
            return
        }
        mNsdManager.unregisterService(NsdRegistrationListener)
    }
}