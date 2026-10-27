package com.coucou.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.coucou.android.app.AppModel
import com.coucou.android.assistant.AssistantProvider
import com.coucou.android.core.TypeScale

/**
 * Manage on-phone AI without displaying previously stored secrets. Password text is deliberately
 * not rememberSaveable: Android must not serialise an unsaved key into Activity saved state.
 */
@Composable
fun AssistantSettingsCard(model: AppModel) {
    val es = assistantSpanish()
    val t = tokens()
    var modelDraft by remember(model.assistantProvider) { mutableStateOf(model.assistantModel) }
    var keyDraft by remember(model.assistantProvider) { mutableStateOf("") }
    var notice by remember(model.assistantProvider) { mutableStateOf<String?>(null) }
    Panel {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                if (es) "Conversa con una IA desde Android, incluso sin vincular una computadora."
                else "Chat with AI directly from Android, even without a paired computer.",
                style = TypeScale.SECONDARY.style(t.textDim.c()),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (provider in AssistantProvider.entries) {
                    FilterChip(
                        selected = model.assistantProvider == provider,
                        onClick = { model.selectAssistantProvider(provider); notice = null },
                        label = { Text(provider.title) },
                    )
                }
            }
            OutlinedTextField(
                value = modelDraft,
                onValueChange = { modelDraft = it.take(100) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(if (es) "Identificador del modelo" else "Model identifier") },
                singleLine = true,
                supportingText = { Text(if (es) "Usa un modelo disponible para tu clave API." else "Use a model available for your API key.") },
            )
            OutlinedButton(onClick = {
                notice = if (model.setAssistantModel(modelDraft)) {
                    if (es) "Modelo guardado" else "Model saved"
                } else {
                    if (es) "Identificador de modelo inválido" else "Invalid model identifier"
                }
            }) {
                Text(if (es) "Guardar modelo" else "Save model")
            }

            Text(
                if (model.assistantConfigured) {
                    if (es) "Clave API guardada de forma cifrada en este dispositivo."
                    else "API key stored encrypted on this device."
                } else {
                    if (es) "Agrega tu propia clave API para activar el chat independiente."
                    else "Add your own API key to enable independent chat."
                },
                style = TypeScale.SECONDARY.style(t.textDim.c()),
            )
            OutlinedTextField(
                value = keyDraft,
                onValueChange = { keyDraft = it.take(4096) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(if (es) "Clave API" else "API key") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    enabled = keyDraft.isNotBlank(),
                    onClick = {
                        val saved = model.saveAssistantKey(keyDraft)
                        keyDraft = ""
                        notice = if (saved) {
                            if (es) "Clave guardada. Ya puedes usar el chat."
                            else "Key saved. Phone AI chat is ready."
                        } else {
                            if (es) "No se pudo guardar la clave. Revisa el formato."
                            else "Could not save the key. Check its format."
                        }
                    },
                ) { Text(if (es) "Guardar clave" else "Save key") }
                if (model.assistantConfigured) {
                    TextButton(onClick = {
                        model.clearAssistantKey()
                        keyDraft = ""
                        notice = if (es) "Clave eliminada" else "Key removed"
                    }) { Text(if (es) "Eliminar clave" else "Remove key") }
                }
            }
            if (notice != null) Text(notice.orEmpty(), style = TypeScale.SECONDARY.style(t.text.c()))
            Text(
                if (es) "Las solicitudes van directamente al proveedor elegido por HTTPS. Las claves no se envían a Coucou Desktop."
                else "Requests go directly to the chosen AI provider over HTTPS. Keys are not sent to Coucou Desktop.",
                style = TypeScale.SECONDARY.style(t.textDim.c()),
            )
        }
    }
}
