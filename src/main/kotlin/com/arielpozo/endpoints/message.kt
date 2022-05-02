package com.arielpozo.endpoints

import com.arielpozo.classes.APIClient
import com.arielpozo.classes.IllegalHTTPCodeException
import com.arielpozo.classes.RateLimitHeaders
import com.arielpozo.classes.RateLimiter
import com.arielpozo.dataclasses.ErrorResponse
import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.RedirectResponseException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.basic
import io.ktor.server.auth.principal
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.origin
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
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
                        call.application.environment.log.info("[${call.request.origin.remoteHost}] - Blocked on \"/messages\" because of Too Many Requests")
                        val retryTime: Long =
                            (rate.resetInstant.epochSecond - Instant.now().epochSecond).coerceAtLeast(0)
                        call.response.headers.append(RateLimitHeaders.HEADER_RETRY.value, retryTime.toString())
                        call.respond(HttpStatusCode.TooManyRequests)
                    } else {
                        call.application.environment.log.info("[${call.request.origin.remoteHost}] - Accessed to \"/messages\" | Remaining Quota: ${rate.quota}")
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
                            call.application.environment.log.error("[${call.request.origin.remoteHost}] - Accessed to \"/messages\" | Remote server failed")
                            call.respond(
                                HttpStatusCode.UnprocessableEntity,
                                ErrorResponse("The server did not return a successful response")
                            )
                        }
                        is IllegalHTTPCodeException -> {
                            call.application.environment.log.error("[${call.request.origin.remoteHost}] - Accessed to \"/messages\" | Remote server returned illegal HTTP status code")
                            call.respond(
                                HttpStatusCode.UnprocessableEntity,
                                ErrorResponse("The server does not give a fuck about RFC7231")
                            )
                        }
                        else -> {
                            call.application.environment.log.error("[${call.request.origin.remoteHost}] - Accessed to \"/messages\" | Internal server error")
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
