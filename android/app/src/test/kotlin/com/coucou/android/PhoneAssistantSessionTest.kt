package com.coucou.android

import com.coucou.android.assistant.AssistantProvider
import com.coucou.android.assistant.AssistantRequest
import com.coucou.android.assistant.AssistantTransport
import com.coucou.android.assistant.PhoneAssistantSession
import com.coucou.android.core.ChatMessage
import com.coucou.android.core.ChatRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneAssistantSessionTest {
    private class FakeTransport : AssistantTransport {
        val requests = mutableListOf<AssistantRequest>()
        var onDone: (() -> Unit)? = null
        var onError: ((String) -> Unit)? = null
        var cancelled = 0
        override fun send(
            request: AssistantRequest, onDelta: (String) -> Unit,
            onDone: () -> Unit, onError: (String) -> Unit,
        ) {
            requests += request
            this.onDone = onDone
            this.onError = onError
        }

        override fun cancel() { cancelled++ }

        fun finish() { onDone?.invoke() }
    }

    private fun credential(provider: AssistantProvider) =
        AssistantRequest(provider, provider.defaultModel, "test-key-only", emptyList())

    private fun user(id: String, text: String) = ChatMessage("u-$id", ChatRole.USER, text)
    private fun answer(id: String, text: String) = ChatMessage(id, ChatRole.ASSISTANT, text)

    @Test fun canStartAndCompletePhoneConversationWithoutAnyDesktop() {
        val fake = FakeTransport()
        val phone = PhoneAssistantSession({ credential(AssistantProvider.GEMINI) }, fake)
        assertTrue(phone.send(AssistantProvider.GEMINI, user("1", "Hola"), "es-MX",
            onDelta = {}, onDone = { answer("1", "Hola") }, onError = {}))
        assertEquals(listOf("Hola"), fake.requests.single().history.map { it.text })
        assertEquals("es-MX", fake.requests.single().languageTag)
        fake.finish()

        assertTrue(phone.send(AssistantProvider.GEMINI, user("2", "Otra"), "es-MX",
            onDelta = {}, onDone = { answer("2", "Claro") }, onError = {}))
        assertEquals(listOf("Hola", "Hola", "Otra"), fake.requests.last().history.map { it.text })
    }

    @Test fun providersNeverSeeEachOthersHistoryOrKeys() {
        val fake = FakeTransport()
        var chosen = AssistantProvider.GEMINI
        val phone = PhoneAssistantSession({ credential(chosen) }, fake)
        phone.send(AssistantProvider.GEMINI, user("a", "private Gemini"),
            "es", {}, { answer("a", "gemini answer") }, {})
        fake.finish()
        chosen = AssistantProvider.NVIDIA
        phone.send(AssistantProvider.NVIDIA, user("b", "NVIDIA request"),
            "es", {}, { answer("b", "NVIDIA response") }, {})
        assertEquals(listOf("NVIDIA request"), fake.requests.last().history.map { it.text })
        assertEquals(AssistantProvider.NVIDIA, fake.requests.last().provider)
    }

    @Test fun refusesMismatchedOrAbsentCredentialWithoutSending() {
        val fake = FakeTransport()
        val phone = PhoneAssistantSession({ credential(AssistantProvider.GEMINI) }, fake)
        assertFalse(phone.send(AssistantProvider.NVIDIA, user("c", "hi"), "en", {}, { null }, {}))
        assertTrue(fake.requests.isEmpty())
        val absent = PhoneAssistantSession({ null }, fake)
        assertFalse(absent.send(AssistantProvider.GEMINI, user("d", "hi"), "en", {}, { null }, {}))
        assertTrue(fake.requests.isEmpty())
    }

    @Test fun clearRemovesAllProviderContextAndCancelIsLocalOnly() {
        val fake = FakeTransport()
        val phone = PhoneAssistantSession({ credential(AssistantProvider.GEMINI) }, fake)
        phone.send(AssistantProvider.GEMINI, user("e", "first"), "en", {}, { answer("e", "previous") }, {})
        fake.finish()
        phone.cancel()
        assertEquals(1, fake.cancelled)
        phone.clear()
        phone.send(AssistantProvider.GEMINI, user("f", "new"), "en", {}, { null }, {})
        assertEquals(listOf("new"), fake.requests.last().history.map { it.text })
    }
}
