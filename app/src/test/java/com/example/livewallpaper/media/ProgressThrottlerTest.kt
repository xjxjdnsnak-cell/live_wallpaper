package com.example.livewallpaper.media

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressThrottlerTest {

    @Test
    fun `first non-forced update emits`() {
        var now = 0L
        val throttler = ProgressThrottler { now }
        assertTrue(throttler.shouldEmit())
    }

    @Test
    fun `non-forced updates within interval are suppressed`() {
        var now = 0L
        val throttler = ProgressThrottler { now }
        assertTrue(throttler.shouldEmit())
        now = 50L
        assertFalse(throttler.shouldEmit())
        now = 99L
        assertFalse(throttler.shouldEmit())
    }

    @Test
    fun `non-forced update after interval emits`() {
        var now = 0L
        val throttler = ProgressThrottler { now }
        assertTrue(throttler.shouldEmit())
        now = 100L
        assertTrue(throttler.shouldEmit())
        now = 150L
        assertFalse(throttler.shouldEmit())
        now = 200L
        assertTrue(throttler.shouldEmit())
    }

    @Test
    fun `forced updates always emit even with no elapsed time`() {
        var now = 0L
        val throttler = ProgressThrottler { now }
        assertTrue(throttler.shouldEmit(forced = true))
        assertTrue(throttler.shouldEmit(forced = true))
    }

    @Test
    fun `forced update restarts the throttle window`() {
        var now = 0L
        val throttler = ProgressThrottler { now }
        assertTrue(throttler.shouldEmit())
        now = 50L
        assertTrue(throttler.shouldEmit(forced = true))
        now = 100L
        assertFalse(throttler.shouldEmit())
        now = 150L
        assertTrue(throttler.shouldEmit())
    }

    @Test
    fun `custom interval is respected`() {
        var now = 0L
        val throttler = ProgressThrottler(minIntervalMs = 250L) { now }
        assertTrue(throttler.shouldEmit())
        now = 240L
        assertFalse(throttler.shouldEmit())
        now = 250L
        assertTrue(throttler.shouldEmit())
    }
}
