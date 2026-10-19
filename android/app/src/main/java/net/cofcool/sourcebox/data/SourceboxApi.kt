package net.cofcool.sourcebox.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

@Serializable
data class ToolArgument(
    val key: String,
    val description: String = "",
    val defaultValue: String = "",
    val required: Boolean = false,
    val demo: String = ""
)

data class ToolInfo(
    val id: String,
    val description: String,
    val arguments: List<ToolArgument>
)

@Serializable
data class NoteRecord(
    val id: String = "",
    val content: String = "",
    val date: String? = null,
    val state: String = "NORMAL"
)

@Serializable
data class TodoRecord(
    val id: String = "",
    val name: String = "",
    val state: String = "todo",
    val remark: String? = null,
    val type: String = "todo",
    val start: String? = null,
    val end: String? = null
)

@Serializable
data class CommandRecord(
    val id: String? = null,
    val cmd: String,
    val alias: String? = null,
    val tags: List<String> = emptyList(),
    val remark: String? = null,
    val frequency: Int? = null
)

@Serializable
data class ToolEvent(
    val source: JsonElement? = null,
    val tool: String = "",
    val action: String = "",
    val success: Boolean = true
)

private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
    explicitNulls = false
}

class SourceboxApi(context: Context) {
    private val preferences = context.getSharedPreferences("sourcebox", Context.MODE_PRIVATE)
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    fun baseUrl(): String = preferences.getString("base_url", "").orEmpty().trim().trimEnd('/')

    fun saveConnection(baseUrl: String, username: String, password: String) {
        preferences.edit()
            .putString("base_url", baseUrl.trim().trimEnd('/'))
            .putString("username", username.trim())
            .putString("password", password)
            .apply()
    }

    fun username(): String = preferences.getString("username", "").orEmpty()

    fun password(): String = preferences.getString("password", "").orEmpty()

    suspend fun tools(): List<ToolInfo> {
        val names = json.decodeFromString<List<String>>(request("/"))
        val help = json.parseToJsonElement(request("/help")).jsonObject
        val config = json.parseToJsonElement(request("/config")).jsonObject
        return names.map { rawName ->
            val id = rawName.substringBefore(':').trim()
            val details = help[id]?.jsonObject
            val description = details?.get("desc")?.jsonPrimitive?.contentOrNull
                ?.substringAfter(':', "")?.trim().orEmpty()
            val helpText = details?.get("help")?.jsonPrimitive?.contentOrNull.orEmpty()
            val arguments = parseArguments(helpText).map { argument ->
                val configured = config.entries.firstOrNull { (key, _) ->
                    key.startsWith("$id.") && (key == "${id}.${argument.key}" || key.endsWith(".${argument.key}"))
                }?.value
                val configuredValue = when (configured) {
                    is JsonObject -> configured["val"]?.jsonPrimitive?.contentOrNull
                    is JsonPrimitive -> configured.contentOrNull
                    else -> null
                }
                argument.copy(defaultValue = configuredValue ?: argument.defaultValue)
            }
            ToolInfo(id, description, arguments)
        }
    }

    suspend fun executeTool(toolId: String, values: Map<String, String>): String {
        val body = buildJsonObject {
            values.filterValues { it.isNotEmpty() }.forEach { (key, value) -> put(key, value) }
        }
        request("/${Uri.encode(toolId)}", method = "POST", body = json.encodeToString(body))

        val output = mutableListOf<String>()
        val deadline = System.currentTimeMillis() + 60_000
        while (System.currentTimeMillis() < deadline) {
            val events = json.decodeFromString<List<ToolEvent>>(request("/event"))
            for (event in events.filter { it.tool == toolId }) {
                if (event.action == "finished") {
                    if (!event.success) {
                        throw IOException(event.source?.displayValue() ?: "工具执行失败")
                    }
                    if (output.isEmpty()) {
                        return event.source?.displayValue()?.takeIf { it != "success" }.orEmpty()
                            .ifBlank { "执行完成" }
                    }
                    return output.joinToString("\n")
                }
                if (event.action == "write" || event.action == "writeFile") {
                    event.source?.displayValue()?.let(output::add)
                }
            }
            delay(600)
        }
        throw IOException("等待工具执行结果超时")
    }

