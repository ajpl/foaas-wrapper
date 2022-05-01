package com.arielpozo.classes

import com.arielpozo.dataclasses.FoaasResponse
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*


const val FOAAS_ENDPOINT = "https://www.foaas.com/yeah/ari"


class APIClient(engine: HttpClientEngine = CIO.create()) {
    private val engine = engine
    suspend fun callFoaas(): FoaasResponse {
        try {
            HttpClient(this.engine) {
                install(HttpCache)
                install(HttpRequestRetry){
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