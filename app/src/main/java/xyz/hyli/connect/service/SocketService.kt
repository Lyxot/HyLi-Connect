package xyz.hyli.connect.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import xyz.hyli.connect.BuildConfig
import xyz.hyli.connect.HyliConnect
import xyz.hyli.connect.R
import xyz.hyli.connect.bean.ServiceState
import xyz.hyli.connect.datastore.PreferencesDataStore
import xyz.hyli.connect.proto.SocketMessage
import xyz.hyli.connect.socket.API_VERSION
import xyz.hyli.connect.socket.MessageHandler
import xyz.hyli.connect.socket.SocketUtils
import xyz.hyli.connect.ui.test.TestActivity
import xyz.hyli.connect.utils.NetworkUtils
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.LinkedBlockingQueue

class SocketService : Service() {
    private var serverPort: Int = 15372
    private var serverSocket: ServerSocket? = null
    private lateinit var mNsdManager: NsdManager
    private lateinit var NsdRegistrationListener: NsdManager.RegistrationListener
    private var mNsdServiceName: String? = null
    private var nsdRunning = false
    private val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action == "xyz.hyli.connect.service.SocketService.action.SOCKET_CLIENT") {
                val command = intent.getStringExtra("command")
                val ip = intent.getStringExtra("ip")
                val port = intent.getIntExtra("port", 0)
                if (command.isNullOrEmpty().not() && ip.isNullOrEmpty().not() && port != 0) {
                    if (command == "stop") {
                        SocketUtils.closeConnection("/$ip:$port")
                    } else if (command == "start") {
                        if (ip != null) {
                            startClient(ip, port)
                            SocketUtils.connectRequest(ip, port)
                        }
                    }
                }
            } else if (action == "xyz.hyli.connect.service.SocketService.action.SERVICE_CONTROLLER") {
                val command = intent.getStringExtra("command")
                if (command.isNullOrEmpty().not()) {
                    if (command == "start_service") {
                        startForegroundService(Intent(this@SocketService, SocketService::class.java))
                    } else if (command == "stop_service") {
                        stopSelf()
                    } else if (command == "reboot_service") {
                        destroy(reboot = true)
                        init()
                    } else if (command == "reboot_nsd_service") {
                        restartNsdService()
                    }
                }
            }
        }
    }
    private lateinit var localBroadcastManager: LocalBroadcastManager
    private lateinit var checkConnectionJob: Job

    override fun onCreate() {
        super.onCreate()
        setForeground()
        init()
        val filter = IntentFilter()
        filter.addAction("xyz.hyli.connect.service.SocketService.action.SOCKET_CLIENT")
        filter.addAction("xyz.hyli.connect.service.SocketService.action.SERVICE_CONTROLLER")
        localBroadcastManager = LocalBroadcastManager.getInstance(this)
        localBroadcastManager.registerReceiver(broadcastReceiver, filter)
    }
    override fun onBind(intent: Intent): IBinder {
        init()
        return Binder()
    }
    private fun init() {
        serverPort = PreferencesDataStore.server_port.getBlocking()!!
        startServer(serverPort)
        registerNsdService()
        checkConnection()
        HyliConnect.serviceStateMap["SocketService"] = ServiceState("running", getString(R.string.state_service_running, getString(R.string.service_socket_service)))
    }

    override fun onUnbind(intent: Intent?): Boolean {
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            localBroadcastManager.sendBroadcast(
                Intent("xyz.hyli.connect.service.SocketService.action.SERVICE_CONTROLLER").apply {
                    putExtra("command", "start")
                }
            )
            destroy()
            stopSelf()
            unregisterReceiver(broadcastReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    private fun destroy(reboot: Boolean = false) {
        if (reboot) HyliConnect.serviceStateMap["SocketService"] = ServiceState("stopped", getString(R.string.state_service_stopped, getString(R.string.service_socket_service)))
        checkConnectionJob.cancel()
        stopSocket()
        unregisterNsdService()
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun startServer(port: Int) {
        val TAG = "SocketServer"
        if (serverSocket == null) {
            HyliConnect.serviceStateMap["SocketServer"] = ServiceState("stopped", getString(R.string.state_service_stopped, getString(R.string.service_socket_server)))
            serverPort = port
            if (NetworkUtils.isPortInUse(port)) {
                Log.i(TAG, "Port $port is in use")
                serverPort = NetworkUtils.getAvailablePort()
                HyliConnect.serviceStateMap["SocketServer"] = ServiceState("error", getString(R.string.state_service_socket_server_port_in_use, port.toString(), serverPort.toString()))
            } else {
                HyliConnect.serviceStateMap["SocketServer"] = ServiceState("running", getString(R.string.state_service_running, getString(R.string.service_socket_server)))
            }
            GlobalScope.launch(context = Dispatchers.IO) {
                serverSocket = ServerSocket(serverPort)
                Log.i(TAG, "Start server: $serverSocket")
                try {
                    while (serverSocket != null) {
                        val socket = serverSocket!!.accept()
                        val IPAddress = socket.remoteSocketAddress.toString()
                        Log.i(TAG, "Accept connection: $IPAddress")
                        HyliConnect.socketMap[IPAddress] = socket
                        HyliConnect.connectionMap[IPAddress] = System.currentTimeMillis()
                        GlobalScope.launch(context = Dispatchers.IO) {
                            try {
                                val inputStream = socket.getInputStream()
                                val outputStream = socket.getOutputStream()

                                HyliConnect.inputStreamMap[IPAddress] = inputStream
                                HyliConnect.outputStreamMap[IPAddress] = outputStream
                                HyliConnect.sendMessageQueueMap[IPAddress] = LinkedBlockingQueue()
                                GlobalScope.launch(context = Dispatchers.IO) {
                                    while (HyliConnect.socketMap.containsKey(IPAddress)) {
                                        try {
                                            val mq = HyliConnect.sendMessageQueueMap[IPAddress]?.take()!!
                                            if (mq.dropTime != 0L && mq.dropTime < System.currentTimeMillis()) {
                                                Log.i(TAG, "Drop message: $IPAddress ${mq.messageBody}")
                                            } else {
                                                SocketUtils.sendQueueMessage(
                                                    IPAddress,
                                                    mq.messageBody,
                                                    mq.onMessageSend
                                                )
                                            }
                                        } catch (e: Exception) {
                                            if (!HyliConnect.socketMap.containsKey(IPAddress)) {
                                                break
                                            }
                                        }
                                    }
                                }

                                SocketUtils.sendHeartbeat(IPAddress)
                                // Authorization timeout
                                GlobalScope.launch(context = Dispatchers.IO) {
                                    Thread.sleep(30000)
                                    if (HyliConnect.uuidMap.containsKey(IPAddress).not() && HyliConnect.socketMap.containsKey(IPAddress)) {
                                        Log.i(TAG, "Authorization timeout: $IPAddress")
                                        SocketUtils.closeConnection(IPAddress)
                                    }
                                }

                                while (HyliConnect.socketMap.containsKey(IPAddress)) {
                                    try {
                                        val message = SocketMessage.Message.parseDelimitedFrom(inputStream)
                                        if (message != null) {
                                            GlobalScope.launch(context = Dispatchers.IO) {
                                                Log.i(TAG, "Receive message: $IPAddress $message")
                                                MessageHandler.messageHandler(IPAddress, message, localBroadcastManager)
                                            }
                                        }
                                    } catch (e: Exception) {
                                        if (!HyliConnect.socketMap.containsKey(IPAddress)) {
                                            break
                                        }
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
                } catch (e: IOException) {
                    Log.e(TAG, "Error: ${e.message}")
                } finally {
                    stopSocket()
                    Log.i(TAG, "Close server: $serverSocket")
                }
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun startClient(ip: String, port: Int) {
        val TAG = "SocketClient"
        if (HyliConnect.socketMap["/$ip:$port"] != null) return
        GlobalScope.launch(context = Dispatchers.IO) {
            val socket = Socket(ip, port)
            val IPAddress = socket.remoteSocketAddress.toString()
            Log.i(TAG, "Start client = $socket")
            Log.i(TAG, "Connect to: $IPAddress")
            HyliConnect.socketMap[IPAddress] = socket
            HyliConnect.connectionMap[IPAddress] = System.currentTimeMillis()
            try {
                val inputStream = socket.getInputStream()
                val outputStream = socket.getOutputStream()

                HyliConnect.inputStreamMap[IPAddress] = inputStream
                HyliConnect.outputStreamMap[IPAddress] = outputStream
                HyliConnect.sendMessageQueueMap[IPAddress] = LinkedBlockingQueue()
                GlobalScope.launch(context = Dispatchers.IO) {
                    while (HyliConnect.socketMap.containsKey(IPAddress)) {
                        try {
                            val mq = HyliConnect.sendMessageQueueMap[IPAddress]?.take()!!
                            if (mq.dropTime != 0L && mq.dropTime < System.currentTimeMillis()) {
                                Log.i(TAG, "Drop message: $IPAddress ${mq.messageBody}")
                            } else {
                                SocketUtils.sendQueueMessage(IPAddress, mq.messageBody, mq.onMessageSend)
                            }
                        } catch (e: Exception) {
                            if (!HyliConnect.socketMap.containsKey(IPAddress)) {
                                break
                            }
                        }
                    }
                }

                SocketUtils.sendHeartbeat(IPAddress)

                while (HyliConnect.socketMap.containsKey(IPAddress)) {
                    try {
                        val message = SocketMessage.Message.parseDelimitedFrom(inputStream)
                        if (message != null) {
                            GlobalScope.launch(context = Dispatchers.IO) {
                                Log.i(TAG, "Receive message: $IPAddress $message")
                                MessageHandler.messageHandler(IPAddress, message, localBroadcastManager)
                            }
                        }
                    } catch (e: Exception) {
                        if (!HyliConnect.socketMap.containsKey(IPAddress)) {
                            break
                        }
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
    private fun stopSocket() {
        try {
            HyliConnect.serviceStateMap["SocketServer"] = ServiceState("stopped", getString(R.string.state_service_stopped, getString(R.string.service_socket_server)))
            SocketUtils.closeAllConnection()
            serverSocket!!.close()
            serverSocket = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun checkConnection() {
        if (::checkConnectionJob.isInitialized.not()) {
            checkConnectionJob = GlobalScope.launch(context = Dispatchers.Main) {
                while (true) {
                    HyliConnect.connectionMap.forEach { (ip, time) ->
                        if (System.currentTimeMillis() - time > 12000) {
                            SocketUtils.closeConnection(ip)
                            Log.i("SocketService", "Close connection: $ip (timeout)")
                        } else if (System.currentTimeMillis() - time > 6000) {
                            SocketUtils.sendHeartbeat(ip)
                        }
                    }
                    delay(3000)
                }
            }
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
                PendingIntent.getActivity(
                    this,
                    0,
                    notificationIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_ONE_SHOT
                )
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
    private fun registerNsdService() {
        if (nsdRunning) {
            return
        }
        HyliConnect.serviceStateMap["NsdService"] = ServiceState("stopped", getString(R.string.state_service_stopped, getString(R.string.service_nsd_service)))

        val uuid = PreferencesDataStore.uuid.getBlocking()!!
        val nickname = PreferencesDataStore.nickname.getBlocking()!!
        val mNsdServiceInfo = NsdServiceInfo().apply {
            serviceName = "HyliConnect@$uuid"
            serviceType = "_hyli-connect._tcp."
            port = serverPort
        }
        mNsdServiceInfo.setAttribute("uuid", uuid)
        mNsdServiceInfo.setAttribute("nickname", nickname)
        mNsdServiceInfo.setAttribute("api", API_VERSION.toString())
        mNsdServiceInfo.setAttribute("app", BuildConfig.VERSION_CODE.toString())
        mNsdServiceInfo.setAttribute("app_name", BuildConfig.VERSION_NAME)
        mNsdServiceInfo.setAttribute("platform", PreferencesDataStore.platformMap[PreferencesDataStore.platform.getBlocking()!!])
        mNsdServiceInfo.setAttribute("ip_addr", NetworkUtils.getLocalIPInfo(this)["wlan0"] ?: "0.0.0.0")
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
        HyliConnect.serviceStateMap["NsdService"] = ServiceState("running", getString(R.string.state_service_running, getString(R.string.service_nsd_service)))
    }
    private fun unregisterNsdService() {
        if (nsdRunning.not()) {
            return
        }
        mNsdManager.unregisterService(NsdRegistrationListener)
        HyliConnect.serviceStateMap["NsdService"] = ServiceState("stopped", getString(R.string.state_service_stopped, getString(R.string.service_nsd_service)))
    }
    private fun restartNsdService() {
        if (nsdRunning) {
            mNsdManager.unregisterService(NsdRegistrationListener)
            nsdRunning = false
        }
        registerNsdService()
    }
}
