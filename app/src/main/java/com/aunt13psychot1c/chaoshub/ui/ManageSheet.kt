package com.aunt13psychot1c.chaoshub.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aunt13psychot1c.chaoshub.HubState
import com.aunt13psychot1c.chaoshub.model.ChatService
import com.aunt13psychot1c.chaoshub.model.baseDomain
import com.aunt13psychot1c.chaoshub.model.normalizeUrl
import com.aunt13psychot1c.chaoshub.web.WebViewPool
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageSheet(
    onDismiss: () -> Unit,
    onKeepRunningChanged: (Boolean) -> Unit,
    onBatterySettings: () -> Unit
) {
    val context = LocalContext.current
    var keepRunning by remember { mutableStateOf(HubState.store.keepRunning) }

    fun apply(updated: List<ChatService>) {
        HubState.update(updated)
        WebViewPool.prune(updated.filter { it.enabled }.map { it.id }.toSet())
        WebViewPool.warmUp(context)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = HubPanel
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Heading("👑 Your chats")
            HubState.services.forEach { s ->
                ServiceRow(
                    service = s,
                    onToggle = { on -> apply(HubState.services.map { if (it.id == s.id) it.copy(enabled = on) else it }) },
                    onRemove = if (s.builtIn) null else { { apply(HubState.services.filter { it.id != s.id }) } }
                )
            }

            Divider()
            Heading("🌐 Add any website")
            AddWebsite(onAdd = { apply(HubState.services + it) })

            Divider()
            Heading("🔔 Notifications")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Keep chats awake in the background", fontSize = 15.sp, color = Color.White)
                    Text(
                        "Needed for new-message alerts. Uses some battery.",
                        fontSize = 13.sp, color = Color.White.copy(alpha = 0.6f)
                    )
                }
                Switch(checked = keepRunning, onCheckedChange = {
                    keepRunning = it
                    HubState.store.keepRunning = it
                    onKeepRunningChanged(it)
                })
            }
            TextButton(onClick = onBatterySettings) {
                Text("🔋 Let Chaos Hub ignore battery saving")
            }
            Text(
                "If alerts stop when your phone sleeps, set Chaos Hub's battery use to Unrestricted.",
                fontSize = 13.sp, color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun ServiceRow(service: ChatService, onToggle: (Boolean) -> Unit, onRemove: (() -> Unit)?) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(service.color)),
            contentAlignment = Alignment.Center
        ) { Text(service.emoji, fontSize = 16.sp) }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(service.name, fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
            Text(
                baseDomain(service.url) ?: service.url,
                fontSize = 12.sp, color = Color.White.copy(alpha = 0.55f)
            )
        }
        if (onRemove != null) {
            TextButton(onClick = onRemove) { Text("🗑️") }
        }
        Switch(checked = service.enabled, onCheckedChange = onToggle)
    }
}

@Composable
private fun AddWebsite(onAdd: (ChatService) -> Unit) {
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var desktop by remember { mutableStateOf(false) }
    var problem by remember { mutableStateOf<String?>(null) }

    OutlinedTextField(
        value = name, onValueChange = { name = it },
        label = { Text("Name (like Slack)") }, singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = address, onValueChange = { address = it; problem = null },
        label = { Text("Web address (like app.slack.com)") }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
        isError = problem != null,
        supportingText = problem?.let { { Text(it) } },
        modifier = Modifier.fillMaxWidth()
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = desktop, onCheckedChange = { desktop = it })
        Text("Ask for the computer version", fontSize = 14.sp, color = Color.White)
    }
    Button(
        onClick = {
            val url = normalizeUrl(address)
            if (url == null) {
                problem = "That doesn't look like a web address"
                return@Button
            }
            onAdd(
                ChatService(
                    id = "web-" + UUID.randomUUID().toString().take(8),
                    name = name.trim().ifEmpty { baseDomain(url) ?: "Website" },
                    url = url,
                    emoji = "🌐",
                    color = PALETTE[(url.hashCode() and 0x7FFFFFFF) % PALETTE.size],
                    desktop = desktop
                )
            )
            name = ""; address = ""; desktop = false
        },
        modifier = Modifier.fillMaxWidth()
    ) { Text("✨ Add it") }
}

@Composable
private fun Heading(text: String) {
    Text(
        text, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White,
        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
    )
}

@Composable
private fun Divider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 16.dp),
        color = Color.White.copy(alpha = 0.12f)
    )
}

private val PALETTE = listOf(0xFF6B2BD9, 0xFFFF4FA3, 0xFF00B3A4, 0xFFFF8A00, 0xFF3D7BFF, 0xFFD6336C)
