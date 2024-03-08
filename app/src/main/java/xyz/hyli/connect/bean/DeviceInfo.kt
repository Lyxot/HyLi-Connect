package xyz.hyli.connect.bean

data class DeviceInfo(
    val apiVersion: Int,
    val appVersion: Int,
    val appVersionName: String,
    val platform: String,
    val uuid: String,
    val nickname: String,
    val ipAddress: MutableList<String>,
    val connectedPort: Int? = null,
    val serverPort: Int
)
