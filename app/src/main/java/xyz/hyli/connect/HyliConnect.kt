package xyz.hyli.connect

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import androidx.lifecycle.MutableLiveData
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.shizuku.Shizuku
import rikka.sui.Sui
import xyz.hyli.connect.bean.DeviceInfo
import xyz.hyli.connect.bean.MessageQueue
import xyz.hyli.connect.bean.ServiceState
import xyz.hyli.connect.datastore.PreferencesDataStore
import xyz.hyli.connect.service.ControlService
import xyz.hyli.connect.service.SocketService
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.util.concurrent.BlockingQueue
import java.util.concurrent.ConcurrentHashMap

open class HyliConnect : Application() {
    val isRunning = MutableLiveData(false)
    private var controlService: IControlService? = null
    private val onRequestPermissionResultListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == SHIZUKU_CODE && grantResult == PackageManager.PERMISSION_GRANTED) {
                try {
                    if (PreferencesDataStore.getConfigMap(true)["app_stream_method"] in 1..2) {
                        initShizuku()
                    }
                } catch (_: Exception) { }
            }
        }

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            Shizuku.requestPermission(SHIZUKU_CODE)
        } else {
            try {
                if (PreferencesDataStore.getConfigMap(true)["app_stream_method"] in 1..2) {
                    initShizuku()
                }
            } catch (_: Exception) { }
        }
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        isRunning.postValue(false)
    }
    private val userServiceArgs =
        Shizuku.UserServiceArgs(
            ComponentName(
                BuildConfig.APPLICATION_ID,
                ControlService::class.java.name
            )
        ).processNameSuffix("service")

    private val userServiceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            isRunning.value = false
            controlService = null
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            controlService = IControlService.Stub.asInterface(service)
            if (controlService!!.init()) isRunning.value = true
        }
    }
    companion object {
        lateinit var me: HyliConnect
        const val SHIZUKU_CODE = 0x3CE9A

        // service name, state
        lateinit var serviceStateMap: ConcurrentHashMap<String, ServiceState>
        lateinit var permissionStateMap: ConcurrentHashMap<String, Boolean>

        // ip: data
        lateinit var uuidMap: ConcurrentHashMap<String, String>
        lateinit var socketMap: ConcurrentHashMap<String, Socket>
        lateinit var inputStreamMap: ConcurrentHashMap<String, InputStream>
        lateinit var outputStreamMap: ConcurrentHashMap<String, OutputStream>
        lateinit var connectionMap: ConcurrentHashMap<String, Long>

        // uuid: deviceInfo
        lateinit var deviceInfoMap: ConcurrentHashMap<String, DeviceInfo>

        // ip: blockingQueue
        lateinit var blockingQueueMap: ConcurrentHashMap<String, BlockingQueue<MessageQueue>>

        init {
            Sui.init(BuildConfig.APPLICATION_ID)
        }
    }
    override fun onCreate() {
        serviceStateMap = ConcurrentHashMap()
        permissionStateMap = ConcurrentHashMap()
        uuidMap = ConcurrentHashMap()
        socketMap = ConcurrentHashMap()
        inputStreamMap = ConcurrentHashMap()
        outputStreamMap = ConcurrentHashMap()
        connectionMap = ConcurrentHashMap()
        deviceInfoMap = ConcurrentHashMap()
        blockingQueueMap = ConcurrentHashMap()
        me = this
        super.onCreate()
        startForegroundService(Intent(this, SocketService::class.java))

        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
        Shizuku.addRequestPermissionResultListener(onRequestPermissionResultListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
    }
    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            HiddenApiBypass.addHiddenApiExemptions("")
        }
    }

    private fun bindShizukuService() {
        try {
            Shizuku.bindUserService(userServiceArgs, userServiceConnection)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun initShizuku() {
        if (controlService?.asBinder()?.pingBinder() == true) return
        bindShizukuService()
    }

    fun getControlService() = controlService

    fun getContext(): Context {
        return me.applicationContext
    }
}
