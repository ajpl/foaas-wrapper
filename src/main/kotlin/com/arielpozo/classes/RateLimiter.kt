package com.arielpozo.classes

import com.arielpozo.dataclasses.Rate
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

const val DEFAULT_RATE_LIMIT_DURATION_IN_SECONDS: Long = 10
const val DEFAULT_RATE_LIMIT: Int = 10
const val MAX_HASHMAP_SIZE: Long = 1000
const val HASHMAP_RESET_TIME_IN_MINUTES: Long = 10

class RateLimiter<T : Any>(val quota: Int = DEFAULT_RATE_LIMIT, val resetDuration: Duration = Duration.ofSeconds(DEFAULT_RATE_LIMIT_DURATION_IN_SECONDS)) {
    private val activity: ConcurrentMap<T, Rate> = ConcurrentHashMap()
    private val lastGeneralReset: Instant = Instant.now()

    fun useQuota(key: T): Rate {
        activity.compute(key) { _, keyRate: Rate? ->
            val currentRate: Rate = keyRate?.reset(quota, resetDuration) ?: getNewRate()
            currentRate.useQuota()
        }
        if (shouldReset()) {
            doGeneralReset()
        }
        return activity[key]!!
    }

    private fun shouldReset(): Boolean {
        val isTimeLimit: Boolean = Duration.between(lastGeneralReset, Instant.now()) >= Duration.ofMinutes(
            HASHMAP_RESET_TIME_IN_MINUTES
        )
        return activity.size > MAX_HASHMAP_SIZE && isTimeLimit
    }

    private fun doGeneralReset() {
        val activityToRemove: Collection<T> = activity.filter { it.value.canReset() }.keys
        activityToRemove.forEach {
            activity.remove(it)
        }
    }

    private fun getNewRate(): Rate {
        return Rate(quota, Instant.now() + resetDuration)
    }
}
