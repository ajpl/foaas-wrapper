package com.arielpozo

import com.arielpozo.classes.APIClient
import com.arielpozo.classes.IllegalHTTPCodeException
import com.arielpozo.dataclasses.ErrorResponse
import com.arielpozo.dataclasses.FoaasResponse
import io.ktor.http.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlin.test.*
import io.ktor.server.testing.*
import com.arielpozo.endpoints.*
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.client.plugins.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking

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
            client.get("/message").apply {
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
            client.get("/message").apply {
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
                client.get("/message").apply {
                    val expected = ErrorResponse("The server does not give a fuck about RF7231")
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
            client.get("/message").apply {
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
            client.get("/message").apply {
                val expected = ErrorResponse("There was an unexpected error")
                val response = jacksonObjectMapper().readValue<ErrorResponse>(bodyAsText())
                assertEquals(HttpStatusCode.InternalServerError, status)
                assertEquals(expected.message, response.message)
            }
        }
    }


}