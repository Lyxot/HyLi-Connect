package xyz.hyli.connect.ui.dialog

import android.os.Bundle
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import xyz.hyli.connect.R
import xyz.hyli.connect.bean.DeviceInfo
import xyz.hyli.connect.socket.SocketUtils
import xyz.hyli.connect.ui.theme.HyLiConnectColorScheme
import xyz.hyli.connect.ui.theme.HyLiConnectTheme
import xyz.hyli.connect.ui.theme.HyLiConnectTypography

class RequestConnectionActivity : ComponentActivity() {
    private lateinit var dialog: AlertDialog
    override fun onCreate(savedInstanceState: Bundle?) {
        this.requestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)

        val intent = intent
        val ip = intent.getStringExtra("ip")
        val nickname = intent.getStringExtra("nickname")
        val uuid = intent.getStringExtra("uuid")
        val api_version = intent.getIntExtra("api_version", 0)
        val app_version = intent.getIntExtra("app_version", 0)
        val app_version_name = intent.getStringExtra("app_version_name")
        val platform = intent.getStringExtra("platform")
        val server_port = intent.getIntExtra("server_port", 15732)

        setContent {
            HyLiConnectTheme {
                val showRequestDialog = remember { mutableStateOf(true) }
                if ((ip.isNullOrEmpty().not() && nickname.isNullOrEmpty().not() && uuid.isNullOrEmpty().not() && api_version != 0 && app_version != 0 && app_version_name.isNullOrEmpty().not() && platform.isNullOrEmpty().not())) {
                    val IP_Address = ip!!.substring(1, ip.length)
                    if (showRequestDialog.value) {
                        AlertDialog(
                            modifier = Modifier.fillMaxWidth(0.9f),
                            onDismissRequest = {
                                showRequestDialog.value = false
                                finish() },
                            title = {
                                Column(modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp)) {
                                    Text(stringResource(id = R.string.dialog_connect_request_title), style = HyLiConnectTypography.titleLarge)
                                    Text(
                                        text = stringResource(id = R.string.dialog_connect_request_message, nickname!!, IP_Address),
                                        style = HyLiConnectTypography.bodyLarge,
                                        modifier = Modifier.padding(top = 6.dp),
                                    )
                                    Text(
                                        text = uuid ?: "",
                                        style = HyLiConnectTypography.bodySmall,
                                        modifier = Modifier.padding(top = 6.dp),
                                        color = HyLiConnectColorScheme().outline
                                    )
                                }
                            },
                            confirmButton = {
                                TextButton(
                                    modifier = Modifier.padding(end = 16.dp),
                                    onClick = {
                                        val deviceInfo = DeviceInfo(
                                            api_version,
                                            app_version,
                                            app_version_name ?: "",
                                            platform ?: "",
                                            uuid ?: "",
                                            nickname ?: "",
                                            mutableListOf(ip.substring(1, ip.length).split(":")[0]),
                                            ip.substring(1, ip.length).split(":").last().toInt(),
                                            server_port
                                        )
                                        SocketUtils.acceptConnection(ip, deviceInfo)
                                        showRequestDialog.value = false
                                        finish()
                                    }
                                ) {
                                    Text(stringResource(id = R.string.dialog_connect_request_accept), style = HyLiConnectTypography.bodyLarge)
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    modifier = Modifier.padding(end = 16.dp),
                                    onClick = {
                                        SocketUtils.rejectConnection(ip)
                                        showRequestDialog.value = false
                                        finish()
                                    }
                                ) {
                                    val countDown = remember { mutableIntStateOf(30) }
                                    LaunchedEffect(countDown.intValue) {
                                        delay(1000)
                                        if (countDown.intValue > 0) {
                                            countDown.intValue -= 1
                                        } else {
                                            SocketUtils.rejectConnection(ip)
                                            showRequestDialog.value = false
                                            finish()
                                        }
                                    }
                                    Text("${stringResource(R.string.dialog_connect_request_reject_countdown)} (${countDown.intValue})", style = HyLiConnectTypography.bodyLarge)
                                }
                            },
                            properties = DialogProperties(usePlatformDefaultWidth = false),
                            containerColor = HyLiConnectColorScheme().background
                        )
                    } else {
                        finish()
                    }
                } else {
                    finish()
                }
            }
        }
    }
}
