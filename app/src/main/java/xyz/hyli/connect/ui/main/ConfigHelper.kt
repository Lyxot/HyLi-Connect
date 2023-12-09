package xyz.hyli.connect.ui.main

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
        var IP_ADDRESS: String = ""
        var NICKNAME: String = ""
    }

    fun getUUID(sharedPreferences: SharedPreferences, editor: SharedPreferences.Editor): String{
        uuid = sharedPreferences.getString("uuid", "").toString()
        if (uuid == "") {
            uuid = randomUUID().toString()
            editor.putString("uuid", uuid)
            editor.apply()
        }
        Log.i("ConfigHelper", "UUID: $uuid")
        return uuid
    }
    fun getIPAddress(context: Context): String {
        val interfaces = NetworkInterface.getNetworkInterfaces()
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val networkInfo = connectivityManager.getNetworkInfo(android.net.ConnectivityManager.TYPE_WIFI)
        if (networkInfo != null) {
            val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager
            IP_ADDRESS = Formatter.formatIpAddress(wifiManager.connectionInfo.ipAddress)
            Log.i("ConfigHelper", "IP_ADDRESS: $IP_ADDRESS")
            return IP_ADDRESS
        } else {
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address.isSiteLocalAddress) {
                        IP_ADDRESS = address.hostAddress?.toString() ?: ""
                        Log.i("ConfigHelper", "IP_ADDRESS: $IP_ADDRESS")
                        return IP_ADDRESS
                    }
                }
            }
        }
        return ""
    }
    fun getNickname(sharedPreferences: SharedPreferences, editor: SharedPreferences.Editor): String {
        NICKNAME = sharedPreferences.getString("nickname", "").toString()
        if (NICKNAME == "") {
            NICKNAME = Build.BRAND + " " + Build.MODEL
            editor.putString("nickname", NICKNAME)
            editor.apply()
        }
        Log.i("ConfigHelper", "NICKNAME: $NICKNAME")
        return NICKNAME
    }
}
