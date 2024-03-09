package xyz.hyli.connect.bean

import com.google.protobuf.ByteString

data class ApplicationInfo(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val mainActivity: String,
    val icon: ApplicationIcon? = null
)

data class ApplicationIcon(
    val width: Int,
    val height: Int,
    val data: ByteString
)
