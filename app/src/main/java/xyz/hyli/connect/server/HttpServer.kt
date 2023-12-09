package xyz.hyli.connect.server

import android.util.Log
import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import fi.iki.elonen.NanoHTTPD
import xyz.hyli.connect.BuildConfig.VERSION_CODE
import xyz.hyli.connect.BuildConfig.VERSION_NAME
import xyz.hyli.connect.server.ServerConfig.Companion.clientMap
import xyz.hyli.connect.ui.main.ConfigHelper
import xyz.hyli.connect.utils.PackageUtils


class HttpServer: NanoHTTPD(HTTP_PORT) {
    private val TAG = "HttpServer"
    data class Clients(
        val ip_addr: String,
        val nickname: String
    )
    override fun serve(session: IHTTPSession?): Response {
        return respond(session)
    }
    private fun respond(session: IHTTPSession?): Response {
        Log.i(TAG, "receive: ${session?.uri}, method = ${session?.method}, header = ${session?.headers}, " +
                "params = ${session?.parameters}")
        var data = "{}"
        if ( Method.GET == session?.method ) {
            return when ( session.uri ) {
                HTTP_URL_INFO -> {
                    data = "{\"api_version\":$API_VERSION," +
                            "\"app_version\":$VERSION_CODE," +
                            "\"app_version_name\":\"$VERSION_NAME\"," +
                            "\"platform\":\"$PLATFORM\"" +
                            "}"
                    getResponse(200, data, "success")
                }
                else -> getResponse(404, data, "not found")
            }
        } else if ( Method.POST == session?.method ) {
            val header = session.headers
            val params = session.parameters

            // check header and params
            if ( header["content-type"].toString() != "application/json" || params == null) {
                return getResponse(400, data, "bad request")
            }
            val ip_addr = header["http-client-ip"].toString()
            val uuid = params["uuid"]?.get(0).toString() ?: ""
            val nickname = params["nickname"]?.get(0).toString() ?: ""
            data = "{\"uuid\":\"${ConfigHelper.uuid}\",\"ip_addr\":\"${ConfigHelper.IP_ADDRESS}\",\"nickname\":\"${ConfigHelper.NICKNAME}\"}"

            // check uuid and ip_addr
            if ( uuid == "" || ip_addr == "" ) {
                return getResponse(400, data, "bad request")
            }

            // connect
            // temporary
            if ( session.uri == HTTP_URL_CONNECT ) {
                return if ( clientMap.containsKey(uuid) && (clientMap[uuid]?.ip_addr?: "") == ip_addr ) {
                    getResponse(200, data, "already connected")
                } else {
                    clientMap.put(uuid, Clients(ip_addr, nickname))
                    getResponse(200, data, "success")
                }
            }

            // authorization && response
            return if ( clientMap.containsKey(uuid) && (clientMap[uuid]?.ip_addr ?: "") == ip_addr ) {
                when ( session.uri ) {
                    HTTP_URL_DISCONNECT -> {
                        clientMap.remove(uuid)
                        getResponse(200, data, "success")
                    }

                    HTTP_URL_HEARTBEAT -> {
                        getResponse(200, data, "success")
                    }

                    HTTP_URL_CLIENT_LIST -> {
                        val data = JSONObject()
                        clientMap.forEach() { (uuid, client) ->
                            data[uuid] = client
                        }
                        getResponse(200, data, "success")
                    }

                    HTTP_URL_APP_LIST -> {
                        val data = JSONObject()
                        PackageUtils.packageList.forEach() { packageName ->
                            val e = JSONObject()
                            e["name"] = PackageUtils.appNameMap[packageName]
                            e["width"] = PackageUtils.appIconMap[packageName]?.width ?: 0
                            e["height"] = PackageUtils.appIconMap[packageName]?.height ?: 0
                            e["icon"] = PackageUtils.appIconMap[packageName]?.iconByteArray ?: ByteArray(0)
                            data[packageName] = e
                        }
                        getResponse(200, data, "success")
                    }

                    else -> getResponse(404, data, "not found")
                }
            } else {
                getResponse(400, data, "authorization failed")
            }
        } else {
            return getResponse(405, data, "method not allowed")
        }
    }
    private fun <T: Any> getResponse(code: Int, data: T, msg: String): Response {
        val status = Response.Status.lookup(code)
        Log.i(TAG, "response: code = $code, data = $data, msg = $msg")
        return newFixedLengthResponse(status,
            "application/json",
            "{\"code\":$code,\"data\":$data,\"msg\":\"$msg\"}")
    }
}