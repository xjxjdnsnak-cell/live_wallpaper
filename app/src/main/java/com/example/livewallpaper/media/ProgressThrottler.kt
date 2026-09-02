package com.example.livewallpaper.media

/**
 * Pure wall-clock throttle decision for progress callbacks (audit P-4).
 *
 * Non-forced updates pass through at most once per [minIntervalMs] of
 * wall-clock time; forced updates (e.g. 0% start / 100% completion) always
 * pass. Kept free of Android/framework dependencies so the decision logic
 * is unit-testable.
 */
class ProgressThrottler(
    private val minIntervalMs: Long = DEFAULT_MIN_INTERVAL_MS,
    private val nowMs: () -> Long = System::currentTimeMillis,
) {
    private var lastEmitAtMs: Long? = null

    /**
     * Returns true when an update should be emitted now. A forced update
     * always emits and restarts the throttle window.
     */
    fun shouldEmit(forced: Boolean = false): Boolean {
        val now = nowMs()
        val last = lastEmitAtMs
        val emit = forced || last == null || now - last >= minIntervalMs
        if (emit) lastEmitAtMs = now
        return emit
    }

    companion object {
        const val DEFAULT_MIN_INTERVAL_MS = 100L
    }
}
