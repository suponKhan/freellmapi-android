package com.freellmapi.android

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.text.format.Formatter
import android.util.Log
import androidx.core.app.NotificationCompat
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.Inet4Address
import java.net.NetworkInterface

class FreeLLMApiService : Service() {

    companion object {
        private const val TAG = "FreeLLMAPIService"
        private const val CHANNEL_ID = "freellmapi_service_channel"
        private const val NOTIFICATION_ID = 2026
        const val ACTION_START = "com.freellmapi.android.ACTION_START"
        const val ACTION_STOP = "com.freellmapi.android.ACTION_STOP"

        @Volatile var isRunning = false
            private set
        
        @Volatile var serverPort = 3001
        
        var onStatusChanged: ((Boolean) -> Unit)? = null

        init {
            try {
                System.loadLibrary("native-lib")
                System.loadLibrary("node")
                Log.i(TAG, "Native libraries loaded successfully")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "Failed to load native libraries: ${e.message}")
            }
        }
    }

    private var wakeLock: PowerManager.WakeLock? = null
    private var nodeThread: Thread? = null

    // Native function defined in native-lib.cpp
    external fun startNodeWithArguments(arguments: Array<String>): Int

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        if (action == ACTION_STOP) {
            stopNodeService()
            stopSelf()
            return START_NOT_STICKY
        }

        if (!isRunning) {
            startForeground(NOTIFICATION_ID, buildNotification("FreeLLMAPI starting...", "Initializing Node.js runtime"))
            acquireWakeLock()
            startNodeServer()
        }

        return START_STICKY
    }

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "FreeLLMAPI::WakeLock").apply {
                acquire()
            }
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        wakeLock = null
    }

    private fun startNodeServer() {
        nodeThread = Thread {
            try {
                val nodeDir = File(filesDir, "nodejs-project")
                if (shouldExtractAssets(nodeDir)) {
                    Log.i(TAG, "Extracting nodejs-project from assets...")
                    deleteRecursively(nodeDir)
                    nodeDir.mkdirs()
                    copyAssetFolder(assets, "nodejs-project", nodeDir.absolutePath)
                    saveLastUpdateTime()
                }

                val mainJs = File(nodeDir, "index.js")
                Log.i(TAG, "Starting Node.js router from: ${mainJs.absolutePath}")

                isRunning = true
                updateNotification("FreeLLMAPI Active", "Listening on :$serverPort (Local/WiFi/Mobile Data)")
                onStatusChanged?.invoke(true)

                // Run node engine
                startNodeWithArguments(arrayOf("node", mainJs.absolutePath))

            } catch (e: Exception) {
                Log.e(TAG, "Node.js engine error", e)
            } finally {
                isRunning = false
                onStatusChanged?.invoke(false)
                releaseWakeLock()
            }
        }.apply {
            name = "FreeLLMAPI-NodeThread"
            start()
        }
    }

    private fun stopNodeService() {
        isRunning = false
        releaseWakeLock()
        onStatusChanged?.invoke(false)
    }

    private fun shouldExtractAssets(targetDir: File): Boolean {
        if (!targetDir.exists() || !File(targetDir, "index.js").exists()) {
            return true
        }
        val prefs = getSharedPreferences("freellmapi_prefs", Context.MODE_PRIVATE)
        val savedTime = prefs.getLong("apk_update_time", 0L)
        val currentUpdateTime = getApkUpdateTime()
        return currentUpdateTime != savedTime
    }

    private fun getApkUpdateTime(): Long {
        return try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            packageInfo.lastUpdateTime
        } catch (e: PackageManager.NameNotFoundException) {
            0L
        }
    }

    private fun saveLastUpdateTime() {
        val prefs = getSharedPreferences("freellmapi_prefs", Context.MODE_PRIVATE)
        prefs.edit().putLong("apk_update_time", getApkUpdateTime()).apply()
    }

    private fun copyAssetFolder(assetManager: android.content.res.AssetManager, fromAssetPath: String, toPath: String): Boolean {
        return try {
            val files = assetManager.list(fromAssetPath) ?: return false
            File(toPath).mkdirs()
            var res = true
            for (file in files) {
                val fromChild = if (fromAssetPath.isEmpty()) file else "$fromAssetPath/$file"
                val toChild = "$toPath/$file"
                val subFiles = assetManager.list(fromChild)
                if (subFiles != null && subFiles.isNotEmpty()) {
                    res = res and copyAssetFolder(assetManager, fromChild, toChild)
                } else {
                    res = res and copyAssetFile(assetManager, fromChild, toChild)
                }
            }
            res
        } catch (e: Exception) {
            Log.e(TAG, "copyAssetFolder failed", e)
            false
        }
    }

    private fun copyAssetFile(assetManager: android.content.res.AssetManager, fromAssetPath: String, toPath: String): Boolean {
        var inStream: InputStream? = null
        var outStream: OutputStream? = null
        return try {
            inStream = assetManager.open(fromAssetPath)
            outStream = FileOutputStream(toPath)
            val buffer = ByteArray(8192)
            var read: Int
            while (inStream.read(buffer).also { read = it } != -1) {
                outStream.write(buffer, 0, read)
            }
            outStream.flush()
            true
        } catch (e: IOException) {
            Log.e(TAG, "copyAssetFile failed for $fromAssetPath", e)
            false
        } finally {
            inStream?.close()
            outStream?.close()
        }
    }

    private fun deleteRecursively(fileOrDirectory: File) {
        if (fileOrDirectory.isDirectory) {
            fileOrDirectory.listFiles()?.forEach { deleteRecursively(it) }
        }
        fileOrDirectory.delete()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "FreeLLMAPI Background Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps FreeLLMAPI router running continuously"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(title: String, content: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, FreeLLMApiService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_delete, "Stop Router", stopPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(title: String, content: String) {
        val notification = buildNotification(title, content)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopNodeService()
    }
}