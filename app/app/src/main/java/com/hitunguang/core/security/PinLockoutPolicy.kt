package com.hitunguang.core.security

object PinLockoutPolicy {
    const val MAX_ATTEMPTS = 5
    const val BASE_LOCKOUT_MS = 30_000L
    const val MAX_LOCKOUT_MS = 30 * 60_000L

    fun lockoutDurationMs(failedAttempts: Int): Long {
        if (failedAttempts < MAX_ATTEMPTS) return 0L
        val doublings = (failedAttempts - MAX_ATTEMPTS).coerceAtMost(16)
        return (BASE_LOCKOUT_MS shl doublings).coerceAtMost(MAX_LOCKOUT_MS)
    }

    fun remainingAttempts(failedAttempts: Int): Int =
        (MAX_ATTEMPTS - failedAttempts).coerceAtLeast(0)
}
