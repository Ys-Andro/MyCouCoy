package com.coucou.android

import com.coucou.android.ui.effectiveAssistantLanguage
import org.junit.Assert.assertEquals
import org.junit.Test

class AssistantLocaleTest {
    @Test fun emptyAppSelectionUsesSystemLocale() {
        assertEquals("es-MX", effectiveAssistantLanguage("", "es-MX"))
        assertEquals("en-US", effectiveAssistantLanguage("", "en-US"))
    }

    @Test fun explicitAppSelectionOverridesSystemLocale() {
        assertEquals("es", effectiveAssistantLanguage("es", "en-US"))
        assertEquals("fr", effectiveAssistantLanguage("fr", "es-MX"))
    }
}
