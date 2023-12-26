package xyz.hyli.connect.bean

data class DeviceInfo(
    val api_version: Int,
    val app_version: Int,
    val app_version_name: String,
    val platform: String,
    val uuid: String,
    val nickname: String,
    val ip_address: MutableList<String>,
    val port: Int
)
