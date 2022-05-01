package com.arielpozo.endpoints

import com.arielpozo.classes.APIClient
import com.arielpozo.classes.IllegalHTTPCodeException
import com.arielpozo.dataclasses.ErrorResponse
import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.client.plugins.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

val applicationAPIClient = APIClient()
fun Application.message(ApiClient: APIClient = applicationAPIClient) {
    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
        }
    }
    routing {
        get("/message") {
            try {
                call.respond(ApiClient.callFoaas())
            } catch (ex: Exception){
                print(ex)
                when(ex) {
                    is RedirectResponseException, is ClientRequestException, is ServerResponseException, is ResponseException -> {
                        call.respond(HttpStatusCode.UnprocessableEntity, ErrorResponse("The server did not return a successful response"))
                    }
                    is IllegalHTTPCodeException -> {
                        call.respond(HttpStatusCode.UnprocessableEntity, ErrorResponse("The server does not give a fuck about RF7231"))
                    }
                    else -> {
                        call.respond(HttpStatusCode.InternalServerError, ErrorResponse("There was an unexpected error"))
                    }
                }
            }
        }
    }
}