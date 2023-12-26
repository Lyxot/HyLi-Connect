package xyz.hyli.connect.socket

import java.io.InputStream
import java.io.OutputStream
import java.net.Socket

const val API_VERSION = 1
const val PLATFORM = "Android Phone"
const val SERVER_PORT = 15372
const val SERVICE_TYPE = "_hyli-connect._tcp."

/**
 * Json
 * {
 *   "message_type": String
 *   "command": String,
 *   "data": {
 *      JsonObject
 *      },
 *   "uuid": String,
 *   "status": String
 * }
 */

// Request
/** get_info
 * data: {
 *   "api_version": Int,
 *   "app_version": Int,
 *   "app_version_name": String,
 *   "platform": String,
 *   "uuid": String,
 *   "nickname": String
 *   }
 */
const val COMMAND_GET_INFO = "get_info"
/** connect
 * data: {
 *   "uuid": String,
 *   "nickname": String
 *   }
 */
const val COMMAND_CONNECT = "connect"
/** disconnect
 * data: {}
 */
const val COMMAND_DISCONNECT = "disconnect"
/** get_clients
 * data: {
 *   "clients": [
 *      "${uuid}": {
 *          "ip_address": String,
 *          "nickname": String
 *          }
 *   ]
 * }
 */
const val COMMAND_CLIENT_LIST = "get_clients"
/** get_apps
 * data: {
 *   "apps": [
 *      "${package_name}": {
 *          "name": String,
 *          "version": String,
 *          "width": Int,
 *          "height": String,
 *          "icon": String
 *          }
 *   ]
 * }
 */
const val COMMAND_APP_LIST = "get_apps"
class SocketConfig {
    companion object {
        val NON_AUTH_COMMAND = listOf(
            COMMAND_GET_INFO
        )
        val AUTH_COMMAND = listOf(
            COMMAND_CONNECT,
            COMMAND_DISCONNECT,
            COMMAND_CLIENT_LIST,
            COMMAND_APP_LIST
        )

        var uuidMap: HashMap<String, String> = HashMap()
        var nicknameMap: HashMap<String, String> = HashMap()
        var socketMap: HashMap<String, Socket> = HashMap()
        var inputStreamMap: HashMap<String, InputStream> = HashMap()
        var outputStreamMap: HashMap<String, OutputStream> = HashMap()
        var connectionMap: HashMap<String, Long> = HashMap()
        var isMainServer: Boolean = false
    }
}
