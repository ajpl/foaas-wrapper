package com.arielpozo

import com.arielpozo.classes.APIClient
import com.arielpozo.classes.DEFAULT_RATE_LIMIT
import com.arielpozo.classes.IllegalHTTPCodeException
import com.arielpozo.classes.RateLimitHeaders
import com.arielpozo.dataclasses.ErrorResponse
import com.arielpozo.dataclasses.FoaasResponse
import com.arielpozo.endpoints.index
import com.arielpozo.endpoints.message
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApplicationTest {
    @Test
    fun testRoot() = testApplication {
        application {
            index()
        }
        client.get("/").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("Hello World!", bodyAsText())
        }
    }

    @Test
    fun `message with healthy remote server`() {
        testApplication {
            val mockAPI = mockk<APIClient>()
            every { runBlocking { mockAPI.callFoaas() } } returns FoaasResponse("asd", "dff")
            application {
                message(mockAPI)
            }
            client.get("/message") {
                headers {
                    append(HttpHeaders.Authorization, "Basic dGVzdDE6dGVzdDE=")
                }
            }.apply {
                val expected = FoaasResponse("asd", "dff")
                val response = jacksonObjectMapper().readValue<FoaasResponse>(bodyAsText())
                assertEquals(HttpStatusCode.OK, status)
                assertEquals(expected.message, response.message)
                assertEquals(expected.subtitle, response.subtitle)
            }
        }
    }

    @Test
    fun `message with one null field in the remote server response`() {
        testApplication {
            val mockAPI = mockk<APIClient>()
            every { runBlocking { mockAPI.callFoaas() } } returns FoaasResponse("", "dff")
            application {
                message(mockAPI)
            }
            client.get("/message") {
                headers {
                    append(HttpHeaders.Authorization, "Basic dGVzdDM6dGVzdDM=")
                }
            }.apply {
                val expected = FoaasResponse("", "dff")
                val response = jacksonObjectMapper().readValue<FoaasResponse>(bodyAsText())
                assertEquals(HttpStatusCode.OK, status)
                assertEquals(expected.message, response.message)
                assertEquals(expected.subtitle, response.subtitle)
            }
        }
    }

    @Test
    fun `message with remote server playing around`() {
        testApplication {
            val mockAPI = mockk<APIClient>()
            every { runBlocking { mockAPI.callFoaas() } }.throws(IllegalHTTPCodeException())
            application {
                message(mockAPI)
            }
                client.get("/message") {
                    headers {
                        append(HttpHeaders.Authorization, "Basic dGVzdDQ6dGVzdDQ=")
                    }
                }.apply {
                    val expected = ErrorResponse("The server does not give a fuck about RFC7231")
                    val response = jacksonObjectMapper().readValue<ErrorResponse>(bodyAsText())
                    assertEquals(HttpStatusCode.UnprocessableEntity, status)
                    assertEquals(expected.message, response.message)
                }
            }
        }
    @Test
    fun `message with remote server failing`() {
        testApplication {
            val mockAPI = mockk<APIClient>()
            val mockEx = mockk<ServerResponseException>()
            every { runBlocking { mockAPI.callFoaas() }}.throws(mockEx)
            application {
                message(mockAPI)
            }
            client.get("/message") {
                headers {
                    append(HttpHeaders.Authorization, "Basic dGVzdDp0ZXN0")
                }
            }.apply {
                val expected = ErrorResponse("The server did not return a successful response")
                val response = jacksonObjectMapper().readValue<ErrorResponse>(bodyAsText())
                assertEquals(HttpStatusCode.UnprocessableEntity, status)
                assertEquals(expected.message, response.message)
            }
        }
    }

    @Test
    fun `message with unknown error`() {
        testApplication {
            val mockAPI = mockk<APIClient>()
            val mockEx = mockk<Exception>()
            every { runBlocking { mockAPI.callFoaas() }}.throws(mockEx)
            application {
                message(mockAPI)
            }
            client.get("/message") {
                headers {
                    append(HttpHeaders.Authorization, "Basic dGVzdDU6dGVzdDU=")
                }
            }.apply {
                val expected = ErrorResponse("There was an unexpected error")
                val response = jacksonObjectMapper().readValue<ErrorResponse>(bodyAsText())
                assertEquals(HttpStatusCode.InternalServerError, status)
                assertEquals(expected.message, response.message)
            }
        }
    }

    @Test
    fun `message rate limit headers`() {
        testApplication {
            val mockAPI = mockk<APIClient>()
            every { runBlocking { mockAPI.callFoaas() } } returns FoaasResponse("asd", "dff")
            application {
                message(mockAPI)
            }
                client.get("/message") {
                    headers {
                        append(HttpHeaders.Authorization, "Basic cmF0ZTE6cmF0ZTE=")
                    }
                }.apply {
                    assertEquals(headers[RateLimitHeaders.HEADER_REMAINING.value]!!.toInt(), DEFAULT_RATE_LIMIT - 1)
                    assertEquals(headers[RateLimitHeaders.HEADER_LIMIT.value]!!.toInt(), DEFAULT_RATE_LIMIT)
                    assertTrue(headers.contains(RateLimitHeaders.HEADER_RESET.value))
            }
        }
    }

    @Test
    fun `message is rate limited`() {
        testApplication {
            val mockAPI = mockk<APIClient>()
            every { runBlocking { mockAPI.callFoaas() } } returns FoaasResponse("asd", "dff")
            application {
                message(mockAPI)
            }
            for (iter in 1..DEFAULT_RATE_LIMIT+1) {
                client.get("/message") {
                    headers {
                        append(HttpHeaders.Authorization, "Basic cmF0ZTpyYXRl")
                    }
                }.apply {
                    if (iter > DEFAULT_RATE_LIMIT) {
                        assertEquals(HttpStatusCode.TooManyRequests, status)
                        assertTrue(headers.contains(RateLimitHeaders.HEADER_RETRY.value))
                    }
                }
            }
        }
    }


}