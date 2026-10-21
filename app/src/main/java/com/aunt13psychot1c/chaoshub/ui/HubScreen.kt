package com.aunt13psychot1c.chaoshub.ui

import android.content.MutableContextWrapper
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.aunt13psychot1c.chaoshub.HubState
import com.aunt13psychot1c.chaoshub.Notifier
import com.aunt13psychot1c.chaoshub.model.ChatService
import com.aunt13psychot1c.chaoshub.web.WebViewPool

@Composable
fun HubScreen(
    onLeave: () -> Unit,
    onKeepRunningChanged: (Boolean) -> Unit,
    onBatterySettings: () -> Unit
) {
    var managing by remember { mutableStateOf(false) }
    val active = HubState.service(HubState.activeId)?.takeIf { it.enabled }

    // Back goes back inside the chat first; at the start of it, the hub steps aside but keeps
    // running instead of closing every page.
    BackHandler {
        val page = WebViewPool.peek(active?.id)
        if (page != null && page.canGoBack()) page.goBack() else onLeave()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HubNight)
            // Includes the keyboard, so the box you're typing in is never hidden behind it.
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (active == null) {
                EmptyHub(onManage = { managing = true })
            } else {
                key(active.id) { ChatPage(active) }
            }
        }
        ServiceBar(
            services = HubState.services.filter { it.enabled },
            activeId = active?.id,
            onManage = { managing = true }
        )
    }

    if (managing) {
        ManageSheet(
            onDismiss = { managing = false },
            onKeepRunningChanged = onKeepRunningChanged,
            onBatterySettings = onBatterySettings
        )
    }
}

@Composable
private fun ChatPage(service: ChatService) {
    val screen = LocalContext.current
    AndroidView(
        factory = { FrameLayout(it) },
        update = { frame ->
            val page = WebViewPool.get(screen, service)
            (page.context as? MutableContextWrapper)?.baseContext = screen
            if (page.parent !== frame) {
                (page.parent as? ViewGroup)?.removeView(page)
                frame.removeAllViews()
                frame.addView(page, FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun EmptyHub(onManage: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("👑✨", fontSize = 48.sp)
        Text(
            "No chats switched on yet",
            fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White,
            modifier = Modifier.padding(top = 12.dp)
        )
        Text(
            "Tap ⚙️ to pick your chats or add any website.",
            fontSize = 15.sp, color = Color.White.copy(alpha = 0.7f), textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp).clickable(onClick = onManage)
        )
    }
}

@Composable
private fun ServiceBar(services: List<ChatService>, activeId: String?, onManage: () -> Unit) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.horizontalGradient(listOf(HubPanel, Color(0xFF2E1848), HubPanel)))
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        services.forEach { s ->
            ServiceChip(
                service = s,
                unread = HubState.unread[s.id] ?: 0,
                selected = s.id == activeId,
                onClick = {
                    if (s.id == activeId) {
                        // Tapping the tab you're on reloads it, for when a page gets stuck.
                        WebViewPool.peek(s.id)?.reload()
                    } else {
                        HubState.open(s.id)
                    }
                    Notifier.clear(context, s.id)
                }
            )
        }
        Tab(label = "Settings", onClick = onManage) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) { Text("⚙️", fontSize = 20.sp) }
        }
    }
}

@Composable
private fun ServiceChip(service: ChatService, unread: Int, selected: Boolean, onClick: () -> Unit) {
    Tab(label = service.name, selected = selected, onClick = onClick) {
        Box {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .then(if (selected) Modifier.border(2.dp, HubPink, CircleShape) else Modifier)
                    .padding(if (selected) 3.dp else 0.dp)
                    .clip(CircleShape)
                    .background(Color(service.color)),
                contentAlignment = Alignment.Center
            ) { Text(service.emoji, fontSize = 20.sp) }
            if (unread > 0) {
                Text(
                    text = if (unread > 99) "99+" else unread.toString(),
                    fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-2).dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFF2D55))
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                )
            }
        }
    }
}

@Composable
private fun Tab(label: String, selected: Boolean = false, onClick: () -> Unit, icon: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .widthIn(min = 52.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        icon()
        Text(
            label,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = if (selected) HubPink else Color.White.copy(alpha = 0.75f),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(top = 2.dp).widthIn(max = 60.dp)
        )
    }
}
