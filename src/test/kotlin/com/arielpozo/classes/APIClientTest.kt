package com.arielpozo.classes

import com.arielpozo.dataclasses.FoaasResponse
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith


internal class APIClientTest {


    @Test
    fun `Remote server response is the expected one`() {
        runBlocking {
            val mockEngine = MockEngine { _ ->
                respond(
                    content = ByteReadChannel("""{"message":"Fuck YEAH!", "subtitle": "- ari"}"""),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }
            val apiClient = APIClient(mockEngine)
            val expected = FoaasResponse("Fuck YEAH!", "- ari")
            val response = apiClient.callFoaas()
            assertEquals(expected.message, response.message)
            assertEquals(expected.subtitle, response.subtitle)
        }
    }

    @Test
    fun `Remote server response is not the expected one`() {
        runBlocking {
            val mockEngine = MockEngine { _ ->
                respond(
                    content = ByteReadChannel("""{"m3ssage":"Fuck YEAH!", "subtitle": "- ari"}"""),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }
            val apiClient = APIClient(mockEngine)
            val expected = FoaasResponse("", "- ari")
            val response = apiClient.callFoaas()
            assertEquals(expected.message, response.message)
            assertEquals(expected.subtitle, response.subtitle)
        }
    }

    @Test
    fun `Remote server is having internal errors`() {
        runBlocking {
            val mockEngine = MockEngine { _ ->
                respond(
                    content = ByteReadChannel(""""""),
                    status = HttpStatusCode.InternalServerError,
                )
            }
            assertFailsWith<ServerResponseException>{
                val apiClient = APIClient(mockEngine)
                apiClient.callFoaas()
            }
        }
    }

    @Test
    fun `Remote server response is an unofficial HTTP Status Code`() {
        runBlocking {
            val mockEngine = MockEngine { _ ->
                respond(
                    content = ByteReadChannel("""{"message":"something something!", "subtitle": "not happening"}"""),
                    status = HttpStatusCode(622, "The server does not care for RFCs"),
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }
            assertFailsWith<IllegalHTTPCodeException> {
                val apiClient = APIClient(mockEngine)
                apiClient.callFoaas()
            }
        }
    }

}