package com.coucou.android.assistant

import android.os.Handler
import android.os.Looper
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicInteger
import com.coucou.android.core.ChatRole

/**
 * The phone's own AI transport. Fixed HTTPS endpoints; no user-supplied hostname,
 * redirect, proxy, telemetry or Android UI work on the network thread.
 * SSE is read incrementally, with callbacks delivered on the main thread.
 */
class CloudAssistant {
    private val main = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "coucou-phone-ai").apply { isDaemon = true }
    }
    private val generation = AtomicInteger(0)
    @Volatile private var current: HttpURLConnection? = null
    @Volatile private var future: Future<*>? = null

    fun cancel() {
        generation.incrementAndGet()
        future?.cancel(true)
        current?.disconnect()
    }

    fun send(
        request: AssistantRequest,
        onDelta: (String) -> Unit,
        onDone: () -> Unit,
        onError: (String) -> Unit,
    ) {
        cancel()
        val id = generation.get()
        future = executor.submit {
            try {
                val delivered = execute(request) { part ->
                    if (generation.get() == id) main.post {
                        if (generation.get() == id) onDelta(part)
                    }
                }
                if (generation.get() == id) main.post {
                    if (generation.get() == id) {
                        if (delivered) onDone() else onError("provider")
                    }
                }
            } catch (e: Throwable) {
                if (generation.get() == id) {
                    val reason = if (e is CloudFailure) e.reason else "unreachable"
                    main.post { if (generation.get() == id) onError(reason) }
                }
            } finally {
                if (generation.get() == id) current = null
            }
        }
    }

    private fun execute(request: AssistantRequest, emit: (String) -> Unit): Boolean {
        val endpoint = when (request.provider) {
            AssistantProvider.GEMINI -> "https://generativelanguage.googleapis.com/v1beta/models/" +
                request.model + ":streamGenerateContent?alt=sse"
            AssistantProvider.NVIDIA -> "https://integrate.api.nvidia.com/v1/chat/completions"
        }
        val url = URL(endpoint)
        require(url.protocol == "https" && (url.host == "generativelanguage.googleapis.com" ||
            url.host == "integrate.api.nvidia.com"))
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            instanceFollowRedirects = false
            connectTimeout = 12_000
            readTimeout = 70_000
            doOutput = true
            setRequestProperty("Accept", "text/event-stream")
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            when (request.provider) {
                AssistantProvider.GEMINI -> setRequestProperty("x-goog-api-key", request.apiKey)
                AssistantProvider.NVIDIA -> setRequestProperty("Authorization", "Bearer " + request.apiKey)
            }
        }
        current = conn
        try {
            val payload = when (request.provider) {
                AssistantProvider.GEMINI -> geminiBody(request)
                AssistantProvider.NVIDIA -> nvidiaBody(request)
            }.toString().toByteArray(Charsets.UTF_8)
            conn.outputStream.use { it.write(payload) }
            val status = conn.responseCode
            if (status !in 200..299) throw CloudFailure(when (status) {
                401, 403 -> "auth"
                429 -> "rate"
                400, 404, 422 -> "provider"
                else -> "unreachable"
            })
            var deliveredChars = 0
            val frames = SseFrames()
            fun consume(frame: String?) {
                if (frame.isNullOrBlank() || frame == "[DONE]") return
                val data = try { JSONObject(frame) } catch (_: Exception) { return }
                if (data.optJSONObject("error") != null) throw CloudFailure("provider")
                val part = when (request.provider) {
                    AssistantProvider.GEMINI -> geminiText(data)
                    AssistantProvider.NVIDIA -> nvidiaText(data)
                } ?: return
                // Match ChatSession's limit before posting anything to the UI.
                val remaining = 40_000 - deliveredChars
                if (remaining > 0) {
                    val chunk = part.take(remaining)
                    if (chunk.isNotEmpty()) {
                        deliveredChars += chunk.length
                        emit(chunk)
                    }
                }
            }
            conn.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                while (true) {
                    if (Thread.currentThread().isInterrupted) throw IOException("cancelled")
                    val line = reader.readLine() ?: break
                    consume(frames.feed(line))
                }
                consume(frames.finish())
            }
            return deliveredChars > 0
        } finally {
            conn.disconnect()
        }
    }

    private fun geminiBody(request: AssistantRequest): JSONObject {
        val messages = JSONArray()
        for (message in request.history) {
            val role = if (message.role == ChatRole.USER) "user" else "model"
            messages.put(JSONObject().put("role", role)
                .put("parts", JSONArray().put(JSONObject().put("text", message.text))))
        }
        return JSONObject()
            .put("systemInstruction", JSONObject().put("parts", JSONArray().put(
                JSONObject().put("text", AssistantPrompt.instruction(request.languageTag))
            )))
            .put("contents", messages)
            .put("generationConfig", JSONObject().put("maxOutputTokens", 2048))
    }

    private fun nvidiaBody(request: AssistantRequest): JSONObject {
        val messages = JSONArray()
        messages.put(JSONObject().put("role", "system")
            .put("content", AssistantPrompt.instruction(request.languageTag)))
        for (message in request.history) {
            messages.put(JSONObject()
                .put("role", if (message.role == ChatRole.USER) "user" else "assistant")
                .put("content", message.text))
        }
        return JSONObject().put("model", request.model).put("messages", messages)
            .put("stream", true).put("max_tokens", 2048)
    }

    private fun geminiText(data: JSONObject): String? {
        val candidate = data.optJSONArray("candidates")?.optJSONObject(0) ?: return null
        val parts = candidate.optJSONObject("content")?.optJSONArray("parts") ?: return null
        val text = StringBuilder()
        for (i in 0 until parts.length()) {
            val value = parts.optJSONObject(i)?.optString("text", "") ?: ""
            text.append(value)
        }
        return text.toString()
    }

    private fun nvidiaText(data: JSONObject): String? =
        data.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("delta")
            ?.optString("content", "")?.takeIf { it.isNotEmpty() }

    private class CloudFailure(val reason: String) : IOException()
}
