package xyz.hyli.connect.ui

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.text.format.Formatter
import android.util.Log
import java.util.UUID.randomUUID
import java.net.NetworkInterface

class ConfigHelper {
    companion object {
        var uuid: String = ""
        var NICKNAME: String = ""
        var SERVER_PORT: Int = 15372
    }

    fun getUUID(sharedPreferences: SharedPreferences, editor: SharedPreferences.Editor): String{
        uuid = sharedPreferences.getString("uuid", "").toString()
        if (uuid == "") {
            uuid = randomUUID().toString()
            editor.putString("uuid", uuid)
            editor.apply()
        }
        return uuid
    }
    fun getNickname(sharedPreferences: SharedPreferences, editor: SharedPreferences.Editor): String {
        NICKNAME = sharedPreferences.getString("nickname", "").toString()
        if (NICKNAME == "") {
            NICKNAME = Build.BRAND + " " + Build.MODEL
            editor.putString("nickname", NICKNAME)
            editor.apply()
        }
        return NICKNAME
    }
    fun editNickname(sharedPreferences: SharedPreferences, editor: SharedPreferences.Editor, nickname: String) {
        NICKNAME = nickname
        editor.putString("nickname", NICKNAME)
        editor.apply()
    }
    fun getServerPort(sharedPreferences: SharedPreferences, editor: SharedPreferences.Editor): Int {
        var serverPort = sharedPreferences.getInt("server_port", 0)
        if (serverPort == 0) {
            serverPort = SERVER_PORT
            editor.putInt("server_port", serverPort)
            editor.apply()
        }
        return serverPort
    }
    fun editServerPort(sharedPreferences: SharedPreferences, editor: SharedPreferences.Editor, serverPort: Int) {
        editor.putInt("server_port", serverPort)
        editor.apply()
    }
    fun initConfig(sharedPreferences: SharedPreferences, editor: SharedPreferences.Editor) {
        getUUID(sharedPreferences, editor)
        getNickname(sharedPreferences, editor)
        getServerPort(sharedPreferences, editor)
    }
}
