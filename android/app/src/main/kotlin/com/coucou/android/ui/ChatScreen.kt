package com.coucou.android.ui

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import java.util.Locale

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.coucou.android.core.IconKind
import com.coucou.android.core.Spacing
import com.coucou.android.core.TypeScale
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.coucou.android.R
import com.coucou.android.app.AppModel
import com.coucou.android.core.ChatMessage
import com.coucou.android.core.ChatReasons
import com.coucou.android.core.ChatTab
import com.coucou.android.core.ChatTabState
import com.coucou.android.core.Screen
import com.coucou.android.core.ShareDraft
import com.coucou.android.mochi.BotState
import com.coucou.android.mochi.MochiEngine
import com.coucou.android.mochi.MochiView
import com.coucou.android.core.ChatRole
import com.coucou.android.core.ChatSession
import com.coucou.android.core.ChatStatus
import com.coucou.android.core.MarkdownLite
import com.coucou.android.core.MdBlock
import com.coucou.android.core.MdSpan
import com.coucou.android.link.Protocol
import androidx.compose.ui.platform.LocalContext

/** Local errors are about the phone's connection, not Coucou Desktop. */
@Composable
private fun visibleChatError(code: String?): String {
    val es = assistantSpanish()
    return when (code) {
        "phone_offline" -> if (es) "Sin conexión a Internet. Reconecta el teléfono e inténtalo de nuevo."
            else "No Internet connection. Reconnect your phone and try again."
        "phone_unreachable" -> if (es) "No se pudo conectar con el proveedor de IA. Comprueba Internet y vuelve a intentarlo."
            else "Could not reach the AI provider. Check your Internet connection and retry."
        "phone_auth" -> if (es) "El proveedor rechazó la clave API del teléfono. Revísala en Ajustes."
            else "The provider rejected the phone's API key. Check it in Settings."
        "phone_provider" -> if (es) "El proveedor devolvió un error. Revisa el modelo y vuelve a intentarlo."
            else "The provider returned an error. Check the model and retry."
        "phone_rate" -> if (es) "Has alcanzado el límite de solicitudes. Inténtalo más tarde."
            else "The request limit has been reached. Try again later."
        "phone_no_key" -> if (es) "Debes configurar una clave API en Ajustes."
            else "Set up an API key in Settings."
        else -> reasonText(code)
    }
}

/** The words for a code the computer (or this app) gives when an answer did not complete. */
@Composable
private fun reasonText(code: String?): String = stringResource(
    when (ChatReasons.key(code)) {
        "off" -> R.string.chat_err_off
        "not_allowed" -> R.string.chat_err_not_allowed
        "busy" -> R.string.chat_err_busy
        "rate" -> R.string.chat_err_rate
        "too_long" -> R.string.chat_err_too_long
        "empty" -> R.string.chat_err_empty
        "no_key" -> R.string.chat_err_no_key
        "unreachable" -> R.string.chat_err_unreachable
        "auth" -> R.string.chat_err_auth
        "provider" -> R.string.chat_err_provider
        "canceled" -> R.string.chat_err_canceled
        "connection" -> R.string.chat_err_connection
        "interrupted" -> R.string.chat_err_interrupted
        else -> R.string.chat_err_internal
    },
)

internal fun chatStateTitle(state: ChatTabState): Int = when (state) {
    ChatTabState.NOT_PAIRED -> R.string.chat_off_not_paired_title
    ChatTabState.NOT_CONNECTED -> R.string.chat_off_offline_title
    ChatTabState.CHAT_OFF -> R.string.chat_off_title
    ChatTabState.NO_MODELS -> R.string.chat_nomodel_title
    ChatTabState.READY -> R.string.chat_title
}

internal fun chatStateBody(state: ChatTabState): Int = when (state) {
    ChatTabState.NOT_PAIRED -> R.string.chat_off_not_paired_body
    ChatTabState.NOT_CONNECTED -> R.string.chat_off_offline_body
    ChatTabState.CHAT_OFF, ChatTabState.NO_MODELS -> R.string.chat_off_hint
    ChatTabState.READY -> R.string.chat_title
}

