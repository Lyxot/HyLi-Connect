package xyz.hyli.connect.ui.test

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku
import xyz.hyli.connect.R
import xyz.hyli.connect.datastore.PreferencesDataStore
import xyz.hyli.connect.service.SocketService
import xyz.hyli.connect.socket.SERVER_PORT
import xyz.hyli.connect.utils.NetworkUtils
import xyz.hyli.connect.utils.PackageUtils
import java.util.concurrent.CompletableFuture


class TestActivity : AppCompatActivity() {
    private val SHIZUKU_CODE = 0xCA07A
    private var shizukuPermissionFuture = CompletableFuture<Boolean>()
    private var appList: Deferred<List<String>>? = null
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var localBroadcastManager: LocalBroadcastManager
    var UUID: Deferred<String>? = null
    var IP_ADDRESS: Deferred<Map<String,String>>? = null
    var NICKNAME: Deferred<String>? = null

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_test)
        startForegroundService(Intent(this, SocketService::class.java))
        sharedPreferences = getSharedPreferences("config", Context.MODE_PRIVATE)
        editor = sharedPreferences.edit()
        localBroadcastManager = LocalBroadcastManager.getInstance(this)
        Log.i("MainActivity", "$sharedPreferences")

        val applist_button = findViewById<Button>(R.id.applist_button)
        val textView = findViewById<TextView>(R.id.info_textview)
        val editText = findViewById<TextView>(R.id.edittext)
        val infoButton = findViewById<Button>(R.id.info_button)
        val connectButton = findViewById<Button>(R.id.connect_button)
        val linearLayout = findViewById<LinearLayout>(R.id.linearlayout_main)
//        infoButton.setOnClickListener {
//            GlobalScope.launch(Dispatchers.IO) {
//                val ip = editText.text.toString()
//                val response = SocketClient.getInfo(ip).toString()
//                runOnUiThread { linearLayout.addView(TextView(this@TestActivity).apply { text = response }) }
//            }
//        }
        connectButton.setOnClickListener{
            val ip = editText.text.toString()
            GlobalScope.launch(Dispatchers.IO) {
                val intent = Intent("xyz.hyli.connect.service.SocketService.action.SOCKET_CLIENT")
                intent.putExtra("command", "start")
                intent.putExtra("ip", ip)
                intent.putExtra("port", SERVER_PORT)
                localBroadcastManager.sendBroadcast(intent)
            }
        }
        GlobalScope.launch(Dispatchers.IO) {
            appList = async { PackageUtils.GetAppList(packageManager) }
            UUID = async { PreferencesDataStore.getConfigMap()["uuid"].toString() }
            IP_ADDRESS = async { NetworkUtils.getLocalIPInfo(this@TestActivity) }
            NICKNAME = async { PreferencesDataStore.getConfigMap()["nickname"].toString() }
        }
        GlobalScope.launch(Dispatchers.Main) {
            textView.text = "UUID: ${UUID?.await()}\nIP_ADDRESS: ${IP_ADDRESS?.await()}\nNICKNAME: ${NICKNAME?.await()}"
        }
        applist_button.setOnClickListener {
            GlobalScope.launch(Dispatchers.Main) {
                val intent = Intent(this@TestActivity, AppListActivity::class.java)
                intent.putExtra("appList", appList?.await()?.toTypedArray())
                startActivity(intent)
            }
        }

        Shizuku.addRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == SHIZUKU_CODE) {
                val granted = grantResult == PackageManager.PERMISSION_GRANTED
                shizukuPermissionFuture.complete(granted)
            }
        }
    }

    private fun checkShizukuPermission(): Boolean {
        val b = if (!Shizuku.pingBinder()) {
            Toast.makeText(this, "Shizuku is not available", Toast.LENGTH_LONG).show()
            false
        } else if (Shizuku.isPreV11()) {
            Toast.makeText(this, "Shizuku < 11 is not supported!", Toast.LENGTH_LONG).show()
            false
        } else if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            true
        } else if (Shizuku.shouldShowRequestPermissionRationale()) {
            Toast.makeText(
                this,
                "You denied the permission for Shizuku. Please enable it in app.",
                Toast.LENGTH_LONG
            ).show()
            false
        } else {
            Shizuku.requestPermission(SHIZUKU_CODE)

            val result = shizukuPermissionFuture.get()
            shizukuPermissionFuture = CompletableFuture<Boolean>()

            result
        }

        return b
    }
}
