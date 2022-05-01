package com.arielpozo.endpoints

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.index() {

    routing {
        get("/") {
            call.respondText("Hello World!")
        }
    }
}