/** The Chat tab with nothing to chat with: a sleeping Mochi, what is wrong in plain words, and the one button that helps (if any). */
@Composable
private fun ChatEmpty(state: ChatTabState, onHome: () -> Unit, onSettings: () -> Unit) {
    val t = tokens()
    val engine = remember { MochiEngine({ android.os.SystemClock.elapsedRealtimeNanos() / 1e6 }).apply { setState(BotState.SLEEPING, force = true) } }
    Column(Modifier.fillMaxSize().padding(horizontal = Gutter)) {
        ScreenTitle(stringResource(R.string.chat_title), null)
        Column(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
        ) {
            MochiView(engine, Modifier.size(120.dp))
            Text(
                if (state == ChatTabState.NOT_PAIRED) {
                    if (assistantSpanish()) "Configura tu IA en el teléfono" else "Set up AI on your phone"
                } else stringResource(chatStateTitle(state)),
                Modifier.padding(top = 16.dp),
                style = TypeScale.TITLE.style(t.text.c()).copy(textAlign = TextAlign.Center),
            )
            Text(
                if (state == ChatTabState.NOT_PAIRED) {
                    if (assistantSpanish()) "Puedes conversar sin vincular una computadora. Añade tu clave API en Ajustes."
                    else "Chat without pairing a computer. Add your API key in Settings."
                } else stringResource(chatStateBody(state)),
                Modifier.padding(top = 8.dp, bottom = 20.dp),
                style = TypeScale.BODY.style(t.textDim.c()).copy(textAlign = TextAlign.Center),
            )
            when (ChatTab.action(state)) {
                Screen.HOME -> {
                    val spanish = assistantSpanish()
                    PillButton(
                        if (spanish) "Configurar IA en el teléfono" else "Set up AI on this phone",
                        onSettings, Modifier.fillMaxWidth(), PillKind.PRIMARY,
                    )
                    TextButton(onClick = onHome) { Text(stringResource(R.string.chat_go_pair)) }
                }
                Screen.SETTINGS -> PillButton(stringResource(R.string.chat_go_settings), onSettings, Modifier.fillMaxWidth(), PillKind.PRIMARY)
                else -> {}
            }
        }
    }
}

/**
 * Chat with the computer's AI providers, like the PC's chat panel: your messages on the right, the
 * answers as plain light Markdown, three dots while waiting, the model above the box. The computer's
 * API key is used on the computer; this screen only ever has text.
 */
