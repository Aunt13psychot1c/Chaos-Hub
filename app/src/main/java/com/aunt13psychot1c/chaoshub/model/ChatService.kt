package com.aunt13psychot1c.chaoshub.model

import java.net.URI

/**
 * One tab in the hub: a chat website opened in its own page that stays signed in.
 *
 * [domains] are the sites the page may move between (logins, redirects, CDNs). Links anywhere
 * else open in the phone's browser, so a stray link never replaces the chat.
 */
data class ChatService(
    val id: String,
    val name: String,
    val url: String,
    val emoji: String,
    val color: Long,
    /** Ask for the computer version of the site; some (WhatsApp, TikTok) refuse phones. */
    val desktop: Boolean = false,
    val enabled: Boolean = true,
    val builtIn: Boolean = false,
    val domains: List<String> = listOfNotNull(baseDomain(url))
)

object BuiltIns {
    val all = listOf(
        ChatService(
            id = "discord", name = "Discord", url = "https://discord.com/app", emoji = "🎮",
            color = 0xFF5865F2, desktop = true, builtIn = true,
            domains = listOf("discord.com", "discordapp.com", "discord.gg", "hcaptcha.com")
        ),
        ChatService(
            id = "telegram", name = "Telegram", url = "https://web.telegram.org/k/", emoji = "✈️",
            color = 0xFF2AABEE, builtIn = true,
            domains = listOf("telegram.org", "t.me")
        ),
        ChatService(
            id = "messenger", name = "Messenger", url = "https://www.facebook.com/messages/", emoji = "💬",
            color = 0xFFA033FF, builtIn = true,
            domains = listOf("facebook.com", "messenger.com", "fb.com", "fbcdn.net")
        ),
        ChatService(
            id = "instagram", name = "Instagram", url = "https://www.instagram.com/direct/inbox/", emoji = "📸",
            color = 0xFFE1306C, builtIn = true,
            domains = listOf("instagram.com", "cdninstagram.com", "facebook.com")
        ),
        ChatService(
            id = "whatsapp", name = "WhatsApp", url = "https://web.whatsapp.com/", emoji = "📞",
            color = 0xFF25D366, desktop = true, builtIn = true,
            domains = listOf("whatsapp.com", "whatsapp.net")
        ),
        ChatService(
            id = "tiktok", name = "TikTok", url = "https://www.tiktok.com/messages", emoji = "🎵",
            color = 0xFF25F4EE, desktop = true, builtIn = true,
            domains = listOf("tiktok.com", "tiktokv.com", "ttwstatic.com")
        ),
        ChatService(
            id = "x", name = "X", url = "https://x.com/messages", emoji = "✖️",
            color = 0xFF3A3A3A, builtIn = true,
            domains = listOf("x.com", "twitter.com", "twimg.com")
        ),
        ChatService(
            id = "irc", name = "IRC", url = "https://web.libera.chat/", emoji = "📡",
            color = 0xFFFF8A00, builtIn = true,
            domains = listOf("libera.chat")
        )
    )
}

/** "https://www.chat.example.com/x" -> "chat.example.com"; null if it isn't a web address. */
fun baseDomain(url: String): String? = try {
    URI(url).host?.lowercase()?.removePrefix("www.")?.takeIf { it.contains('.') }
} catch (_: Exception) {
    null
}

/** True when [host] is one of [domains] or a subdomain of one. */
fun isInside(host: String?, domains: List<String>): Boolean {
    val h = host?.lowercase() ?: return false
    return domains.any { d -> h == d || h.endsWith(".$d") }
}

/** Accepts what people actually type: "kiwiirc.com" becomes "https://kiwiirc.com". */
fun normalizeUrl(input: String): String? {
    val trimmed = input.trim()
    if (trimmed.isEmpty() || trimmed.any { it.isWhitespace() }) return null
    val withScheme = if (trimmed.contains("://")) trimmed else "https://$trimmed"
    if (!withScheme.startsWith("https://") && !withScheme.startsWith("http://")) return null
    return withScheme.takeIf { baseDomain(it) != null }
}

/**
 * Most chat sites put the unread count in the tab title, e.g. "(3) Discord | #general" or
 * "(12) WhatsApp". Returns 0 when there isn't one.
 */
fun parseUnread(title: String?): Int {
    if (title.isNullOrBlank()) return 0
    val match = UNREAD.find(title) ?: return 0
    return match.groupValues[1].toIntOrNull() ?: 0
}

private val UNREAD = Regex("""\((\d{1,5})\+?\)""")
