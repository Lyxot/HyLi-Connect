package xyz.hyli.connect.datastore

import android.util.Log
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
    val stream_method by stringPreference()
    val refuse_fullscreen_method by stringPreference()
    val notification_forward by booleanPreference()

    private fun init() {
        runBlocking {
            // init every update
            if ( last_run_version_code.get()!! < BuildConfig.VERSION_CODE && BuildConfig.DEBUG ) {
                if ( uuid.get().isNullOrEmpty() ) uuid.set(randomUUID().toString())
                if ( nickname.get().isNullOrEmpty() ) nickname.set(android.os.Build.BRAND + " " + android.os.Build.MODEL)
                if ( platform.get().isNullOrEmpty() ) platform.set("Android Phone")
                if ( server_port.get() == null ) server_port.set(15732)
                if ( is_stream.get() == null ) is_stream.set(false)
                if ( stream_method.get().isNullOrEmpty() ) stream_method.set("Shizuku")
                if ( refuse_fullscreen_method.get().isNullOrEmpty() ) refuse_fullscreen_method.set("Xposed")
                if ( notification_forward.get() == null ) notification_forward.set(false)
            }
            configMap["uuid"] = uuid.get()!!
            configMap["nickname"] = nickname.get()!!
            configMap["platform"] = platform.get()!!
            configMap["server_port"] = server_port.get()!!
            configMap["is_stream"] = is_stream.get()!!
            configMap["stream_method"] = stream_method.get()!!
            configMap["refuse_fullscreen_method"] = refuse_fullscreen_method.get()!!
            configMap["notification_forward"] = notification_forward.get()!!
            isInit = true
        }
    }

    fun getConfigMap(refresh: Boolean = false): MutableMap<String, Any> {
        if (isInit.not() || configMap.isEmpty() || refresh) init()
        Log.i("PreferencesDataStore", configMap.toString())
        return configMap
    }
}