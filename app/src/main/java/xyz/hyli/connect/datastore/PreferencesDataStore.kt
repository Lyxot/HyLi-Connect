package xyz.hyli.connect.datastore

import kotlinx.coroutines.runBlocking
import xyz.hyli.connect.socket.SERVER_PORT
import java.util.UUID.randomUUID

object PreferencesDataStore : DataStoreOwner("preferences") {
    private var isInit = false
    private val configMap = mutableMapOf<String, Any>()

    val uuid by stringPreference("")
    val nickname by stringPreference("")
    val server_port by intPreference(SERVER_PORT)
    val is_stream by booleanPreference(false)
    val stream_method by stringPreference("Shizuku")
    val refuse_fullscreen_method by stringPreference("Shizuku")

    private fun init() {
        if (isInit) return
        runBlocking {
            if ( uuid.get() == "" ) {
                uuid.set(randomUUID().toString())
            }
            if ( nickname.get() == "" ) {
                nickname.set(android.os.Build.BRAND + " " + android.os.Build.MODEL)
            }
            configMap["uuid"] = uuid.get()!!
            configMap["nickname"] = nickname.get()!!
            configMap["server_port"] = server_port.get()!!
            configMap["is_stream"] = is_stream.get()!!
            configMap["stream_method"] = stream_method.get()!!
            configMap["refuse_fullscreen_method"] = refuse_fullscreen_method.get()!!
            isInit = true
        }
    }

    fun getConfigMap(refresh: Boolean = false): MutableMap<String, Any> {
        if (isInit.not() || configMap.isEmpty() || refresh) init()
        return configMap
    }
}