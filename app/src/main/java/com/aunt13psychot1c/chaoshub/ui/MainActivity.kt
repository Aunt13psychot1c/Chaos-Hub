package com.aunt13psychot1c.chaoshub.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.aunt13psychot1c.chaoshub.HubState
import com.aunt13psychot1c.chaoshub.Notifier
import com.aunt13psychot1c.chaoshub.service.KeepAliveService
import com.aunt13psychot1c.chaoshub.web.WebViewPool

class MainActivity : ComponentActivity() {

    private var pendingUpload: ValueCallback<Array<Uri>>? = null

    private val pickFiles = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        pendingUpload?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data))
        pendingUpload = null
    }

    private val askNotifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        openFromNotification(intent)

        WebViewPool.attachTo(this)
        WebViewPool.fileChooser = { callback, params ->
            pendingUpload?.onReceiveValue(null)
            pendingUpload = callback
            try {
                pickFiles.launch(params.createIntent())
                true
            } catch (_: Exception) {
                pendingUpload = null
                false
            }
        }
        WebViewPool.warmUp(this)

        if (Build.VERSION.SDK_INT >= 33 && !HubState.store.askedNotifications) {
            HubState.store.askedNotifications = true
            askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            ChaosHubTheme {
                HubScreen(
                    onLeave = { moveTaskToBack(true) },
                    onKeepRunningChanged = { on -> if (on) KeepAliveService.start(this) else KeepAliveService.stop(this) },
                    onBatterySettings = ::openBatterySettings
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        openFromNotification(intent)
    }

    override fun onStart() {
        super.onStart()
        HubState.onScreen = true
        HubState.activeId?.let { Notifier.clear(this, it) }
        if (HubState.store.keepRunning) KeepAliveService.start(this)
    }

    override fun onStop() {
        HubState.onScreen = false
        super.onStop()
    }

    override fun onDestroy() {
        WebViewPool.fileChooser = null
        WebViewPool.detach(applicationContext)
        pendingUpload?.onReceiveValue(null)
        pendingUpload = null
        super.onDestroy()
    }

    private fun openFromNotification(intent: Intent?) {
        val id = intent?.getStringExtra(Notifier.EXTRA_SERVICE) ?: return
        if (HubState.service(id)?.enabled == true) {
            HubState.open(id)
            Notifier.clear(this, id)
        }
    }

    // Sideloaded app, and staying awake is the whole point of the setting it sits next to.
    @SuppressLint("BatteryLife")
    private fun openBatterySettings() {
        val direct = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName"))
        try {
            startActivity(direct)
        } catch (_: Exception) {
            try { startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) } catch (_: Exception) { }
        }
    }
}
