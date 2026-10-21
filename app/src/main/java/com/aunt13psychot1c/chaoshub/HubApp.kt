package com.aunt13psychot1c.chaoshub

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.aunt13psychot1c.chaoshub.model.ServiceStore

class HubApp : Application() {
    override fun onCreate() {
        super.onCreate()
        HubState.load(ServiceStore(this))
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(Notifier.CHANNEL_MESSAGES, "New messages", NotificationManager.IMPORTANCE_HIGH)
        )
        nm.createNotificationChannel(
            NotificationChannel(Notifier.CHANNEL_KEEP_ALIVE, "Keeping chats awake", NotificationManager.IMPORTANCE_MIN)
                .apply { setShowBadge(false) }
        )
    }
}
