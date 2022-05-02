package com.arielpozo.endpoints

import com.arielpozo.classes.APIClient
import com.arielpozo.classes.IllegalHTTPCodeException
import com.arielpozo.classes.RateLimitHeaders
import com.arielpozo.classes.RateLimiter
import com.arielpozo.dataclasses.ErrorResponse
import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.client.plugins.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.time.Instant

val applicationAPIClient = APIClient()
val rateLimiter: RateLimiter<Any> = RateLimiter()
fun Application.message(ApiClient: APIClient = applicationAPIClient) {
    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
        }
    }
    install(Authentication) {
        basic("auth-basic") {
            realm = "Access to the message endpoint"
            validate { credentials ->
                if (credentials.name == credentials.password) {
                    UserIdPrincipal(credentials.name)
                } else {
                    null
                }
            }
        }
    }
    // YXJpZWw6YXJpZWw=
    routing {
        authenticate("auth-basic") {
            get("/message") {
                try {
                    val rate = rateLimiter.useQuota(call.principal<UserIdPrincipal>()?.name!!)
                    if (rate.isQuotaEmpty()) {
                        val retryTime: Long =
                            (rate.resetInstant.epochSecond - Instant.now().epochSecond).coerceAtLeast(0)
                        call.response.headers.append(RateLimitHeaders.HEADER_RETRY.value, retryTime.toString())
                        call.respond(HttpStatusCode.TooManyRequests)
                    } else {
                        call.response.headers.append(RateLimitHeaders.HEADER_REMAINING.value, rate.quota.toString())
                        call.response.headers.append(RateLimitHeaders.HEADER_LIMIT.value, rateLimiter.quota.toString())
                        call.response.headers.append(
                            RateLimitHeaders.HEADER_RESET.value,
                            rate.resetInstant.epochSecond.toString()
                        )
                        call.respond(ApiClient.callFoaas())
                    }
                } catch (ex: Exception) {
                    when (ex) {
                        is RedirectResponseException, is ClientRequestException, is ServerResponseException, is ResponseException -> {
                            call.respond(
                                HttpStatusCode.UnprocessableEntity,
                                ErrorResponse("The server did not return a successful response")
                            )
                        }
                        is IllegalHTTPCodeException -> {
                            call.respond(
                                HttpStatusCode.UnprocessableEntity,
                                ErrorResponse("The server does not give a fuck about RFC7231")
                            )
                        }
                        else -> {
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                ErrorResponse("There was an unexpected error")
                            )
                        }
                    }
                }
            }
        }
    }
}
