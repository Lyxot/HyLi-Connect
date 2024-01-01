package xyz.hyli.connect.socket

import xyz.hyli.connect.bean.DeviceInfo
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket

object SocketData {
    // ip: data
    var uuidMap: HashMap<String, String> = HashMap()
    var socketMap: HashMap<String, Socket> = HashMap()
    var inputStreamMap: HashMap<String, InputStream> = HashMap()
    var outputStreamMap: HashMap<String, OutputStream> = HashMap()
    var connectionMap: HashMap<String, Long> = HashMap()
    // uuid: deviceInfo
    var deviceInfoMap: HashMap<String, DeviceInfo> = HashMap()
}