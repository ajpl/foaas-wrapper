package com.arielpozo.classes

// https://tools.ietf.org/id/draft-polli-ratelimit-headers-00.html
enum class RateLimitHeaders(val value: String) {
    HEADER_LIMIT("X-RateLimit-Limit"),
    HEADER_REMAINING("X-RateLimit-Remaining"),
    HEADER_RESET("X-RateLimit-Reset"),
    HEADER_RETRY("Retry-After")
}