@Composable
fun ChatScreen(model: AppModel, onHome: () -> Unit, onSettings: () -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    var refusal by remember { mutableStateOf<ChatSession.Refusal?>(null) }
    var confirmClear by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val messages = model.chatMessages
    val localMode = model.activeChatDestination == com.coucou.android.core.ChatRouting.Destination.PHONE
    val context = LocalContext.current
    val spanish = assistantSpanish()
    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val phrase = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!phrase.isNullOrBlank()) text = ShareDraft.into(text, phrase)
        }
    }
    var ttsReady by remember { mutableStateOf(false) }
    val tts = remember(context) {
        TextToSpeech(context) { status -> ttsReady = status == TextToSpeech.SUCCESS }
    }
    DisposableEffect(tts) {
        onDispose { tts.stop(); tts.shutdown() }
    }
    val assistantEngine = remember {
        MochiEngine({ android.os.SystemClock.elapsedRealtimeNanos() / 1e6 })
    }
    LaunchedEffect(model.assistantState) {
        assistantEngine.setState(model.assistantState, force = true)
    }

    LaunchedEffect(Unit) { model.chatRefreshModels() }
    // Follow the answer as it grows.
    val lastLength = messages.lastOrNull()?.text?.length ?: 0
    LaunchedEffect(messages.size, lastLength) {
        if (messages.isNotEmpty()) listState.scrollToItem(messages.size - 1)
    }

    val tabState = model.chatTabState
    // Text shared from another app waits for a chat that can take it: it goes in the box (never sent), or is dropped with a word if the
    // computer does not offer chat. While the computer is not reachable yet it stays, for a couple of minutes.
    val sharedChatOff = stringResource(R.string.share_chat_off)
    LaunchedEffect(tabState, model.shareVersion) {
        when (tabState) {
            ChatTabState.READY -> model.shareDraft.take()?.let { text = ShareDraft.into(text, it) }
            ChatTabState.CHAT_OFF, ChatTabState.NO_MODELS -> if (model.shareDraft.take() != null) model.message = sharedChatOff
            else -> {}
        }
    }
    // Nothing to chat with and nothing to read: say why, calmly, with the one thing to do about it.
    if (tabState != ChatTabState.READY && messages.isEmpty()) {
        ChatEmpty(tabState, onHome, onSettings)
        return
    }

    Column(Modifier.fillMaxSize().padding(horizontal = Gutter)) {
        ScreenTitle(
            stringResource(R.string.chat_title), null,
            trailing = {
                if (messages.isNotEmpty()) {
                    Text(
                        stringResource(R.string.chat_clear),
                        Modifier.heightIn(min = Spacing.MIN_TOUCH.dp).clickable(role = Role.Button) { confirmClear = true }.padding(horizontal = 12.dp, vertical = 14.dp),
                        style = TypeScale.LABEL.style(tokens().textDim.c()), maxLines = 1,
                    )
                }
            },
        )
        if (localMode) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MochiView(assistantEngine, Modifier.size(34.dp))
                Text(
                    if (spanish) "IA directamente en este teléfono · Sin computadora"
                    else "AI running from this phone · No computer required",
                    modifier = Modifier.weight(1f),
                    style = TypeScale.SECONDARY.style(tokens().textDim.c()),
                )
            }
        } else {
            Text(
                stringResource(R.string.chat_note), Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                style = TypeScale.SECONDARY.style(tokens().textDim.c()),
            )
        }
        if (localMode && (!model.assistantNetworkOnline || model.assistantNetworkRestored)) {
            Text(
                if (!model.assistantNetworkOnline) {
                    if (spanish) "Sin Internet. El chat volverá a funcionar cuando haya conexión."
                    else "No Internet. Chat will work again when a connection is available."
                } else {
                    if (spanish) "Conexión restablecida. Ya puedes volver a enviar tu mensaje."
                    else "Connection restored. You can send your message again."
                },
                Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite },
                style = TypeScale.SECONDARY.style(
                    if (!model.assistantNetworkOnline) tokens().danger.c() else tokens().text.c()
                ),
            )
        }
        if (!model.chatAvailable) {
            // The conversation stays readable; the reason sits above it.
            Panel(Modifier.padding(vertical = 4.dp)) {
                Column(Modifier.padding(Gutter), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(chatStateTitle(tabState)), style = TypeScale.HEADLINE.style(tokens().text.c()))
                    Text(stringResource(chatStateBody(tabState)), style = TypeScale.SECONDARY.style(tokens().textDim.c()))
                }
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (messages.isEmpty()) {
                Text(
                    stringResource(R.string.chat_empty), Modifier.align(Alignment.Center).padding(Gutter),
                    style = TypeScale.BODY.style(tokens().textDim.c()).copy(textAlign = TextAlign.Center),
                )
            } else {
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(Gap)) {
                    items(messages, key = { it.id }) { m -> Bubble(m) }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                enabled = model.chatAvailable,
                onClick = {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                    }
                    runCatching { voiceLauncher.launch(intent) }.onFailure {
                        model.message = if (spanish) "No se encontró un servicio de reconocimiento de voz."
                            else "No speech recognition service is available."
                    }
                },
            ) { Text(if (spanish) "Dictar" else "Dictate") }
            val lastAnswer = messages.lastOrNull {
                it.role == ChatRole.ASSISTANT && it.status == ChatStatus.DONE && it.text.isNotBlank()
            }
            if (ttsReady && lastAnswer != null) {
                TextButton(onClick = {
                    tts.language = Locale.getDefault()
                    tts.speak(lastAnswer.text.take(3500), TextToSpeech.QUEUE_FLUSH, null, "coucou-voice-answer")
                }) { Text(if (spanish) "Escuchar respuesta" else "Read aloud") }
            }
        }
        Composer(
            model, text, refusal,
            onText = { text = it; refusal = null },
            onSend = {
                val r = model.chatSend(text)
                if (r == null) { text = ""; refusal = null } else refusal = r
            },
        )
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.chat_clear_title)) },
            text = { Text(stringResource(R.string.chat_clear_body)) },
            confirmButton = {
                TextButton(onClick = { confirmClear = false; model.chatClear() }) {
                    Text(stringResource(R.string.chat_clear), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun Bubble(m: ChatMessage) {
    if (m.role == ChatRole.USER) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            SelectionContainer {
                Text(
                    m.text,
                    Modifier.widthIn(max = 300.dp)
                        .clip(RoundedCornerShape(18.dp, 18.dp, 6.dp, 18.dp))
                        .background(tokens().panel2.c())
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    style = TypeScale.BODY.style(tokens().text.c()),
                )
            }
        }
        return
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (m.text.isEmpty() && m.status == ChatStatus.STREAMING) TypingDots()
        else if (m.text.isNotEmpty()) SelectionContainer { MarkdownText(m.text) }
        if (m.status == ChatStatus.FAILED) {
            Text(visibleChatError(m.reason), style = TypeScale.SECONDARY.style(tokens().danger.c()))
        }
    }
}

/** The three dots of the PC's chat; they only move while this is on screen. */
@Composable
private fun TypingDots() {
    val transition = rememberInfiniteTransition(label = "typing")
    val phase by transition.animateFloat(
        initialValue = 0f, targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart), label = "dots",
    )
    Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(3) { i ->
            val on = phase.toInt() == i
            Box(
                Modifier.size(8.dp).clip(CircleShape)
                    .background(tokens().textDim.c().copy(alpha = if (on) 1f else 0.35f)),
            )
        }
    }
}

