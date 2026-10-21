package com.aunt13psychot1c.chaoshub.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.aunt13psychot1c.chaoshub.HubState
import com.aunt13psychot1c.chaoshub.Notifier
import com.aunt13psychot1c.chaoshub.R
import com.aunt13psychot1c.chaoshub.web.WebViewPool

/**
 * Keeps the app's process, and with it every chat page, running after you leave the hub, so
 * messages keep arriving and notifying. Android insists on a visible notification for this;
 * it sits in the quietest channel.
 */
class KeepAliveService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!HubState.store.keepRunning) {
            stopSelf()
            return START_NOT_STICKY
        }
        val notification = NotificationCompat.Builder(this, Notifier.CHANNEL_KEEP_ALIVE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Chaos Hub is keeping your chats awake 👑")
            .setContentText("Tap to open. Turn this off in the hub's ⚙️ settings.")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setContentIntent(Notifier.openTab(this, null))
            .build()
        try {
            ServiceCompat.startForeground(
                this, NOTIFICATION_ID, notification,
                if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
            )
        } catch (e: Exception) {
            Log.w("KeepAlive", "couldn't stay in the foreground", e)
            stopSelf()
            return START_NOT_STICKY
        }
        // Brought back by Android after it killed the process: reopen the pages.
        WebViewPool.warmUp(this)
        return START_STICKY
    }

    companion object {
        private const val NOTIFICATION_ID = 1

        fun start(context: Context) {
            try {
                ContextCompat.startForegroundService(context, Intent(context, KeepAliveService::class.java))
            } catch (e: Exception) {
                // Android refuses when the app isn't on screen; it'll start next time it is.
                Log.w("KeepAlive", "start refused", e)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, KeepAliveService::class.java))
        }
    }
}
