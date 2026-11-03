package com.example

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.data.ScrollSearchStateBus
import com.example.service.FloatingOverlayService
import com.example.service.InstagramAccessibilityService
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.theme.ScrollSearchTheme

class MainActivity : ComponentActivity() {

    private var hasOverlayPermission by mutableStateOf(false)
    private var hasAccessibilityPermission by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ScrollSearchTheme {
                DashboardScreen(
                    isOverlayPermissionGranted = hasOverlayPermission,
                    isAccessibilityServiceEnabled = hasAccessibilityPermission,
                    onRequestOverlayPermission = { requestOverlayPermission() },
                    onRequestAccessibilityPermission = { requestAccessibilityPermission() },
                    onLaunchInstagram = { launchInstagram() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
        // If overlay is enabled and permission is granted, auto-start overlay service
        if (hasOverlayPermission && ScrollSearchStateBus.isOverlayActive.value) {
            FloatingOverlayService.startService(this)
        }
    }

    private fun checkPermissions() {
        hasOverlayPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }

        hasAccessibilityPermission = isAccessibilityServiceRunning()
    }

    private fun isAccessibilityServiceRunning(): Boolean {
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager ?: return false
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        val expectedServiceName = "${packageName}/${InstagramAccessibilityService::class.java.name}"
        return enabledServices.any {
            it.id.equals(expectedServiceName, ignoreCase = true) ||
                    it.resolveInfo?.serviceInfo?.name?.contains("InstagramAccessibilityService") == true
        }
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
            Toast.makeText(this, "Ative a permissão de sobreposição para o ScrollSearch", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "Permissão de sobreposição já concedida!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestAccessibilityPermission() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
        Toast.makeText(
            this,
            "Localize 'ScrollSearch Instagram Detector' e ative nas Configurações de Acessibilidade",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun launchInstagram() {
        val pm = packageManager
        val intent = pm.getLaunchIntentForPackage("com.instagram.android")
        if (intent != null) {
            startActivity(intent)
        } else {
            // Fallback: open Instagram in browser
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/reels/"))
            startActivity(webIntent)
            Toast.makeText(this, "Instagram não instalado. Abrindo no navegador.", Toast.LENGTH_SHORT).show()
        }
    }
}
