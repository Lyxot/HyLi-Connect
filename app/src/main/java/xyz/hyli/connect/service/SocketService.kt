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
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import xyz.hyli.connect.BuildConfig
import xyz.hyli.connect.R
import xyz.hyli.connect.bean.ServiceState
import xyz.hyli.connect.datastore.PreferencesDataStore
import xyz.hyli.connect.socket.API_VERSION
import xyz.hyli.connect.socket.MessageHandler
import xyz.hyli.connect.socket.SERVICE_TYPE
import xyz.hyli.connect.socket.SocketData
import xyz.hyli.connect.socket.utils.SocketUtils
import xyz.hyli.connect.ui.dialog.RequestConnectionActivity
import xyz.hyli.connect.ui.state.HyliConnectState
import xyz.hyli.connect.ui.test.TestActivity
import xyz.hyli.connect.utils.NetworkUtils
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket

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
            if ( action == "xyz.hyli.connect.service.SocketService.action.SOCKET_CLIENT" ) {
                val command = intent.getStringExtra("command")
                val ip = intent.getStringExtra("ip")
                val port = intent.getIntExtra("port", 0)
                if ( command.isNullOrEmpty().not() && ip.isNullOrEmpty().not() && port != 0 ) {
                    if ( command == "stop" ) {
                        SocketUtils.closeConnection("/$ip:$port")
                    } else if ( command == "start" ) {
                        if (ip != null) {
                            startClient(ip, port)
                            SocketUtils.connect(ip, port)
                        }
                    }
                }
            } else if ( action == "xyz.hyli.connect.service.SocketService.action.CONNECT_REQUEST" ) {
                val command = intent.getStringExtra("command")
                val ip = intent.getStringExtra("ip")
                val nickname = intent.getStringExtra("nickname")
                val uuid = intent.getStringExtra("uuid")
                val api_version = intent.getIntExtra("api_version", 0)
                val app_version = intent.getIntExtra("app_version", 0)
                val app_version_name = intent.getStringExtra("app_version_name")
                val platform = intent.getStringExtra("platform")
                if ( command.isNullOrEmpty().not() && ip.isNullOrEmpty().not() && nickname.isNullOrEmpty().not() && uuid.isNullOrEmpty().not() && api_version != 0 && app_version != 0 && app_version_name.isNullOrEmpty().not() && platform.isNullOrEmpty().not() ) {
                    if ( command == "connect" ) {
                        if ( SocketData.uuidMap.containsKey(ip).not() ) {
                            context.startActivity(Intent(context, RequestConnectionActivity::class.java)
                                .apply {
                                    putExtra("ip", ip)
                                    putExtra("nickname", nickname)
                                    putExtra("uuid", uuid)
                                    putExtra("api_version", api_version)
                                    putExtra("app_version", app_version)
                                    putExtra("app_version_name", app_version_name)
                                    putExtra("platform", platform)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                            )
                        }
                    }
                }
            } else if ( action == "xyz.hyli.connect.service.SocketService.action.SERVICE_CONTROLLER" ) {
                val command = intent.getStringExtra("command")
                if ( command.isNullOrEmpty().not() ) {
                    if ( command == "start_service" ) {
                        startForegroundService(Intent(this@SocketService, SocketService::class.java))
                    } else if ( command == "stop_service" ) {
                        stopSocket()
                    } else if ( command == "reboot_nsd_service" ) {
                        restartNsdService()
                    }
                }
            }
        }
    }
    private lateinit var localBroadcastManager: LocalBroadcastManager
    private lateinit var checkConnectionJob: Job
    private lateinit var configMap: MutableMap<String, Any>

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate() {
        super.onCreate()
        setForeground()
        configMap = PreferencesDataStore.getConfigMap(true)
        serverPort = configMap["server_port"] as Int
        startServer(serverPort)
        registerNsdService()
        checkConnection()
        val filter = IntentFilter()
        filter.addAction("xyz.hyli.connect.service.SocketService.action.SOCKET_CLIENT")
        filter.addAction("xyz.hyli.connect.service.SocketService.action.CONNECT_REQUEST")
        filter.addAction("xyz.hyli.connect.service.SocketService.action.SERVICE_CONTROLLER")
        localBroadcastManager = LocalBroadcastManager.getInstance(this)
        localBroadcastManager.registerReceiver(broadcastReceiver, filter)
    }
    override fun onBind(intent: Intent): IBinder {
        configMap = PreferencesDataStore.getConfigMap(true)
        serverPort = configMap["server_port"] as Int
        startServer(serverPort)
        registerNsdService()
        checkConnection()
        return Binder()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            HyliConnectState.serviceStateMap["SocketService"] = ServiceState("stopped", getString(R.string.state_service_stopped, getString(R.string.service_socket_service)))
            localBroadcastManager.sendBroadcast(Intent("xyz.hyli.connect.service.SocketService.action.SERVICE_CONTROLLER").apply {
                putExtra("command", "start")
            })
            checkConnectionJob.cancel()
            stopSocket()
            unregisterNsdService()
            stopForeground(true)
            unregisterReceiver(broadcastReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun startServer(port: Int) {
        val TAG = "SocketServer"
        if ( serverSocket == null ) {
            HyliConnectState.serviceStateMap["SocketServer"] = ServiceState("stopped", getString(R.string.state_service_stopped, getString(R.string.service_socket_server)))
            serverPort = port
            if ( NetworkUtils.isPortInUse(port) ) {
                Log.i(TAG, "Port $port is in use")
                serverPort = NetworkUtils.getAvailablePort()
                HyliConnectState.serviceStateMap["SocketServer"] = ServiceState("error", getString(R.string.state_service_socket_server_port_in_use, port.toString(), serverPort.toString()))
            } else {
                HyliConnectState.serviceStateMap["SocketServer"] = ServiceState("running", getString(R.string.state_service_running, getString(R.string.service_socket_server)))
            }
            GlobalScope.launch(context = Dispatchers.IO) {
                serverSocket = ServerSocket(serverPort)
                Log.i(TAG, "Start server: $serverSocket")
                while (true) {
                    val socket = serverSocket!!.accept()
                    val IPAddress = socket.remoteSocketAddress.toString()
                    Log.i(TAG, "Accept connection: $IPAddress")
                    SocketData.socketMap[IPAddress] = socket
                    SocketData.connectionMap[IPAddress] = System.currentTimeMillis()
                    GlobalScope.launch(context = Dispatchers.IO) {
                        try {
                            val inputStream = socket.getInputStream()
                            val bufferedReader = inputStream.bufferedReader()
                            val outputStream = socket.getOutputStream()

                            SocketData.inputStreamMap[IPAddress] = inputStream
                            SocketData.outputStreamMap[IPAddress] = outputStream
                            socket.keepAlive = true
                            SocketUtils.sendHeartbeat(IPAddress)
                            // Authorization timeout
                            GlobalScope.launch(context = Dispatchers.IO) {
                                Thread.sleep(30000)
                                if ( SocketData.uuidMap.containsKey(IPAddress).not() ) {
                                    Log.i(TAG, "Authorization timeout: $IPAddress")
                                    SocketUtils.closeConnection(IPAddress)
                                }
                            }

                            var message: String?
                            try {
                                while (socket.isConnected) {
                                    message = bufferedReader.readLine()
                                    if ( message.isNullOrEmpty().not() ) {
                                        Log.i(TAG, "Receive message: $IPAddress $message")
                                        MessageHandler.messageHandler(IPAddress, message, localBroadcastManager)
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                SocketUtils.closeConnection(IPAddress)
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
    @OptIn(DelicateCoroutinesApi::class)
    private fun startClient(ip: String, port: Int) {
        val TAG = "SocketClient"
        if ( SocketData.socketMap["/$ip:$port"] != null ) return
        GlobalScope.launch(context = Dispatchers.IO) {
            val socket = Socket(ip, port)
            val IPAddress = socket.remoteSocketAddress.toString()
            Log.i(TAG, "Start client = $socket")
            Log.i(TAG, "Connect to: $IPAddress")
            SocketData.socketMap[IPAddress] = socket
            SocketData.connectionMap[IPAddress] = System.currentTimeMillis()
            try {
                val inputStream = socket.getInputStream()
                val bufferedReader = inputStream.bufferedReader()
                val outputStream = socket.getOutputStream()

                SocketData.inputStreamMap[IPAddress] = inputStream
                SocketData.outputStreamMap[IPAddress] = outputStream
                socket.keepAlive = true
                SocketUtils.sendHeartbeat(IPAddress)

                var message: String?
                try {
                    while (socket.isConnected) {
                        message = bufferedReader.readLine()
                        if ( message.isNullOrEmpty().not() ) {
                            Log.i(TAG, "Receive message: $IPAddress $message")
                            MessageHandler.messageHandler(IPAddress, message, localBroadcastManager)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    SocketUtils.closeConnection(IPAddress)
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
        SocketUtils.closeAllConnection()
        HyliConnectState.serviceStateMap["SocketServer"] = ServiceState("stopped", getString(R.string.state_service_stopped, getString(R.string.service_socket_server)))
    }
    @OptIn(DelicateCoroutinesApi::class)
    private fun checkConnection() {
        if ( ::checkConnectionJob.isInitialized.not() ) {
            checkConnectionJob = GlobalScope.launch(context = Dispatchers.Main) {
                while (true) {
                    val map = SocketData.connectionMap
                    map.forEach { (ip, time) ->
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
    private fun registerNsdService() {
        if ( nsdRunning ) {
            return
        }
        HyliConnectState.serviceStateMap["NsdService"] = ServiceState("stopped", getString(R.string.state_service_stopped, getString(R.string.service_nsd_service)))

        val uuid = configMap["uuid"].toString()
        val nickname = configMap["nickname"].toString()
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
        mNsdServiceInfo.setAttribute("platform", configMap["platform"].toString())
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
        HyliConnectState.serviceStateMap["NsdService"] = ServiceState("running", getString(R.string.state_service_running, getString(R.string.service_nsd_service)))
    }
    private fun unregisterNsdService() {
        if ( nsdRunning.not() ) {
            return
        }
        mNsdManager.unregisterService(NsdRegistrationListener)
        HyliConnectState.serviceStateMap["NsdService"] = ServiceState("stopped", getString(R.string.state_service_stopped, getString(R.string.service_nsd_service)))
    }
    private fun restartNsdService() {
        if ( nsdRunning ) {
            mNsdManager.unregisterService(NsdRegistrationListener)
            nsdRunning = false
        }
        registerNsdService()
    }
}