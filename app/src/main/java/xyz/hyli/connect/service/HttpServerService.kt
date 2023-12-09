package xyz.hyli.connect.service

import android.R
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import xyz.hyli.connect.server.HTTP_PORT
import xyz.hyli.connect.server.HttpServer


class HttpServerService : Service() {
    private val TAG = "HttpServerService"
    private var httpServer: HttpServer? = null
    private var isRunning = false
    private var mNM: NotificationManager? = null
    private val NOTI_SERV_RUNNING= "Http Server is running."

    override fun onCreate() {
        super.onCreate()
        mNM = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        startServer()
    }

    override fun onBind(intent: Intent): IBinder {
        startServer()
        return Binder()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        stopServer()
        return super.onUnbind(intent)
    }

    private fun startServer() {
        if (isRunning.not()) {
            Log.i(TAG, "create http server: port= $HTTP_PORT")
            httpServer = HttpServer()
            httpServer?.start()
            isRunning = true
        }
    }
    private fun stopServer() {
        if (isRunning) {
            httpServer?.stop()
            httpServer = null
            isRunning = false
        }
    }
}