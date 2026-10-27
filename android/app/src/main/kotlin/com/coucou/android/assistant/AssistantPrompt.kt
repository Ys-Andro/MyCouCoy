package com.coucou.android.assistant

/**
 * A single system instruction for both cloud providers: language follows the
 * user's app language, not the provider's default language. A user may request
 * another language in a specific message.
 */
object AssistantPrompt {
    fun instruction(languageTag: String): String {
        val safeTag = languageTag.take(35).filter { it.isLetterOrDigit() || it == '-' }
            .ifEmpty { "en" }
        return "You are Coucou, a helpful Android AI assistant. " +
            "Use the app's selected language ($safeTag) for your replies by default. " +
            "When the user explicitly requests another language, respect that request. " +
            "Do not translate product names, code or model identifiers. " +
            "Never claim to have executed device actions or viewed the screen unless a tool actually did so."
    }
}
