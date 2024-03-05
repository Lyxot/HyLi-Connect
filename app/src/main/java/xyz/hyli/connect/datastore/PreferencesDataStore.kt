package xyz.hyli.connect.datastore

import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat.getString
import kotlinx.coroutines.runBlocking
import xyz.hyli.connect.BuildConfig
import xyz.hyli.connect.HyliConnect
import xyz.hyli.connect.R
import java.util.UUID.randomUUID

object PreferencesDataStore : DataStoreOwner("preferences") {
    val last_run_version_code by intPreference(0)
    val uuid by stringPreference(randomUUID().toString())
    val nickname by stringPreference(Build.BRAND + " " + Build.MODEL)
    val platform by intPreference(0)
    val server_port by intPreference(15732)
    val nsd_service by booleanPreference(true)
    val working_mode by intPreference(0)
    val function_app_streaming by booleanPreference(false)
    val function_notification_forward by booleanPreference(false)
    val connect_to_myself by booleanPreference(BuildConfig.DEBUG)

    init {
        runBlocking {
            if (uuid.get(null).isNullOrEmpty()) uuid.reset()
            if (nickname.get(null).isNullOrEmpty()) nickname.reset()
            if (platform.get(null) == null || platform.get(null) !in 0..6) platform.reset()
            if (server_port.get(null) == null || server_port.get(null) !in 1024..65535) server_port.reset()
            if (nsd_service.get(null) == null) nsd_service.reset()
            if (working_mode.get(null) == null || working_mode.get(null) !in 0..2) working_mode.reset()
            if (function_app_streaming.get(null) == null) function_app_streaming.reset()
            if (function_notification_forward.get(null) == null) function_notification_forward.reset()
            if (connect_to_myself.get(null) == null) connect_to_myself.reset()
        }
    }

    suspend fun resetAll() {
        platform.reset()
        server_port.reset()
        nsd_service.reset()
        working_mode.reset()
        function_app_streaming.reset()
        function_notification_forward.reset()
        connect_to_myself.reset()
        Log.i("PreferencesDataStore", "All preferences reset")
    }

    val platformMap = linkedMapOf(
        0 to "Android Phone",
        1 to "Android TV",
        2 to "Android Wear",
        3 to "Windows",
        4 to "Linux",
        5 to "Mac",
        6 to "Web"
    )
    val workingModeMap = linkedMapOf(
        0 to getString(HyliConnect().getContext(), R.string.working_mode_basic),
        1 to getString(HyliConnect().getContext(), R.string.working_mode_shizuku),
        2 to getString(HyliConnect().getContext(), R.string.working_mode_root),
    )
}
