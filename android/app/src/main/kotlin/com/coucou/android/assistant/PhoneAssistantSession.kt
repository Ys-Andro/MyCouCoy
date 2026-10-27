package com.coucou.android.assistant

import com.coucou.android.core.ChatMessage
import com.coucou.android.core.ChatRole

/**
 * Android's autonomous AI conversation coordinator.
 *
 * No DesktopLink, pairing, Android Activity or UI dependency. The AppModel owns the
 * visible ChatSession and persistence; this class owns provider-specific request
 * construction, isolated history and the cancellable phone network transport.
 */
interface AssistantTransport {
    fun send(
        request: AssistantRequest,
        onDelta: (String) -> Unit,
        onDone: () -> Unit,
        onError: (String) -> Unit,
    )

    fun cancel()
}

class PhoneAssistantSession(
    private val credentials: () -> AssistantRequest?,
    private val transport: AssistantTransport,
    private val providerHistory: ProviderHistory = ProviderHistory(),
) {
    /**
     * Starts a phone turn when credentials match the selected provider.
     * Returns false without touching the transport if the provider is not ready.
     * [onDone] returns the final on-screen response for provider-local history.
     */
    fun send(
        provider: AssistantProvider,
        user: ChatMessage,
        languageTag: String,
        onDelta: (String) -> Unit,
        onDone: () -> ChatMessage?,
        onError: (String) -> Unit,
    ): Boolean {
        require(user.role == ChatRole.USER)
        val base = credentials()?.takeIf { it.provider == provider } ?: return false
        val request = base.copy(
            history = providerHistory.forRequest(provider, user),
            languageTag = languageTag,
        )
        transport.send(
            request,
            onDelta = onDelta,
            onDone = {
                val response = onDone()
                if (response != null) providerHistory.completed(provider, user, response)
            },
            onError = onError,
        )
        return true
    }

    fun cancel() = transport.cancel()

    /** Clearing the visible conversation also clears every provider's context. */
    fun clear() = providerHistory.clear()
}
