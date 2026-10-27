package com.coucou.android.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.coucou.android.app.AppLanguage

/**
 * AppLanguage.current returns an empty tag when the user has chosen "System".
 * The effective language then comes from the Context's active resource configuration,
 * not from a hard-coded English fallback.
 */
internal fun effectiveAssistantLanguage(configured: String, system: String): String =
    configured.ifBlank { system }

@Composable
internal fun assistantSpanish(): Boolean {
    val context: Context = LocalContext.current
    val system = context.resources.configuration.locales[0].toLanguageTag()
    return effectiveAssistantLanguage(AppLanguage.current(context), system)
        .startsWith("es", ignoreCase = true)
}
