package xyz.hyli.connect.datastore

import android.os.Build
import kotlinx.coroutines.runBlocking
import xyz.hyli.connect.BuildConfig
import java.util.UUID.randomUUID
import java.util.concurrent.ConcurrentHashMap

object PreferencesDataStore : DataStoreOwner("preferences") {
    private var isInit = false
    private val configMap = ConcurrentHashMap<String, Any>()
    val platformMap = linkedMapOf(
        0 to "Android Phone",
        1 to "Android TV",
        2 to "Android Wear",
        3 to "Windows",
        4 to "Linux",
        5 to "Mac",
        6 to "Web"
    )
    val appStreamMethodMap = linkedMapOf(
        0 to "Root + Xposed",
        1 to "Shizuku + Xposed",
        2 to "Shizuku"
    )

    val last_run_version_code by intPreference(0)
    val uuid by stringPreference()
    val nickname by stringPreference()
    val platform by intPreference()
    val server_port by intPreference()
    val nsd_service by booleanPreference()
    val is_stream by booleanPreference()
    val app_stream_method by intPreference()
    val notification_forward by booleanPreference()
    val connect_to_myself by booleanPreference()

    private fun refresh() {
        runBlocking {
            if ( uuid.get().isNullOrEmpty() ) uuid.set(randomUUID().toString())
            if ( nickname.get().isNullOrEmpty() ) nickname.set(Build.BRAND + " " + Build.MODEL)
            if ( platform.get() == null || platform.get() !in 0..6 ) platform.set(0)
            if ( server_port.get() == null || server_port.get() !in 1024..65535 ) server_port.set(15732)
            if ( nsd_service.get() == null ) nsd_service.set(true)
            if ( is_stream.get() == null ) is_stream.set(false)
            if ( app_stream_method.get() == null
                || ((Build.VERSION_CODES.S <= Build.VERSION.SDK_INT || BuildConfig.DEBUG) && app_stream_method.get() !in 0..2)
                || ((Build.VERSION_CODES.S > Build.VERSION.SDK_INT || !BuildConfig.DEBUG) && app_stream_method.get() !in 0..1)) {
                app_stream_method.set(1)
            } else
            if ( notification_forward.get() == null ) notification_forward.set(false)
            if ( connect_to_myself.get() == null ) connect_to_myself.set(false)

            configMap["uuid"] = uuid.get()!!
            configMap["nickname"] = nickname.get()!!
            configMap["platform"] = platformMap[platform.get()]!!
            configMap["server_port"] = server_port.get()!!
            configMap["nsd_service"] = nsd_service.get()!!
            configMap["is_stream"] = is_stream.get()!!
            configMap["app_stream_method"] = app_stream_method.get()!!
            configMap["notification_forward"] = notification_forward.get()!!
            configMap["connect_to_myself"] = connect_to_myself.get()!!
            isInit = true
        }
    }

    fun getConfigMap(refresh: Boolean = true): MutableMap<String, Any> {
        if (isInit.not() || configMap.isEmpty() || refresh) refresh()
        return configMap
    }
}