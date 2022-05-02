package com.arielpozo.dataclasses

import java.time.Duration
import java.time.Instant

data class Rate(val quota: Int, val resetInstant: Instant) {

    fun isQuotaEmpty(): Boolean {
        return quota <= 0
    }

    fun useQuota(): Rate {
        return if (isQuotaEmpty()) {
            this
        } else {
            this.copy(quota = quota - 1)
        }
    }

    fun canReset(): Boolean {
        return resetInstant <= Instant.now()
    }

    fun reset(limit: Int, resetTime: Duration): Rate {
        return if (canReset()) {
            Rate(limit, Instant.now().plus(resetTime))
        } else {
            this
        }
    }
}
