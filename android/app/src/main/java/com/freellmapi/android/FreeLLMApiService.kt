package com.freellmapi.android

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import org.nodejs.mobile.NodeJS
import org.nodejs.mobile.NodeAppListener
import java.io.File

class FreeLLMApiService : Service(), NodeAppListener {
    companion object {
        private const val TAG = "FreeLLMApiService"
        private const val CHANNEL_ID = "freellmapi_channel"
        private const val NOTIFICATION_ID = 1001
        
        @Volatile var isRunning = false
    }
    
    private var nodeApp: NodeJS? = null
    
    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Service onCreate")
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "Service onStartCommand")
        
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
        
        // Start Node.js in background thread
        Thread {
            try {
                startNodeJs()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start Node.js", e)
                stopSelf()
            }
        }.start()
        
        return START_STICKY
    }
    
    private fun startNodeJs() {
        val nodeDir = filesDir.absolutePath + "/nodejs-project"
        val mainScript = "$nodeDir/index.js"
        
        Log.i(TAG, "Starting Node.js from: $mainScript")
        
        nodeApp = NodeJS(this, this)
        nodeApp?.setWorkingDirectory(nodeDir)
        nodeApp?.start(listOf("node", mainScript))
        
        isRunning = true
        updateNotification("FreeLLMAPI", "Router started on :3001")
    }
    
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.channel_description)
        }
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
    
    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
    
    private fun updateNotification(title: String, content: String) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val notification = createNotification().apply {
            this.contentTitle = title
            this.contentText = content
        }
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "Service onDestroy")
        nodeApp?.stop()
        isRunning = false
    }
    
    // NodeAppListener callbacks
    override fun onNodeAppStarted() {
        Log.i(TAG, "Node.js started successfully")
        updateNotification("FreeLLMAPI", "Router active")
    }
    
    override fun onNodeAppStopped(code: Int, reason: String?) {
        Log.i(TAG, "Node.js stopped: code=$code, reason=$reason")
        isRunning = false
    }
    
    override fun onLogReceived(tag: String, message: String) {
        Log.d(TAG, "[$tag] $message")
    }
    
    override fun onConsoleMessage(message: String, lineNumber: Int, sourceID: String) {
        Log.d(TAG, "Console: $message")
    }
}