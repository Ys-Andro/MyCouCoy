package com.coucou.android.ui

import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.coucou.android.app.AppModel
import com.coucou.android.core.TypeScale
import com.coucou.android.mochi.MochiEngine
import com.coucou.android.mochi.MochiView

/**
 * When phone AI has been configured, opening Coucou no longer demands a desktop.
 * The classic pairing flow is still one tap away and all desktop features remain.
 */
@Composable
fun AssistantHome(model: AppModel, onChat: () -> Unit, onScan: () -> Unit, onSettings: () -> Unit) {
    var pairing by remember { mutableStateOf(false) }
    if (pairing) {
        PairingScreen(model, onScan = onScan, onAbout = onSettings)
        return
    }

    val spanish = assistantSpanish()
    val t = tokens()
    val engine = remember {
        MochiEngine({ SystemClock.elapsedRealtimeNanos() / 1e6 })
    }
    LaunchedEffect(model.assistantState) {
        engine.setState(model.assistantState, force = true)
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = Gutter + 8.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.size(20.dp))
        MochiView(engine, Modifier.size(168.dp))
        Text(
            if (spanish) "Coucou está listo" else "Coucou is ready",
            style = TypeScale.DISPLAY.style(t.text.c()).copy(textAlign = TextAlign.Center),
        )
        Text(
            if (spanish) "Tu asistente IA funciona desde este teléfono. Puedes conversar sin encender una computadora."
            else "Your AI assistant runs from this phone. Chat without turning on a computer.",
            style = TypeScale.BODY.style(t.textDim.c()).copy(textAlign = TextAlign.Center),
        )
        Text(
            model.assistantProvider.title + " · " + model.assistantModel,
            style = TypeScale.SECONDARY.style(t.textDim.c()).copy(textAlign = TextAlign.Center),
        )
        PillButton(
            if (spanish) "Empezar a conversar" else "Start chatting",
            onChat, Modifier.fillMaxWidth(), PillKind.PRIMARY,
        )
        PillButton(
            if (spanish) "Configurar IA" else "AI settings",
            onSettings, Modifier.fillMaxWidth(),
        )
        PillButton(
            if (spanish) "Conectar una computadora (opcional)" else "Pair a computer (optional)",
            { pairing = true }, Modifier.fillMaxWidth(),
        )
        Text(
            if (spanish) "Coucou utiliza tu propia API. El chat en la nube requiere conexión a Internet."
            else "Coucou uses your own API key. Cloud chat needs Internet access.",
            style = TypeScale.SECONDARY.style(t.textFaint.c()).copy(textAlign = TextAlign.Center),
        )
    }
}
