package com.freellmapi.android

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.net.Inet4Address
import java.net.NetworkInterface

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var tvLocalEndpoint: TextView
    private lateinit var tvLanEndpoint: TextView
    private lateinit var btnToggleService: Button
    private lateinit var btnOpenDashboard: Button
    private lateinit var btnCopyEndpoint: Button
    private lateinit var btnBatteryOpt: Button
    private lateinit var webViewDashboard: WebView

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startRouterService()
        } else {
            Toast.makeText(this, "Notification permission is required for the foreground service", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupListeners()
        updateUiState()

        // Listen for service changes
        FreeLLMApiService.onStatusChanged = { isRunning ->
            runOnUiThread {
                updateUiState()
            }
        }

        // Auto-start router service on launch
        checkPermissionsAndStart()
    }

    override fun onResume() {
        super.onResume()
        updateUiState()
    }

    private fun initViews() {
        tvStatus = findViewById(R.id.tvStatus)
        tvLocalEndpoint = findViewById(R.id.tvLocalEndpoint)
        tvLanEndpoint = findViewById(R.id.tvLanEndpoint)
        btnToggleService = findViewById(R.id.btnToggleService)
        btnOpenDashboard = findViewById(R.id.btnOpenDashboard)
        btnCopyEndpoint = findViewById(R.id.btnCopyEndpoint)
        btnBatteryOpt = findViewById(R.id.btnBatteryOpt)
        webViewDashboard = findViewById(R.id.webViewDashboard)

        webViewDashboard.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
        }
        webViewDashboard.webViewClient = WebViewClient()
        webViewDashboard.webChromeClient = WebChromeClient()
    }

    private fun setupListeners() {
        btnToggleService.setOnClickListener {
            if (FreeLLMApiService.isRunning) {
                stopRouterService()
            } else {
                checkPermissionsAndStart()
            }
        }

        btnOpenDashboard.setOnClickListener {
            val url = "http://127.0.0.1:${FreeLLMApiService.serverPort}"
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Opening dashboard...", Toast.LENGTH_SHORT).show()
                webViewDashboard.loadUrl(url)
                webViewDashboard.visibility = View.VISIBLE
            }
        }

        btnCopyEndpoint.setOnClickListener {
            val endpoint = "http://localhost:${FreeLLMApiService.serverPort}/v1"
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("FreeLLMAPI Endpoint", endpoint)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Copied: $endpoint", Toast.LENGTH_SHORT).show()
        }

        btnBatteryOpt.setOnClickListener {
            requestIgnoreBatteryOptimization()
        }
    }

    private fun checkPermissionsAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                startRouterService()
            } else {
                requestNotificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            startRouterService()
        }
    }

    private fun startRouterService() {
        val intent = Intent(this, FreeLLMApiService::class.java).apply {
            action = FreeLLMApiService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        updateUiState()
    }

    private fun stopRouterService() {
        val intent = Intent(this, FreeLLMApiService::class.java).apply {
            action = FreeLLMApiService.ACTION_STOP
        }
        startService(intent)
        updateUiState()
    }

    private fun requestIgnoreBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    startActivity(intent)
                }
            } else {
                Toast.makeText(this, "Battery optimization already ignored (24/7 Keep-Alive Active)", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateUiState() {
        val running = FreeLLMApiService.isRunning
        val port = FreeLLMApiService.serverPort
        val lanIp = getLocalIpAddress()

        if (running) {
            tvStatus.text = "● RUNNING (Port $port)"
            tvStatus.setTextColor(0xFF4CAF50.toInt())
            btnToggleService.text = "STOP ROUTER"
            btnToggleService.setBackgroundColor(0xFFB3261E.toInt())

            tvLocalEndpoint.text = "Localhost: http://127.0.0.1:$port/v1"
            tvLanEndpoint.text = if (lanIp != null) "LAN/WiFi: http://$lanIp:$port/v1" else "LAN: Connecting..."

            webViewDashboard.visibility = View.VISIBLE
            webViewDashboard.loadUrl("http://127.0.0.1:$port")
        } else {
            tvStatus.text = "○ STOPPED"
            tvStatus.setTextColor(0xFF9E9E9E.toInt())
            btnToggleService.text = "START ROUTER"
            btnToggleService.setBackgroundColor(0xFF6750A4.toInt())

            tvLocalEndpoint.text = "Localhost: http://127.0.0.1:$port/v1"
            tvLanEndpoint.text = if (lanIp != null) "LAN/WiFi: http://$lanIp:$port/v1" else "LAN: Offline"
        }
    }

    private fun getLocalIpAddress(): String? {
        try {
            val en = NetworkInterface.getNetworkInterfaces()
            while (en.hasMoreElements()) {
                val intf = en.nextElement()
                val enumIpAddr = intf.inetAddresses
                while (enumIpAddr.hasMoreElements()) {
                    val inetAddress = enumIpAddr.nextElement()
                    if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                        return inetAddress.hostAddress
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}