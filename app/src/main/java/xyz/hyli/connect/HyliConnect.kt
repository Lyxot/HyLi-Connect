package xyz.hyli.connect

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import androidx.lifecycle.MutableLiveData
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.shizuku.Shizuku
import rikka.sui.Sui
import xyz.hyli.connect.bean.DeviceInfo
import xyz.hyli.connect.bean.ServiceState
import xyz.hyli.connect.service.ControlService
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket


open class HyliConnect : Application() {
    val isRunning = MutableLiveData(false)
    private var controlService: IControlService? = null
    private val onRequestPermissionResultListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == SHIZUKU_CODE && grantResult == PackageManager.PERMISSION_GRANTED) {
                initShizuku()
            }
        }

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            Shizuku.requestPermission(SHIZUKU_CODE)
        } else {
            initShizuku()
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
        val SHIZUKU_CODE = 0x3CE9A

        // service name, state
        lateinit var serviceStateMap: MutableMap<String, ServiceState>
        lateinit var permissionStateMap: MutableMap<String, Boolean>

        // ip: data
        lateinit var uuidMap: MutableMap<String, String>
        lateinit var socketMap: MutableMap<String, Socket>
        lateinit var inputStreamMap: MutableMap<String, InputStream>
        lateinit var outputStreamMap: MutableMap<String, OutputStream>
        lateinit var connectionMap: MutableMap<String, Long>
        // uuid: deviceInfo
        lateinit var deviceInfoMap: MutableMap<String, DeviceInfo>

        init {
            Sui.init(BuildConfig.APPLICATION_ID)
        }
    }
    override fun onCreate() {
        serviceStateMap = mutableMapOf()
        permissionStateMap = mutableMapOf()
        uuidMap = mutableMapOf()
        socketMap = mutableMapOf()
        inputStreamMap = mutableMapOf()
        outputStreamMap = mutableMapOf()
        connectionMap = mutableMapOf()
        deviceInfoMap = mutableMapOf()
        me = this
        super.onCreate()

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