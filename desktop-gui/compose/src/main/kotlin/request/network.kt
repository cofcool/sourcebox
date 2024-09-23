package request

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

val logger = LoggerFactory.getLogger("request.network")

class Request {

    private fun baseUrl() = "http://localhost:38080"

    private var client: HttpClient = buildClient()

    private fun buildClient(): HttpClient {
        return HttpClient {
            defaultRequest {
                url(baseUrl())
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

    suspend fun getAllTools(): Set<String> {
        val r = client.get("/")
        return r.body<Set<String>>()
    }

    suspend fun invokeTool(tools: Tools, input: Params): String {
        val r = client.post("/${tools.tool!!.cmd}") {
            setBody(Params(tools.cmd, input.input))
        }
        return r.body<String>()
    }
}
