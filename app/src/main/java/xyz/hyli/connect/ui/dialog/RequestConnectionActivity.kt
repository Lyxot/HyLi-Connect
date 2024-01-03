package xyz.hyli.connect.ui.dialog

import android.content.DialogInterface
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.Settings
import android.view.Gravity
import android.view.Window
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AlertDialog
import com.alibaba.fastjson2.JSONObject
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import xyz.hyli.connect.R
import xyz.hyli.connect.socket.utils.SocketUtils
import xyz.hyli.connect.ui.theme.HyliConnectTheme

class RequestConnectionActivity : ComponentActivity() {
    private lateinit var dialog: AlertDialog
    override fun onCreate(savedInstanceState: Bundle?) {
        this.requestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)

        val intent = intent
        var ip = intent.getStringExtra("ip")
        val messageData = intent.getStringExtra("data")

        setContent {
            HyliConnectTheme {
                if ((ip != null && messageData != null) && Settings.canDrawOverlays(this)) {
                    val IP_Address = ip!!.substring(1, ip!!.length)
                    val data = JSONObject.parseObject(messageData)
                    val nickname = data.getString("nickname")
                    val uuid = data.getString("uuid")
                    dialog = MaterialAlertDialogBuilder(this)
                        .setTitle(getString(R.string.dialog_connect_request_title))
                        .setMessage("${getString(R.string.dialog_connect_request_message, nickname, IP_Address)}\n\nUUID: $uuid")
                        .setPositiveButton(getString(R.string.dialog_connect_request_accept)) { dialog, which ->
                            SocketUtils.acceptConnection(ip!!, data)
                            dialog.dismiss()
                        }
                        .setNegativeButton(getString(R.string.dialog_connect_request_reject_countdown, 30000 / 1000)) { dialog, which ->
                            SocketUtils.rejectConnection(ip!!)
                            dialog.dismiss()
                        }
                        .setCancelable(true)
                        .create()
                    val timer = object : CountDownTimer(30000, 1000) {
                        override fun onTick(millisUntilFinished: Long) {
                            dialog.getButton(DialogInterface.BUTTON_NEGATIVE).text = getString(R.string.dialog_connect_request_reject_countdown, millisUntilFinished / 1000)
                        }
                        override fun onFinish() {
                            dialog.dismiss()
                        }
                    }
                    dialog.setOnDismissListener {
                        timer.cancel()
                    }
                    dialog.window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
                    dialog.window?.attributes?.height = WindowManager.LayoutParams.WRAP_CONTENT
                    dialog.window?.attributes?.width = WindowManager.LayoutParams.WRAP_CONTENT
                    dialog.window?.attributes?.gravity = Gravity.CENTER
                    dialog.show()
                    timer.start()
                }
                finish()
            }
        }
    }
}