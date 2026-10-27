package com.coucou.android.assistant

/**
 * Small, bounded Server-Sent Events decoder. A data event ends at a blank line.
 * Multiline data is joined with newlines as required by the SSE format.
 * Only text frames are stored; comments and event metadata are ignored.
 */
class SseFrames {
    private val data = StringBuilder()

    fun feed(line: String): String? {
        if (line.isEmpty()) return finish()
        if (line.startsWith("data:")) {
            val value = line.substring(5).removePrefix(" ")
            require(data.length + value.length + 1 <= MAX_FRAME_CHARS) { "SSE frame too large" }
            if (data.isNotEmpty()) data.append('\n')
            data.append(value)
        }
        return null
    }

    fun finish(): String? {
        if (data.isEmpty()) return null
        val frame = data.toString()
        data.setLength(0)
        return frame
    }

    companion object {
        const val MAX_FRAME_CHARS = 256 * 1024
    }
}
