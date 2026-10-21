package com.aunt13psychot1c.chaoshub

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aunt13psychot1c.chaoshub.model.ChatService
import com.aunt13psychot1c.chaoshub.model.ServiceStore

/**
 * App-wide state. Lives as long as the process, like the chat pages themselves, so the screen,
 * the web pages and the background service all see the same tabs and unread counts.
 * Only touched from the main thread.
 */
object HubState {
    lateinit var store: ServiceStore
        private set

    val services = mutableStateListOf<ChatService>()

    /** Unread count per tab. Missing means the page hasn't reported one since it last loaded. */
    val unread = mutableStateMapOf<String, Int>()

    var activeId by mutableStateOf<String?>(null)

    /** True while the hub is on screen; notifications for the tab you're looking at are skipped. */
    var onScreen = false

    fun load(store: ServiceStore) {
        this.store = store
        services.clear()
        services.addAll(store.load())
        activeId = store.lastOpened?.takeIf { id -> services.any { it.id == id && it.enabled } }
            ?: services.firstOrNull { it.enabled }?.id
    }

    fun service(id: String?): ChatService? = services.firstOrNull { it.id == id }

    fun update(services: List<ChatService>) {
        this.services.clear()
        this.services.addAll(services)
        store.save(services)
        if (service(activeId)?.enabled != true) activeId = services.firstOrNull { it.enabled }?.id
    }

    fun open(id: String) {
        activeId = id
        store.lastOpened = id
    }

    fun isWatching(id: String) = onScreen && activeId == id
}
