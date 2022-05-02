package com.arielpozo

import com.arielpozo.endpoints.index
import com.arielpozo.endpoints.message
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        index()
        message()
    }.start(wait = true)
}
