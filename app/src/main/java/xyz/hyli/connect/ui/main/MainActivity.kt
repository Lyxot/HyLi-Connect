package xyz.hyli.connect.ui.main

import xyz.hyli.connect.ui.applist.AppListAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import rikka.shizuku.Shizuku
import xyz.hyli.connect.R
import xyz.hyli.connect.utils.PackageUtils
import xyz.hyli.connect.utils.ShellUtils
import xyz.hyli.connect.utils.VirtualDisplayUtils
import java.util.concurrent.CompletableFuture


class MainActivity : ComponentActivity() {
    private val SHIZUKU_CODE = 0xCA07A
    private var shizukuPermissionFuture = CompletableFuture<Boolean>()
    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        val applist_button = findViewById<Button>(R.id.applist_button)
        applist_button.setOnClickListener {
            val intent = Intent(this, AppListActivity::class.java)
            startActivity(intent)
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
