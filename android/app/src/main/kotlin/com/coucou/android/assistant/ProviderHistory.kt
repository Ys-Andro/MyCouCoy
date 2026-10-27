package com.coucou.android.assistant

import com.coucou.android.core.ChatMessage
import com.coucou.android.core.ChatRole
import com.coucou.android.core.ChatStatus

/**
 * Each provider receives only the turns made with that provider.
 * The on-screen transcript can remain shared without forwarding a desktop session
 * or a different provider's private conversation to a newly selected API.
 */
class ProviderHistory {
    private val turns = mutableMapOf<AssistantProvider, List<ChatMessage>>()

    fun forRequest(provider: AssistantProvider, user: ChatMessage): List<ChatMessage> {
        require(user.role == ChatRole.USER)
        return AssistantPolicy.messageHistory(turns[provider].orEmpty() + user)
    }

    fun completed(provider: AssistantProvider, user: ChatMessage, response: ChatMessage) {
        if (user.role != ChatRole.USER || response.role != ChatRole.ASSISTANT ||
            response.status != ChatStatus.DONE || response.text.isBlank()) return
        turns[provider] = AssistantPolicy.messageHistory(turns[provider].orEmpty() + user + response)
    }

    fun clear() { turns.clear() }
}
