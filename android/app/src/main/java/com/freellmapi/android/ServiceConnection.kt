package com.freellmapi.android

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import android.widget.Toast

class ServiceConnection : ServiceConnection {
    companion object {
        private const val TAG = "ServiceConnection"
        var isConnected = false
    }
    
    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        Log.i(TAG, "Service connected")
        isConnected = true
    }
    
    override fun onServiceDisconnected(name: ComponentName?) {
        Log.i(TAG, "Service disconnected")
        isConnected = false
    }
}