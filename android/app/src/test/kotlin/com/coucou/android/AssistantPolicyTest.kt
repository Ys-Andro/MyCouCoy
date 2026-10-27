package com.coucou.android

import com.coucou.android.assistant.AssistantPolicy
import com.coucou.android.assistant.AssistantProvider
import com.coucou.android.assistant.ProviderHistory
import com.coucou.android.core.ChatMessage
import com.coucou.android.core.ChatRole
import com.coucou.android.core.ChatStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantPolicyTest {
    @Test fun modelsCannotChangeTheNetworkHost() {
        assertTrue(AssistantPolicy.modelValid("gemini-2.5-flash"))
        assertTrue(AssistantPolicy.modelValid("meta/llama-3.1-8b-instruct"))
        assertFalse(AssistantPolicy.modelValid("https://evil.test/model"))
        assertFalse(AssistantPolicy.modelValid("../other"))
        assertFalse(AssistantPolicy.modelValid("model?key=secret"))
        assertFalse(AssistantPolicy.modelValid(""))
    }

    @Test fun onlyCompletedAssistantTurnsAreSentAsHistory() {
        val list = listOf(
            ChatMessage("u1", ChatRole.USER, "Hi"),
            ChatMessage("a1", ChatRole.ASSISTANT, "Hello"),
            ChatMessage("u2", ChatRole.USER, "Question"),
            ChatMessage("a2", ChatRole.ASSISTANT, "partial", ChatStatus.STREAMING),
            ChatMessage("a3", ChatRole.ASSISTANT, "error", ChatStatus.FAILED),
        )
        assertEquals(listOf("u1", "a1", "u2"), AssistantPolicy.messageHistory(list).map { it.id })
    }

    @Test fun eachDefaultProviderModelIsValid() {
        for (provider in AssistantProvider.entries) {
            assertTrue(AssistantPolicy.modelValid(provider, provider.defaultModel))
        }
    }

    @Test fun geminiModelCannotEscapeTheModelsEndpoint() {
        assertTrue(AssistantPolicy.modelValid(AssistantProvider.GEMINI, "gemini-3.8-flash"))
        assertFalse(AssistantPolicy.modelValid(AssistantProvider.GEMINI, "gemini-3.8-flash/v1"))
        assertFalse(AssistantPolicy.modelValid(AssistantProvider.GEMINI, "gemini-3.8-flash:generateContent"))
        assertTrue(AssistantPolicy.modelValid(AssistantProvider.NVIDIA, "nvidia/nemotron-3.5-lightning-30b-a3b"))
    }

    @Test fun providersDoNotShareConversationContext() {
        val history = ProviderHistory()
        val google = AssistantProvider.GEMINI
        val nvidia = AssistantProvider.NVIDIA
        val question = ChatMessage("q1", ChatRole.USER, "private first prompt")
        val answer = ChatMessage("a1", ChatRole.ASSISTANT, "private first answer")
        history.completed(google, question, answer)

        val second = ChatMessage("q2", ChatRole.USER, "second provider")
        assertEquals(listOf("q2"), history.forRequest(nvidia, second).map { it.id })
        assertEquals(listOf("q1", "a1", "q2"), history.forRequest(google, second).map { it.id })

        history.clear()
        assertEquals(listOf("q2"), history.forRequest(google, second).map { it.id })
    }

    @Test fun providerIdsAreNamespacedAwayFromDesktopModels() {
        for (provider in AssistantProvider.entries) {
            assertEquals(provider, AssistantPolicy.fromLocalId(AssistantPolicy.localId(provider)))
        }
        assertEquals(null, AssistantPolicy.fromLocalId("openai/gpt-4"))
    }
}
