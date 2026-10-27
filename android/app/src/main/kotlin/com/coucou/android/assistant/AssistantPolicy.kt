package com.coucou.android.assistant

import com.coucou.android.core.ChatMessage
import com.coucou.android.core.ChatRole
import com.coucou.android.core.ChatStatus

/** Model names are identifiers, never URLs. Network hosts are fixed by the transport. */
enum class AssistantProvider(val id: String, val title: String, val defaultModel: String) {
    GEMINI("gemini", "Google Gemini", "gemini-3.8-flash"),
    NVIDIA("nvidia", "NVIDIA API", "nvidia/nemotron-3.5-lightning-30b-a3b");

    companion object {
        fun parse(value: String?): AssistantProvider =
            entries.firstOrNull { it.id == value } ?: GEMINI
    }
}

data class AssistantRequest(
    val provider: AssistantProvider,
    val model: String,
    val apiKey: String,
    val history: List<ChatMessage>,
)

object AssistantPolicy {
    const val MAX_HISTORY_MESSAGES = 20
    const val MAX_MODEL_LENGTH = 100
    private val MODEL_PATTERN = Regex("[A-Za-z0-9][A-Za-z0-9._:/-]{0,99}")

    /** Reject malformed identifiers and arbitrary URLs, not just suspicious prefixes. */
    fun modelValid(value: String): Boolean =
        value.length <= MAX_MODEL_LENGTH && MODEL_PATTERN.matches(value) &&
            !value.contains("://") && !value.contains("..")

    /** Provider-specific validation: Gemini models are a single path segment. */
    fun modelValid(provider: AssistantProvider, value: String): Boolean =
        modelValid(value) && (provider != AssistantProvider.GEMINI || ("/" !in value && ":" !in value))

    fun messageHistory(all: List<ChatMessage>): List<ChatMessage> =
        all.filter { message ->
            message.text.isNotBlank() &&
                (message.role == ChatRole.USER ||
                    (message.role == ChatRole.ASSISTANT && message.status == ChatStatus.DONE))
        }.takeLast(MAX_HISTORY_MESSAGES)

    fun localId(provider: AssistantProvider): String = "phone/" + provider.id

    fun fromLocalId(id: String?): AssistantProvider? =
        AssistantProvider.entries.firstOrNull { localId(it) == id }
}
