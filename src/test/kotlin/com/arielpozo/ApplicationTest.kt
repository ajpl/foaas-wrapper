package com.arielpozo

import io.ktor.http.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlin.test.*
import io.ktor.server.testing.*
import com.arielpozo.endpoints.*
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import index
import io.mockk.every
import io.mockk.mockkStatic

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
    fun testMessage() = testApplication {
        application {
            message()
        }
        client.get("/message").apply {
            val expected = FoaasResponse("hello", "bye")
            val response = jacksonObjectMapper().readValue<FoaasResponse>(bodyAsText())
            assertEquals(HttpStatusCode.OK, status)
            assertEquals(expected.message, response.message)
            assertEquals(expected.subtitle, response.subtitle)
        }
    }
}