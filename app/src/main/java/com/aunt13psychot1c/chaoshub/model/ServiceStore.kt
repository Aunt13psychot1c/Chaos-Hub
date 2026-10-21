package com.aunt13psychot1c.chaoshub.model

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Remembers which tabs exist, their order and which are switched on. */
class ServiceStore(context: Context) {

    private val prefs = context.getSharedPreferences("hub", Context.MODE_PRIVATE)

    fun load(): List<ChatService> {
        val raw = prefs.getString(KEY_SERVICES, null) ?: return BuiltIns.all
        val saved = try {
            val array = JSONArray(raw)
            List(array.length()) { i -> fromJson(array.getJSONObject(i)) }.filterNotNull()
        } catch (_: Exception) {
            return BuiltIns.all
        }
        // Built-ins added in a later version show up for people who already have a saved list.
        val missing = BuiltIns.all.filter { b -> saved.none { it.id == b.id } }
        return saved + missing
    }

    fun save(services: List<ChatService>) {
        val array = JSONArray()
        services.forEach { array.put(toJson(it)) }
        prefs.edit().putString(KEY_SERVICES, array.toString()).apply()
    }

    var keepRunning: Boolean
        get() = prefs.getBoolean(KEY_KEEP_RUNNING, true)
        set(value) = prefs.edit().putBoolean(KEY_KEEP_RUNNING, value).apply()

    var lastOpened: String?
        get() = prefs.getString(KEY_LAST, null)
        set(value) = prefs.edit().putString(KEY_LAST, value).apply()

    var askedNotifications: Boolean
        get() = prefs.getBoolean(KEY_ASKED, false)
        set(value) = prefs.edit().putBoolean(KEY_ASKED, value).apply()

    private fun toJson(s: ChatService): JSONObject {
        val o = JSONObject().put("id", s.id).put("enabled", s.enabled)
        // Built-ins keep their details in code, so fixes to their addresses reach everyone.
        if (!s.builtIn) {
            o.put("name", s.name).put("url", s.url).put("emoji", s.emoji)
                .put("color", s.color).put("desktop", s.desktop)
        }
        return o
    }

    private fun fromJson(o: JSONObject): ChatService? {
        val id = o.optString("id").ifEmpty { return null }
        val enabled = o.optBoolean("enabled", true)
        BuiltIns.all.firstOrNull { it.id == id }?.let { return it.copy(enabled = enabled) }
        val url = o.optString("url").ifEmpty { return null }
        return ChatService(
            id = id,
            name = o.optString("name", "Website"),
            url = url,
            emoji = o.optString("emoji", "🌐"),
            color = o.optLong("color", 0xFF6B2BD9),
            desktop = o.optBoolean("desktop", false),
            enabled = enabled
        )
    }

    private companion object {
        const val KEY_SERVICES = "services"
        const val KEY_KEEP_RUNNING = "keep_running"
        const val KEY_LAST = "last_opened"
        const val KEY_ASKED = "asked_notifications"
    }
}
