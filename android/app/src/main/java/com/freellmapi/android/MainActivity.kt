package com.freellmapi.android

import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.freellmapi.android.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startServiceIfNeeded()
        } else {
            Toast.makeText(this, "Notification permission required", Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUi()
        checkPermissions()
    }
    
    private fun setupUi() {
        binding.btnStart.setOnClickListener {
            requestNotificationPermission()
        }
        
        binding.btnStop.setOnClickListener {
            stopService()
        }
        
        binding.btnOpenDashboard.setOnClickListener {
            openDashboard()
        }
        
        binding.btnCopyEndpoint.setOnClickListener {
            copyEndpoint()
        }
    }
    
    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                startServiceIfNeeded()
            } else {
                requestNotificationPermission()
            }
        } else {
            startServiceIfNeeded()
        }
    }
    
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        } else {
            startServiceIfNeeded()
        }
    }
    
    private fun startServiceIfNeeded() {
        val intent = Intent(this, FreeLLMApiService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        updateUi()
    }
    
    private fun stopService() {
        val intent = Intent(this, FreeLLMApiService::class.java)
        stopService(intent)
        updateUi()
        Toast.makeText(this, "Service stopped", Toast.LENGTH_SHORT).show()
    }
    
    private fun openDashboard() {
        val uri = Uri.Builder()
            .scheme("http")
            .authority("localhost:3001")
            .path("/")
            .build()
        val browserIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
        startActivity(browserIntent)
    }
    
    private fun copyEndpoint() {
        val endpoint = "http://localhost:3001/v1"
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("FreeLLMAPI Endpoint", endpoint)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Copied: $endpoint", Toast.LENGTH_SHORT).show()
    }
    
    private fun updateUi() {
        val isRunning = FreeLLMApiService.isRunning
        binding.tvStatus.text = if (isRunning) "● Running" else "○ Stopped"
        binding.tvStatus.setTextColor(if (isRunning) 0xFF4CAF50.toInt() else 0xFF9E9E9E.toInt())
        binding.btnStart.visibility = if (isRunning) View.GONE else View.VISIBLE
        binding.btnStop.visibility = if (isRunning) View.VISIBLE else View.GONE
    }
    
    override fun onResume() {
        super.onResume()
        updateUi()
    }
}