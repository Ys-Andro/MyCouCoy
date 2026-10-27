package com.coucou.android

import com.coucou.android.core.ChatRouting
import com.coucou.android.core.ChatRouting.Destination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatRoutingTest {
    private val phone = "phone/gemini"
    private val desktop = listOf("anthropic/claude", "openai/gpt")

    @Test fun phoneWorksWithoutAnyDesktopConnection() {
        assertEquals(phone, ChatRouting.selected(null, phone, emptyList(), desktopReady = false))
        assertEquals(Destination.PHONE, ChatRouting.destination(phone, phone, emptyList(), desktopReady = false))
    }

    @Test fun disconnectedDesktopFallsBackToPhoneWithoutLosingPreference() {
        val saved = "openai/gpt"
        assertEquals(phone, ChatRouting.selected(saved, phone, desktop, desktopReady = false))
        assertEquals(Destination.PHONE, ChatRouting.destination(phone, phone, desktop, desktopReady = false))
        assertEquals(saved, ChatRouting.selected(saved, phone, desktop, desktopReady = true))
        assertEquals(Destination.DESKTOP, ChatRouting.destination(saved, phone, desktop, desktopReady = true))
    }

    @Test fun explicitPhoneChoiceStaysOnPhoneWhenDesktopReconnects() {
        assertEquals(phone, ChatRouting.selected(phone, phone, desktop, desktopReady = true))
    }

    @Test fun noPhoneNeverRoutesToOfflineOrDisallowedDesktop() {
        assertEquals(Destination.UNAVAILABLE, ChatRouting.destination("openai/gpt", null, desktop, desktopReady = false))
        assertEquals(Destination.UNAVAILABLE, ChatRouting.destination("unlisted", phone, desktop, desktopReady = true))
        assertNull(ChatRouting.selected(null, null, emptyList(), desktopReady = false))
    }

    @Test fun aDesktopModelNamedLikePhoneCannotImpersonateThePhoneProvider() {
        assertEquals(Destination.UNAVAILABLE, ChatRouting.destination("phone/gemini", null, listOf("phone/gemini"), desktopReady = false))
        assertEquals(Destination.DESKTOP, ChatRouting.destination("phone/gemini", null, listOf("phone/gemini"), desktopReady = true))
    }

    @Test fun validDesktopWithoutPhoneKeepsOriginalSelection() {
        assertEquals("openai/gpt", ChatRouting.selected("openai/gpt", null, desktop, desktopReady = true))
        assertEquals(desktop.first(), ChatRouting.selected("gone", null, desktop, desktopReady = true))
    }
}
