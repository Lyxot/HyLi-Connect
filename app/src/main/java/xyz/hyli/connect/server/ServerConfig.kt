package xyz.hyli.connect.server

const val API_VERSION = 1
const val PLATFORM = "Android Phone"
const val HTTP_PORT = 15372

/**
 * 获取服务端信息
 * GET
 * {
 *   "code": 200,
 *   "data": {
 *      "api_version": 1,
 *      "app_version": 10000,
 *      "app_version_name": "1.0.0",
 *      "platform": "Android Phone"
 *   },
 *   "msg": "success"
 * }
 */
const val HTTP_URL_INFO = "/api/info"

/**
 * 连接客户端
 * POST
 */
const val HTTP_URL_CONNECT = "/api/connect"

/**
 * 断开客户端
 * POST
 */
const val HTTP_URL_DISCONNECT = "/api/disconnect"

/**
 * 心跳
 * POST
 */
const val HTTP_URL_HEARTBEAT = "/api/heartbeat"

/**
 * 获取客户端列表
 * POST
 */
const val HTTP_URL_CLIENT_LIST = "/api/clients"

/**
 * 获取应用列表
 * POST
 */
const val HTTP_URL_APP_LIST = "/api/apps"

class ServerConfig {
    companion object {
        val clientMap: MutableMap<String, HttpServer.Clients> = mutableMapOf()
    }
}
