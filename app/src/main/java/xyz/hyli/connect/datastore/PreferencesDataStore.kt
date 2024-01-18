package xyz.hyli.connect.datastore

import android.os.Build
import kotlinx.coroutines.runBlocking
import xyz.hyli.connect.BuildConfig
import java.util.UUID.randomUUID

object PreferencesDataStore : DataStoreOwner("preferences") {
    private var isInit = false
    private val configMap = mutableMapOf<String, Any>()

    val last_run_version_code by intPreference(0)
    val uuid by stringPreference()
    val nickname by stringPreference()
    val platform by stringPreference()
    val server_port by intPreference()
    val is_stream by booleanPreference()
    val app_stream_method by stringPreference()
    val notification_forward by booleanPreference()

    private fun init() {
        runBlocking {
            if ( uuid.get().isNullOrEmpty() ) uuid.set(randomUUID().toString())
            if ( nickname.get().isNullOrEmpty() ) nickname.set(Build.BRAND + " " + Build.MODEL)
            if ( platform.get().isNullOrEmpty() ) platform.set("Android Phone")
            if ( server_port.get() == null ) server_port.set(15732)
            if ( is_stream.get() == null ) is_stream.set(false)
            if ( app_stream_method.get().isNullOrEmpty() || listOf("Root + Xposed", "Shizuku + Xposed", "Shizuku").contains(app_stream_method.get()).not() ) {
                app_stream_method.set("Shizuku + Xposed")
            }
            if ( notification_forward.get() == null ) notification_forward.set(false)

            configMap["uuid"] = uuid.get()!!
            configMap["nickname"] = nickname.get()!!
            configMap["platform"] = platform.get()!!
            configMap["server_port"] = server_port.get()!!
            configMap["is_stream"] = is_stream.get()!!
            configMap["app_stream_method"] = app_stream_method.get()!!
            configMap["notification_forward"] = notification_forward.get()!!
            isInit = true
        }
    }

    fun getConfigMap(refresh: Boolean = false): MutableMap<String, Any> {
        if (isInit.not() || configMap.isEmpty() || refresh) init()
        return configMap
    }
}