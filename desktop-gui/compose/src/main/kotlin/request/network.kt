package request

import ConfigManager
import globalJson
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.selects.select
import kotlinx.serialization.json.Json
import loadServer
import org.slf4j.LoggerFactory
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlin.time.Duration.Companion.seconds

val logger = LoggerFactory.getLogger("request.network")
val actionEvents = Channel<Action>(Channel.UNLIMITED)
val trigger = Channel<Boolean>(Channel.RENDEZVOUS)
val msgDone = Channel<Boolean>(Channel.RENDEZVOUS)

val CFG = ConfigManager

class Request {

    private var baseUrl = CFG.requestUrl()

    private var client: HttpClient = buildClient()

    private fun buildClient(): HttpClient {
        if (CFG.needLocalServer()) {
            loadServer()
        }
        return HttpClient {
            defaultRequest {
                url(baseUrl)
            }
            install(ContentNegotiation) {
                json(Json {
                    encodeDefaults = true
                    isLenient = true
                    allowSpecialFloatingPointValues = true
                    allowStructuredMapKeys = true
                    prettyPrint = false
                    useArrayPolymorphism = false
                    ignoreUnknownKeys = true
                })
            }
            HttpResponseValidator {
                validateResponse { response ->
                    if (response.status.value !in (200..299)) {
                        logger.warn(
                            "HTTP error: ${response.status.value}: ${
                                response.bodyAsText(
                                    charset("utf-8")
                                )
                            }"
                        )
                        throw ResponseException(
                            response,
                            "HTTP error with status: ${response.status}"
                        )
                    }
                }
            }
        }
    }

    suspend fun getAllTools(): List<String> {
        val r = client.get("/")
        return r.body<List<String>>().sorted()
    }

    suspend fun getConfig(): Map<String, Arg> {
        delay(100L)
        val r = client.get("/config")
        return r.body<Map<String, Arg>>()
    }

    fun listNote(note: Note): List<Note> {
        return runBlocking {
            val r = client.get("/note/list")
            return@runBlocking r.body<List<Note>>()
        }.filter { it.state == note.state }.sortedByDescending { it.date }
    }

    fun saveNote(note: Note) {
        return runBlocking {
            client.post("/note/note") {
                contentType(ContentType.Application.Json)
                setBody(note)
            }
        }
    }

    fun deleteNote(note: Note) {
        if (note.id.isNotBlank()) {
            runBlocking {
                client.delete("/note/note/${note.id}")
            }
        }
    }

    fun listTodo(item: TodoItem?): List<TodoItem> {
        return runBlocking {
            var path = "/action?state=todo&type=todo"
            item?.apply{
                if (item.name.isNotBlank()) {
                    path = "${path}&name=${URLEncoder.encode(item.name, StandardCharsets.UTF_8)}"
                }
            }
            val r = client.get(path)
            return@runBlocking r.body<List<TodoItem>>()
        }
    }

    fun listCmd(q: String): List<CommandItem> {
        return runBlocking {
            var path = "/cmd/quick"
            if (q.isNotEmpty()) {
                var c = "cmd"
                if (q.startsWith("#") || q.startsWith("@")) {
                    c = "q"
                }
                path = "${path}?${c}=${URLEncoder.encode(q, StandardCharsets.UTF_8)}"
            }
            val r = client.get(path)
            return@runBlocking r.body<List<CommandItem>>()
        }
    }

    fun deleteCmd(id: String) {
        runBlocking {
            client.delete("/cmd/${id}")
        }
    }

    fun storeCmd() {
        runBlocking {
            client.get("/cmd/store/ALL")
        }
    }

    fun runTool(tool: Tools, data: Any) {
        runBlocking {
            client.post("/${tool.toolName()}") {
                contentType(ContentType.Application.Json)
                setBody(data)
            }
        }
    }

    fun runTool(path: String, data: Any) {
        runBlocking {
            client.post("/${path}") {
                contentType(ContentType.Application.Json)
                setBody(data)
            }
        }
    }

    fun readEvents(onBefore: () -> Unit = {}, onReceived: (event: Action, json: Json) -> Unit) {
        val result = trigger.trySend(true)
        if (result.isFailure) {
            logger.warn("Other action is running")
            return
        }
        onBefore()
        CoroutineScope(Dispatchers.IO).launch {
            while (!msgDone.receive()) {
                select<Unit> {
                    actionEvents.onReceive {
                        onReceived(it, globalJson)
                    }
                }
            }
        }
    }

    suspend fun getActionEvents(): List<Action> {
        val r = client.get("/event")
        return r.body<List<Action>>()
    }

    fun checkEvent() {
        CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                var flag = trigger.receive()
                while (flag) {
                    val a = getActionEvents()
                    if (a.isNotEmpty()) {
                        a.forEach {
                            if (it.action == "finished") {
                                flag = false
                                msgDone.send(true)
                                return@forEach
                            }
                            actionEvents.send(it)
                            msgDone.send(false)
                        }
                    }
                    delay(2.seconds)
                }
            }
        }
    }

    suspend fun invokeTool(tools: Tools, input: Params): String {
        val r = client.post("/${tools.tool!!.toolName()}") {
            setBody(Params(tools.toolName(), input.input))
        }
        return r.body<String>()
    }
}
