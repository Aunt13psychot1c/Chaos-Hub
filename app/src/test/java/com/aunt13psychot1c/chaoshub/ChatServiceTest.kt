package com.aunt13psychot1c.chaoshub

import com.aunt13psychot1c.chaoshub.model.BuiltIns
import com.aunt13psychot1c.chaoshub.model.baseDomain
import com.aunt13psychot1c.chaoshub.model.isInside
import com.aunt13psychot1c.chaoshub.model.normalizeUrl
import com.aunt13psychot1c.chaoshub.model.parseUnread
import com.aunt13psychot1c.chaoshub.web.UserAgents
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatServiceTest {

    @Test fun unreadComesFromTheTitle() {
        assertEquals(3, parseUnread("(3) Discord | #general"))
        assertEquals(12, parseUnread("(12) WhatsApp"))
        assertEquals(99, parseUnread("Messenger (99+)"))
        assertEquals(0, parseUnread("Telegram Web"))
        assertEquals(0, parseUnread(null))
    }

    @Test fun pagesStayOnTheirOwnSites() {
        val discord = BuiltIns.all.first { it.id == "discord" }.domains
        assertTrue(isInside("discord.com", discord))
        assertTrue(isInside("canary.discord.com", discord))
        assertFalse(isInside("notdiscord.com", discord))
        assertFalse(isInside("discord.com.evil.example", discord))
        assertFalse(isInside(null, discord))
    }

    @Test fun typedAddressesAreCleanedUp() {
        assertEquals("https://app.slack.com", normalizeUrl(" app.slack.com "))
        assertEquals("http://example.org/chat", normalizeUrl("http://example.org/chat"))
        assertNull(normalizeUrl("not a site"))
        assertNull(normalizeUrl("javascript:alert(1)"))
        assertNull(normalizeUrl(""))
        assertEquals("kiwiirc.com", baseDomain("https://www.kiwiirc.com/nextclient/"))
    }

    @Test fun everyBuiltInIsUniqueAndReachable() {
        assertEquals(BuiltIns.all.size, BuiltIns.all.map { it.id }.toSet().size)
        BuiltIns.all.forEach { s -> assertTrue(s.name, isInside(baseDomain(s.url), s.domains)) }
    }

    @Test fun browserDisguisesKeepTheChromeVersion() {
        val wv = "Mozilla/5.0 (Linux; Android 15; Pixel 9; wv) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Version/4.0 Chrome/141.0.7390.55 Mobile Safari/537.36"
        val mobile = UserAgents.mobile(wv)
        assertFalse(mobile.contains("; wv"))
        assertFalse(mobile.contains("Version/4.0"))
        assertTrue(UserAgents.desktop(wv).contains("Chrome/141.0.7390.55"))
        assertFalse(UserAgents.desktop(wv).contains("Android"))
    }
}