@Composable
private fun MarkdownText(text: String) {
    val blocks = remember(text) { MarkdownLite.parse(text) }
    val t = tokens()
    val code = t.panel2.c()
    val body = TypeScale.BODY.style(t.text.c())
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (b in blocks) {
            when (b) {
                is MdBlock.Para -> Text(styled(b.spans, code), style = body)
                is MdBlock.Bullet -> Row {
                    Text("•  ", style = body)
                    Text(styled(b.spans, code), Modifier.weight(1f), style = body)
                }
                is MdBlock.Code -> Text(
                    b.text,
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(code)
                        .border(1.dp, t.line.c(), RoundedCornerShape(12.dp)).padding(12.dp),
                    style = TypeScale.MONO.style(t.text.c()).copy(fontFamily = FontFamily.Monospace),
                )
            }
        }
    }
}

private fun styled(spans: List<MdSpan>, codeBackground: androidx.compose.ui.graphics.Color): AnnotatedString = buildAnnotatedString {
    for (s in spans) {
        val style = SpanStyle(
            fontWeight = if (s.bold) FontWeight.Bold else null,
            fontFamily = if (s.code) FontFamily.Monospace else null,
            background = if (s.code) codeBackground else androidx.compose.ui.graphics.Color.Unspecified,
        )
        withStyle(style) { append(s.text) }
    }
}

@Composable
private fun Composer(model: AppModel, text: String, refusal: ChatSession.Refusal?, onText: (String) -> Unit, onSend: () -> Unit) {
    val limit = Protocol.CHAT_MAX_TEXT
    val length = text.trim().length
    val t = tokens()
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ModelChip(model)
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = text, onValueChange = { onText(it.take(limit + 200)) }, modifier = Modifier.weight(1f),
                placeholder = { Text(stringResource(R.string.chat_placeholder), color = t.textFaint.c()) },
                maxLines = 4, shape = RoundedCornerShape(26.dp), enabled = model.chatAvailable,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = t.panel.c(), unfocusedContainerColor = t.panel.c(), disabledContainerColor = t.panel.c(),
                    focusedBorderColor = t.textDim.c(), unfocusedBorderColor = t.line.c(), disabledBorderColor = t.line.c(),
                    focusedTextColor = t.text.c(), unfocusedTextColor = t.text.c(), cursorColor = t.text.c(),
                ),
            )
            if (model.chatBusy) {
                PillButton(stringResource(R.string.chat_stop), { model.chatCancel() }, Modifier.height(56.dp))
            } else {
                val canSend = model.chatAvailable && length in 1..limit
                val send = stringResource(R.string.chat_send)
                Box(
                    Modifier.alpha(if (canSend) 1f else 0.4f).size(56.dp).clip(CircleShape).background(t.primaryButton.c())
                        .clickable(enabled = model.chatAvailable && length in 1..limit, role = Role.Button, onClick = onSend)
                        .semantics { contentDescription = send },
                    contentAlignment = Alignment.Center,
                ) { CoucouIcon(IconKind.SEND, tint = t.onPrimaryButton.c(), size = 22.dp) }
            }
        }
        val note = when {
            refusal == ChatSession.Refusal.TOO_LONG || length > limit -> stringResource(R.string.chat_ref_too_long)
            refusal == ChatSession.Refusal.NO_MODEL -> stringResource(R.string.chat_ref_no_model)
            refusal == ChatSession.Refusal.BUSY -> stringResource(R.string.chat_ref_busy)
            refusal == ChatSession.Refusal.OFFLINE &&
                model.activeChatDestination == com.coucou.android.core.ChatRouting.Destination.PHONE ->
                if (assistantSpanish()) "Sin Internet en el teléfono. Reconecta e inténtalo otra vez."
                else "The phone has no Internet. Reconnect and try again."
            refusal == ChatSession.Refusal.OFFLINE -> stringResource(R.string.chat_ref_offline)
            else -> null
        }
        if (note != null) Text(note, style = TypeScale.SECONDARY.style(t.danger.c()))
        else if (length > limit * 9 / 10) {
            Text(stringResource(R.string.chat_count, length, limit), style = TypeScale.SECONDARY.style(t.textDim.c()))
        }
    }
}

/** The model above the box, like the PC's: tap to pick another of the ones the computer allows. */
@Composable
private fun ModelChip(model: AppModel) {
    var open by remember { mutableStateOf(false) }
    val models = model.availableChatModels
    val current = models.firstOrNull { it.id == model.activeChatModel }
    val t = tokens()
    Box {
        Row(
            Modifier.heightIn(min = Spacing.MIN_TOUCH.dp).clip(CircleShape).background(t.panel2.c())
                .clickable(enabled = models.size > 1, role = Role.Button) { open = true }.padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.chat_model, current?.label ?: "—"), style = TypeScale.LABEL.style(t.text.c()), maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (models.size > 1) CoucouIcon(IconKind.CHEVRON, tint = t.textDim.c(), size = 16.dp, modifier = Modifier.rotate(90f), mirror = false)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            for (m in models) {
                DropdownMenuItem(
                    text = { Text(m.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    onClick = { model.chatSelect(m.id); open = false },
                )
            }
        }
    }
}
