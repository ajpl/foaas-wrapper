package com.arielpozo.endpoints

import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*


fun Application.message() {
    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
        }
    }
    routing {
        get("/message") {
            call.respond(callFoaas())
        }
    }
}

data class FoaasResponse(val message: String, val subtitle: String)


fun callFoaas(): FoaasResponse {
    return FoaasResponse("hello","bye")
}