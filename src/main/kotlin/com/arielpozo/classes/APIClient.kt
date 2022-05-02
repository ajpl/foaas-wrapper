package com.arielpozo.classes

import com.arielpozo.dataclasses.FoaasResponse
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode

const val FOAAS_ENDPOINT = "https://www.foaas.com/yeah/ari"

class APIClient(engine: HttpClientEngine = CIO.create()) {
    private val engine = engine
    suspend fun callFoaas(): FoaasResponse {
        try {
            HttpClient(this.engine) {
                install(HttpCache)
                install(HttpRequestRetry) {
                    retryOnServerErrors(maxRetries = 3)
                    exponentialDelay()
                }
                expectSuccess = true
            }.use { client ->
                val response: HttpResponse = client.get(FOAAS_ENDPOINT) {
                    headers {
                        append(HttpHeaders.Accept, "application/json")
                    }
                }
                return jacksonObjectMapper().readValue<FoaasResponse>(response.bodyAsText())
            }
        } catch (ex: ResponseException) {
            if (ex.response.status !in HttpStatusCode.allStatusCodes) {
                throw IllegalHTTPCodeException()
            } else {
                throw ex
            }
        }
    }
}
