package xyz.hyli.connect.ui

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import xyz.hyli.connect.socket.SERVER_PORT
import java.util.UUID.randomUUID

object ConfigHelper {
    private val configList = listOf(
        "uuid",
        "nickname",
        "server_port",
        "is_stream"
    )
    private val configMap = mutableMapOf<String, Any>()

    private fun getUUID(sharedPreferences: SharedPreferences, editor: SharedPreferences.Editor): String{
        var uuid = sharedPreferences.getString("uuid", "").toString()
        if (uuid == "") {
            uuid = randomUUID().toString()
            editor.putString("uuid", uuid)
            editor.apply()
        }
        return uuid
    }
    private fun getNickname(sharedPreferences: SharedPreferences, editor: SharedPreferences.Editor): String {
        var nickname = sharedPreferences.getString("nickname", "").toString()
        if (nickname == "") {
            nickname = Build.BRAND + " " + Build.MODEL
            editor.putString("nickname", nickname)
            editor.apply()
        }
        return nickname
    }
    private fun getServerPort(sharedPreferences: SharedPreferences, editor: SharedPreferences.Editor): Int {
        var serverPort = sharedPreferences.getInt("server_port", 0)
        if (serverPort == 0) {
            serverPort = SERVER_PORT
            editor.putInt("server_port", serverPort)
            editor.apply()
        }
        return serverPort
    }
    private fun getIsStream(sharedPreferences: SharedPreferences, editor: SharedPreferences.Editor): Boolean {
        if (sharedPreferences.contains("is_stream").not()) {
            editor.putBoolean("is_stream", false)
            editor.apply()
        }
        return sharedPreferences.getBoolean("is_stream", false)
    }
    private fun getStreamMethod(sharedPreferences: SharedPreferences, editor: SharedPreferences.Editor): String {
        if (sharedPreferences.contains("stream_method").not()) {
            editor.putString("stream_method", "Shizuku")
            editor.apply()
        }
        return sharedPreferences.getString("stream_method", "Shizuku").toString()
    }
    private fun getRefuseFullScreenMethod(sharedPreferences: SharedPreferences, editor: SharedPreferences.Editor): String {
        if (sharedPreferences.contains("refuse_fullscreen_method").not()) {
            editor.putString("refuse_full_screen_method", "Shizuku")
            editor.apply()
        }
        return sharedPreferences.getString("refuse_full_screen_method", "Shizuku").toString()
    }
    private fun initConfig(sharedPreferences: SharedPreferences, editor: SharedPreferences.Editor) {
        configMap["uuid"] = getUUID(sharedPreferences, editor)
        configMap["nickname"] = getNickname(sharedPreferences, editor)
        configMap["server_port"] = getServerPort(sharedPreferences, editor)
        configMap["is_stream"] = getIsStream(sharedPreferences, editor)
        configMap["stream_method"] = getStreamMethod(sharedPreferences, editor)
        configMap["refuse_full_screen_method"] = getRefuseFullScreenMethod(sharedPreferences, editor)
    }
    fun getConfigMap(context: Context? = null): MutableMap<String, Any> {
        if (configMap.isEmpty() && context != null) {
            val sharedPreferences = context.getSharedPreferences("config", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            initConfig(sharedPreferences, editor)
        }
        return configMap
    }
}