    suspend fun notes(): List<NoteRecord> =
        json.decodeFromString<List<NoteRecord>>(request("/note/list"))
            .filter { it.state == "NORMAL" }

    suspend fun saveNote(note: NoteRecord): NoteRecord {
        val date = java.time.LocalDateTime.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        val updated = note.copy(date = date, state = "NORMAL")
        return json.decodeFromString(request("/note/note", "POST", json.encodeToString(updated)))
    }

    suspend fun deleteNote(id: String) {
        request("/note/note/${Uri.encode(id)}", "DELETE")
    }

    suspend fun todos(query: String = ""): List<TodoRecord> {
        val path = buildString {
            append("/action?state=todo&type=todo")
            if (query.isNotBlank()) append("&name=${Uri.encode(query)}")
        }
        return json.decodeFromString(request(path))
    }

    suspend fun addTodo(name: String, remark: String) {
        val now = java.time.LocalDateTime.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        val body = buildJsonObject {
            put("name", name)
            put("remark", remark)
            put("state", "todo")
            put("type", "todo")
            put("start", now)
            put("end", now)
        }
        request("/action", "POST", json.encodeToString(body))
    }

    suspend fun completeTodo(id: String) {
        val body = buildJsonObject {
            put("id", id)
            put("state", "done")
        }
        request("/action", "POST", json.encodeToString(body))
    }

    suspend fun commands(query: String): List<CommandRecord> {
        val path = if (query.isBlank()) "/cmd/quick" else "/cmd/quick?q=${Uri.encode(query)}"
        return json.decodeFromString(request(path))
    }

    suspend fun saveCommand(command: CommandRecord) {
        request("/cmd", "POST", json.encodeToString(command))
    }

    suspend fun deleteCommand(id: String) {
        request("/cmd/${Uri.encode(id)}", "DELETE")
    }

    suspend fun storeCommands() {
        request("/cmd/store/ALL")
    }

    private suspend fun request(path: String, method: String = "GET", body: String? = null): String =
        withContext(Dispatchers.IO) {
            val base = baseUrl()
            if (base.isBlank()) throw IOException("请先在设置中配置后端根地址")
            val builder = Request.Builder()
                .url("$base$path")
                .header("Accept", "application/json")
            if (body != null) {
                builder.method(
                    method,
                    body.toRequestBody("application/json; charset=utf-8".toMediaType())
                )
            } else if (method != "GET") {
                builder.method(method, null)
            }
            val user = username()
            if (user.isNotBlank()) {
                builder.header("Authorization", Credentials.basic(user, password()))
            }
            client.newCall(builder.build()).execute().use { response ->
                val content = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw IOException("HTTP ${response.code}: ${content.take(300)}")
                }
                content
            }
        }

    private fun parseArguments(help: String): List<ToolArgument> {
        val requiredKeys = Regex("(?m)^Synopsis\\s*\\n\\s*([^\\n]+)")
            .find(help)?.groupValues?.get(1)
            ?.let { synopsis ->
                Regex("--([\\w-]+)=").findAll(synopsis)
                    .filter { match -> synopsis.substring(0, match.range.first).count { it == '[' } ==
                        synopsis.substring(0, match.range.first).count { it == ']' } }
                    .map { it.groupValues[1] }
                    .toSet()
            }.orEmpty()

        val descriptionSection = help.substringAfter("\nDescription\n", help)
            .substringBefore("\nAlias\n")
        return Regex("(?m)^\\s+--([\\w-]+)\\s{2,}(.+)$")
            .findAll(descriptionSection)
            .map { match ->
                val key = match.groupValues[1]
                val raw = match.groupValues[2].trim()
                val default = raw.substringAfter(". Default: ", "").takeIf { it != raw }.orEmpty()
                val demo = raw.substringAfter(". Example: ", "").takeIf { it != raw }.orEmpty()
                val description = raw.substringBefore(". Default: ")
                    .substringBefore(". Example: ")
                    .trim()
                ToolArgument(key, description, default, key in requiredKeys, demo)
            }
            .distinctBy { it.key }
            .toList()
    }

    private fun JsonElement.displayValue(): String =
        if (this is kotlinx.serialization.json.JsonPrimitive && isString) content else toString()

}
