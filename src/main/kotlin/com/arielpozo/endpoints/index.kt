package com.arielpozo.endpoints

import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.plugins.origin
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun Application.index() {

    routing {
        get("/") {
            call.application.environment.log.info("[${call.request.origin.remoteHost}] - Accessed to \"/\"")
            call.respondText("Hello World!")
        }
    }
}
