package com.coucou.android

import com.coucou.android.assistant.SseFrames
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SseFramesTest {
    @Test fun singleLineEventWaitsForBlankLine() {
        val frames = SseFrames()
        assertNull(frames.feed(": keep-alive"))
        assertNull(frames.feed("data: {\"value\":1}"))
        assertEquals("{\"value\":1}", frames.feed(""))
        assertNull(frames.finish())
    }

    @Test fun multilineEventsAndEofAreDecoded() {
        val frames = SseFrames()
        frames.feed("event: message")
        frames.feed("data: first")
        frames.feed("data: second")
        assertEquals("first\nsecond", frames.feed(""))
        frames.feed("data: [DONE]")
        assertEquals("[DONE]", frames.finish())
    }

    @Test fun emptyLinesDoNotCreateEvents() {
        val frames = SseFrames()
        assertNull(frames.feed(""))
        assertNull(frames.feed("event: ping"))
        assertNull(frames.feed(""))
    }

    @Test fun oversizedEventsAreRejected() {
        val frames = SseFrames()
        var rejected = false
        try {
            frames.feed("data: " + "a".repeat(SseFrames.MAX_FRAME_CHARS + 1))
        } catch (_: IllegalArgumentException) {
            rejected = true
        }
        assertTrue(rejected)
    }
}
