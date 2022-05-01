package com.arielpozo

import com.arielpozo.endpoints.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        index()
        message()
    }.start(wait = true)
}
