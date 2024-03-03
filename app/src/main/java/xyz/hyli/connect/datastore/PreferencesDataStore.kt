package xyz.hyli.connect.datastore

import android.os.Build
import androidx.core.content.ContextCompat.getString
import kotlinx.coroutines.runBlocking
import xyz.hyli.connect.HyliConnect
import xyz.hyli.connect.R
import java.util.UUID.randomUUID

object PreferencesDataStore : DataStoreOwner("preferences") {
    val last_run_version_code by intPreference(0)
    val uuid by stringPreference()
    val nickname by stringPreference()
    val platform by intPreference()
    val server_port by intPreference()
    val nsd_service by booleanPreference()
    val working_mode by intPreference()
    val function_app_streaming by booleanPreference()
    val function_notification_forward by booleanPreference()
    val connect_to_myself by booleanPreference()

    init {
        runBlocking {
            if (uuid.get().isNullOrEmpty()) uuid.set(randomUUID().toString())
            if (nickname.get().isNullOrEmpty()) nickname.set(Build.BRAND + " " + Build.MODEL)
            if (platform.get() == null || platform.get() !in 0..6) platform.set(0)
            if (server_port.get() == null || server_port.get() !in 1024..65535) server_port.set(15732)
            if (nsd_service.get() == null) nsd_service.set(true)
            if (working_mode.get() == null) working_mode.set(0)
            if (function_app_streaming.get() == null) function_app_streaming.set(false)
            if (function_notification_forward.get() == null) function_notification_forward.set(false)
            if (connect_to_myself.get() == null) connect_to_myself.set(false)
        }
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
