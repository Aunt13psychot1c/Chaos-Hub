package com.aunt13psychot1c.chaoshub

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.aunt13psychot1c.chaoshub.model.ChatService
import com.aunt13psychot1c.chaoshub.ui.MainActivity

/**
 * One notification per tab: a newer message replaces the older one instead of piling up, and
 * tapping it opens the hub on that tab.
 */
object Notifier {
    const val CHANNEL_MESSAGES = "messages"
    const val CHANNEL_KEEP_ALIVE = "keep_alive"
    const val EXTRA_SERVICE = "com.aunt13psychot1c.chaoshub.SERVICE"

    // Checked in allowed(), and a revoked permission is caught below.
    @SuppressLint("MissingPermission")
    fun show(context: Context, service: ChatService, title: String, body: String) {
        if (!allowed(context)) return
        val heading = if (title.isBlank() || title == service.name) "${service.emoji} ${service.name}"
                      else "${service.emoji} $title"
        val notification = NotificationCompat.Builder(context, CHANNEL_MESSAGES)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(heading)
            .setContentText(body.ifBlank { "New message on ${service.name}" })
            .setStyle(NotificationCompat.BigTextStyle().bigText(body.ifBlank { "New message on ${service.name}" }))
            .setSubText(service.name)
            .setColor(service.color.toInt())
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openTab(context, service.id))
            .build()
        try {
            NotificationManagerCompat.from(context).notify(idFor(service.id), notification)
        } catch (_: SecurityException) {
            // Permission was taken away between the check and the post.
        }
    }

    fun clear(context: Context, serviceId: String) {
        NotificationManagerCompat.from(context).cancel(idFor(serviceId))
    }

    fun openTab(context: Context, serviceId: String?): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(EXTRA_SERVICE, serviceId)
        return PendingIntent.getActivity(
            context, idFor(serviceId ?: ""), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun allowed(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    // 1 is the keep-alive notification; tabs hash to everything else.
    private fun idFor(serviceId: String) = 1000 + (serviceId.hashCode() and 0x7FFFFF)
}
