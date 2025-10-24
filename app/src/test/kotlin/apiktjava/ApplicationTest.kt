package apiktjava

import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ApplicationTest {
    @Test
    fun `server starts and root route responds`() = testApplication {
        application {
            module(ItemRepository())
        }

        val response = client.get("/")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Ktor API is running", response.body())
    }

    @Test
    fun `GET items returns JSON list`() = testApplication {
        application {
            module(ItemRepository())
        }

        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        val response = client.get("/items")
        assertEquals(HttpStatusCode.OK, response.status)
        assertJsonContentType(response)
        val items: List<Item> = response.body()
        assertTrue(items.isEmpty())
    }

    @Test
    fun `POST creates item and GET by id returns JSON payload`() = testApplication {
        application {
            module(ItemRepository())
        }

        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        val createResponse = client.post("/items") {
            contentType(ContentType.Application.Json)
            setBody(CreateItemRequest(name = "Sprocket", owner = "alice", description = "Demo item"))
        }

        assertEquals(HttpStatusCode.Created, createResponse.status)
        val created: Item = createResponse.body()
        assertJsonContentType(createResponse)

        val getResponse = client.get("/items/${created.id}")
        assertEquals(HttpStatusCode.OK, getResponse.status)
        assertJsonContentType(getResponse)
        assertEquals(created, getResponse.body())
    }

    @Test
    fun `GET items supports owner filter`() = testApplication {
        application {
            module(ItemRepository())
        }

        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        repeat(2) {
            client.post("/items") {
                contentType(ContentType.Application.Json)
                setBody(CreateItemRequest(name = "Widget-$it", owner = if (it == 0) "alice" else "bob"))
            }
        }

        val response = client.get("/items?owner=alice")
        assertEquals(HttpStatusCode.OK, response.status)
        assertJsonContentType(response)

        val items: List<Item> = response.body()
        assertEquals(1, items.size)
        assertTrue(items.all { it.owner.equals("alice", ignoreCase = true) })
    }

    @Test
    fun `POST with invalid payload returns 400 and JSON error`() = testApplication {
        application {
            module(ItemRepository())
        }

        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        val response = client.post("/items") {
            contentType(ContentType.Application.Json)
            setBody(CreateItemRequest(name = "", owner = ""))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertJsonContentType(response)
        val error: ErrorResponse = response.body()
        assertTrue(error.message.contains("name", ignoreCase = true))
    }

    @Test
    fun `GET missing item returns 404 with JSON error`() = testApplication {
        application {
            module(ItemRepository())
        }

        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        val response = client.get("/items/missing-id")
        assertEquals(HttpStatusCode.NotFound, response.status)
        assertJsonContentType(response)
        val error: ErrorResponse = response.body()
        assertTrue(error.message.contains("missing-id"))
    }

    @Test
    fun `DELETE removes existing item and missing delete returns 404`() = testApplication {
        application {
            module(ItemRepository())
        }

        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        val created: Item = client.post("/items") {
            contentType(ContentType.Application.Json)
            setBody(CreateItemRequest(name = "ToDelete", owner = "tester"))
        }.body()

        val deleteResponse = client.delete("/items/${created.id}")
        assertEquals(HttpStatusCode.NoContent, deleteResponse.status)
        assertNull(deleteResponse.headers[HttpHeaders.ContentType])

        val deleteMissing = client.delete("/items/${created.id}")
        assertEquals(HttpStatusCode.NotFound, deleteMissing.status)
        assertJsonContentType(deleteMissing)
        val error: ErrorResponse = deleteMissing.body()
        assertTrue(error.message.contains(created.id))
    }

    private fun assertJsonContentType(response: HttpResponse) {
        val header = response.headers[HttpHeaders.ContentType]
        assertNotNull(header)
        val parsed = ContentType.parse(header)
        assertEquals(ContentType.Application.Json, parsed.withoutParameters())
    }
}
