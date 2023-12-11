package xyz.hyli.connect.client;

import android.util.Log
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import org.json.JSONObject
import xyz.hyli.connect.server.HTTP_PORT
import xyz.hyli.connect.server.HTTP_URL_INFO
import xyz.hyli.connect.ui.main.ConfigHelper

object HttpClient {
    val TAG = "HttpClient"
    val client = OkHttpClient()
    private fun method_get(ip_addr: String, uri: String, params: MutableMap<String, String>? = null ): String {
        val builder = "http://$ip_addr:${HTTP_PORT}$uri".toHttpUrlOrNull()!!.newBuilder()
        if ( params != null ) {
            for ( (key, value) in params ) {
                builder.addQueryParameter(key, value)
            }
        }
        val request = okhttp3.Request.Builder()
            .url(builder.build())
            .header("content-type", "application/json")
            .build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: ""
        Log.i(TAG, "method_get: $uri, response = $body")
        return body
    }
    fun get_info(ip_addr: String): String {
        return method_get(ip_addr, HTTP_URL_INFO)
    }
    fun connect(ip_addr: String): String {
        val params = mutableMapOf<String, String>()
        params["uuid"] = ConfigHelper.uuid
        params["nickname"] = ConfigHelper.NICKNAME
        return method_get(ip_addr, "/api/connect", params)
    }
    fun disconnect(ip_addr: String): String {
        val params = mutableMapOf<String, String>()
        params["uuid"] = ConfigHelper.uuid
        return method_get(ip_addr, "/api/disconnect", params)
    }
    fun heartbeat(ip_addr: String): String {
        val params = mutableMapOf<String, String>()
        params["uuid"] = ConfigHelper.uuid
        return method_get(ip_addr, "/api/heartbeat", params)
    }
    fun get_clients(ip_addr: String): String {
        val params = mutableMapOf<String, String>()
        params["uuid"] = ConfigHelper.uuid
        return method_get(ip_addr, "/api/clients", params)
    }
    fun get_apps(ip_addr: String): String {
        val params = mutableMapOf<String, String>()
        params["uuid"] = ConfigHelper.uuid
        return method_get(ip_addr, "/api/apps", params)
    }

}
