package xyz.hyli.connect.utils

import android.content.Context
import android.util.Log
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.SocketException

object NetworkUtils {
    fun getLocalIPInfo(context: Context): Map<String, String> {
        val map = mutableMapOf<String, String>()
        try {
            val enNetI = NetworkInterface
                .getNetworkInterfaces()
            while (enNetI.hasMoreElements()) {
                val netI = enNetI.nextElement()
                val enumIpAddr = netI
                    .inetAddresses
                while (enumIpAddr.hasMoreElements()) {
                    val inetAddress = enumIpAddr.nextElement()
                    if (inetAddress is Inet4Address && !inetAddress.isLoopbackAddress()) {
                        map[netI.displayName] = inetAddress.getHostAddress()?.toString() ?: "0.0.0.0"
                    }
                }
            }
            if (map.containsKey("wlan0").not()) {
                val connectivityManager = context.getSystemService(
                    Context.CONNECTIVITY_SERVICE
                ) as android.net.ConnectivityManager
                val networkInfo = connectivityManager.activeNetworkInfo
                if (networkInfo != null) {
                    if (networkInfo.type == android.net.ConnectivityManager.TYPE_WIFI) {
                        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager
                        wifiManager.wifiState
                        val ipAddress = intToIp(wifiManager.connectionInfo.ipAddress)
                        map["wlan0"] = ipAddress
                    }
                }
            }
        } catch (e: SocketException) {
            e.printStackTrace()
        }
        Log.i("NetworkUtils", map.toString())
        return map
    }
    private fun intToIp(i: Int): String {
        return (i and 0xFF).toString() + "." + (i shr 8 and 0xFF) + "." + (i shr 16 and 0xFF) + "." + (i shr 24 and 0xFF)
    }
    fun isPortInUse(port: Int): Boolean {
        return try {
            val socket = ServerSocket(port)
            socket.close()
            false
        } catch (e: Exception) {
            true
        }
    }
    fun getAvailablePort(): Int {
        var port = 0
        while (true) {
            port = (Math.random() * 10000).toInt() + 10240
            if (isPortInUse(port).not()) {
                break
            }
        }
        return port
    }
}
