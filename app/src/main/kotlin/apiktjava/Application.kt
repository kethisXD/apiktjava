package apiktjava

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import io.ktor.serialization.kotlinx.json.json

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module(itemRepository: ItemRepository = ItemRepository()) {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = false
            ignoreUnknownKeys = true
        })
    }

    routing {
        get("/") {
            call.respondText("Ktor API is running")
        }

        route("/items") {
            get {
                val ownerFilter = call.request.queryParameters["owner"]
                val items = itemRepository.all(ownerFilter)
                call.respond(items)
            }

            get("{id}") {
                val id = call.parameters["id"]
                if (id.isNullOrBlank()) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Item id is required"))
                    return@get
                }

                val item = itemRepository.find(id)
                if (item == null) {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("Item with id $id not found"))
                } else {
                    call.respond(item)
                }
            }

            post {
                val request = runCatching { call.receive<CreateItemRequest>() }
                    .getOrElse {
                        call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid item payload"))
                        return@post
                    }

                if (request.name.isBlank() || request.owner.isBlank()) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse("Both name and owner must be provided"),
                    )
                    return@post
                }

                val created = itemRepository.create(request)
                call.respond(HttpStatusCode.Created, created)
            }

            delete("{id}") {
                val id = call.parameters["id"]
                if (id.isNullOrBlank()) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Item id is required"))
                    return@delete
                }

                val removed = itemRepository.delete(id)
                if (removed) {
                    call.respond(HttpStatusCode.NoContent)
                } else {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("Item with id $id not found"))
                }
            }
        }
    }
}

@Serializable
data class Item(
    val id: String,
    val name: String,
    val owner: String,
    val description: String? = null,
)

@Serializable
data class CreateItemRequest(
    val name: String,
    val owner: String,
    val description: String? = null,
)

@Serializable
data class ErrorResponse(val message: String)

class ItemRepository {
    private val items = ConcurrentHashMap<String, Item>()

    fun all(ownerFilter: String?): List<Item> =
        items.values
            .asSequence()
            .filter { ownerFilter.isNullOrBlank() || it.owner.equals(ownerFilter, ignoreCase = true) }
            .sortedBy { it.name }
            .toList()

    fun find(id: String): Item? = items[id]

    fun create(request: CreateItemRequest): Item {
        val id = UUID.randomUUID().toString()
        val item = Item(id = id, name = request.name.trim(), owner = request.owner.trim(), description = request.description?.takeIf { it.isNotBlank() })
        items[id] = item
        return item
    }

    fun delete(id: String): Boolean = items.remove(id) != null
}
