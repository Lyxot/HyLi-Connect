package xyz.hyli.connect.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import xyz.hyli.connect.socket.SocketServer

class SocketServerService : Service() {
    private val TAG = "SocketServerService"
    private var isRunning = false

    override fun onCreate() {
        super.onCreate()
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
        if ( isRunning.not() ) {
            SocketServer.start()
            isRunning = true
        }
    }
    private fun stopServer() {
        if ( isRunning ) {
            SocketServer.stop()
            isRunning = false
        }
    }
}