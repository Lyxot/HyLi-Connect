package xyz.hyli.connect.bean

data class ServiceState(
    // running, error, stopped
    val state: String,
    val message: String? = null
)
