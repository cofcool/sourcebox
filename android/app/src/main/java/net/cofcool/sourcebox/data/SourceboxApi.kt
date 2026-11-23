package net.cofcool.sourcebox.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.SystemClock
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
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
import net.cofcool.sourcebox.CrashLogStore
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import java.io.IOException
import java.net.URI
import java.security.DigestInputStream
import java.security.MessageDigest
import java.util.Base64
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds

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
    val category: String? = null,
    val refs: String? = null,
    val start: String? = null,
    val end: String? = null
)

@Serializable
data class CommandRecord(
    val id: String? = null,
    val cmd: String,
    val alias: String? = null,
    val tags: List<String>? = emptyList(),
    val remark: String? = null,
    val frequency: Int? = null,
    val createTime: String? = null,
    val updateTime: String? = null
)

@Serializable
data class ActionRecord(
    val id: String = "",
    val name: String,
    val icon: String? = null,
    val index: String? = null,
    val device: String? = null,
    val type: String = "record",
    val state: String = "todo",
    val category: String? = null,
    val rating: Int? = null,
    val comments: List<String> = emptyList(),
    val labels: String? = "",
    val refs: String? = null,
    val remark: String? = null,
    val start: String? = null,
    val end: String? = null,
    val duration: Int? = null,
    val createTime: String? = null,
    val updateTime: String? = null
)

@Serializable
data class ActionComment(
    val id: String = "",
    val actionId: String = "",
    val content: String = "",
    val createTime: String? = null
)

@Serializable
data class ActionCheckList(
    val id: String = "",
    val actionId: String = "",
    val content: String = "",
    val first: Int = 0,
    val last: Int = 0,
    val current: Int = 0,
    val createTime: String? = null,
    val updateTime: String? = null
)

@Serializable
data class HabitDefinition(
    val id: String? = null,
    val actionId: String? = null,
    val category: String = "check",
    val note: String? = null,
    val unit: String? = null,
    val freqNum: Int? = null,
    val freqDen: Int? = null,
    val question: String? = null,
    val targetValue: Int? = null,
    val targetType: String? = null,
    val createTime: String? = null,
    val updateTime: String? = null
)

@Serializable
data class HabitRecord(
    val action: ActionRecord,
    val habit: HabitDefinition,
    val checkLists: List<ActionCheckList> = emptyList()
)

@Serializable
data class ActionTypeRecord(
    val id: String = "",
    val name: String = "",
    val type: String = "",
    val createTime: String? = null
)

@Serializable
data class ActionTypeResponse(
    val types: List<ActionTypeRecord> = emptyList(),
    val states: String = ""
)

@Serializable
data class ActionExport(
    val record: ActionRecord,
    val comments: List<ActionComment> = emptyList(),
    val checklists: List<ActionCheckList> = emptyList(),
    val other: JsonElement? = null,
    val habit: HabitDefinition? = null
)

@Serializable
data class HabitValueChange(
    val checklistId: String,
    val checklist: ActionCheckList
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
    private val crashLogStore = CrashLogStore(context.applicationContext)
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
            delay(600.milliseconds)
        }
        throw IOException("等待工具执行结果超时")
    }

    suspend fun exportAllNotes(): List<NoteRecord> =
        listOf()

    suspend fun saveNote(note: NoteRecord): NoteRecord {
        val updated = note.copy(
            id = note.id.ifBlank { UUID.randomUUID().toString() },
            date = note.date ?: java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
            state = "NORMAL"
        )
        return json.decodeFromString(request("/note/note", "POST", json.encodeToString(updated)))
    }

    suspend fun deleteNote(id: String) {
        request("/note/note/${Uri.encode(id)}", "DELETE")
    }

    suspend fun addTodo(action: ActionRecord) {
        request("/action", "POST", json.encodeToString(action))
    }

    suspend fun completeTodo(action: ActionRecord): ActionRecord =
        saveAction(action.copy(state = "done"))

    suspend fun saveCommand(command: CommandRecord): CommandRecord =
        json.decodeFromString(request("/cmd", "POST", json.encodeToString(command)))

    suspend fun deleteCommand(id: String) {
        request("/cmd/${Uri.encode(id)}", "DELETE")
    }

    suspend fun listLanShareFiles(receiverUrl: String, token: String, path: String): List<String> =
        withContext(Dispatchers.IO) {
            require(token.isNotBlank()) { "请输入接收端令牌" }
            val baseUrl = validateLanShareReceiver(receiverUrl)
            val listingUrl = "$baseUrl/download?path=${Uri.encode(path.trim().trim('/'))}"
            val request = Request.Builder()
                .url(listingUrl)
                .header("Authorization", "Bearer $token")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                val body = response.body.string()
                if (!response.isSuccessful) {
                    recordApiFailure("GET", "/download", response.code, body)
                    throw IOException("获取文件列表失败：HTTP ${response.code} ${body.take(300)}")
                }
                json.decodeFromString<List<String>>(body)
            }
        }

    suspend fun downloadLanShareFile(
        context: Context,
        receiverUrl: String,
        token: String,
        relativePath: String,
        onProgress: (String, Long, Long) -> Unit
    ): String = withContext(Dispatchers.IO) {
        require(token.isNotBlank()) { "请输入接收端令牌" }
        val baseUrl = validateLanShareReceiver(receiverUrl)
        val safePath = relativePath.replace('\\', '/').trimStart('/')
        require(safePath.isNotBlank() && safePath.split('/').none { it == ".." || it == "." || it.isBlank() }) {
            "无效的下载路径：$relativePath"
        }
        val encodedPath = safePath.split('/').joinToString("/") { Uri.encode(it) }
        val request = Request.Builder()
            .url("$baseUrl/download/$encodedPath")
            .header("Authorization", "Bearer $token")
            .get()
            .build()
        client.newBuilder()
            .readTimeout(30, TimeUnit.MINUTES)
            .callTimeout(30, TimeUnit.MINUTES)
            .build()
            .newCall(request)
            .execute()
            .use { response ->
                if (!response.isSuccessful) {
                    val message = response.body.string()
                    recordApiFailure("GET", "/download/$safePath", response.code, message)
                    throw IOException("下载 $safePath 失败：HTTP ${response.code} ${message.take(300)}")
                }
                val body = response.body
                val expectedSha256 = response.header("X-Sourcebox-SHA256")
                    ?.takeIf { it.matches(Regex("(?i)[0-9a-f]{64}")) }
                    ?: throw IOException("服务端未返回有效的 SHA-256：$safePath")
                val totalBytes = body.contentLength()
                val parentPath = safePath.substringBeforeLast('/', "")
                val relativeDirectory = buildString {
                    append(Environment.DIRECTORY_DOWNLOADS)
                    append("/Sourcebox")
                    if (parentPath.isNotBlank()) append("/$parentPath")
                }
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, safePath.substringAfterLast('/'))
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relativeDirectory)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val destination = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: throw IOException("无法在下载目录创建文件：$safePath")
                try {
                    val output = resolver.openOutputStream(destination, "w")
                        ?: throw IOException("无法写入下载文件：$safePath")
                    val digest = MessageDigest.getInstance("SHA-256")
                    body.byteStream().use { input ->
                        output.use { stream ->
                            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                            var transferred = 0L
                            var lastReportedAt = 0L
                            onProgress(safePath, 0, totalBytes)
                            while (true) {
                                val count = input.read(buffer)
                                if (count == -1) break
                                stream.write(buffer, 0, count)
                                digest.update(buffer, 0, count)
                                transferred += count
                                val now = SystemClock.elapsedRealtime()
                                if (now - lastReportedAt >= 100 || (totalBytes >= 0 && transferred >= totalBytes)) {
                                    onProgress(safePath, transferred, totalBytes)
                                    lastReportedAt = now
                                }
                            }
                            onProgress(safePath, transferred, totalBytes)
                        }
                    }
                    val actualSha256 = digest.digest().joinToString("") { "%02x".format(it) }
                    if (!MessageDigest.isEqual(
                            expectedSha256.lowercase().toByteArray(Charsets.US_ASCII),
                            actualSha256.toByteArray(Charsets.US_ASCII)
                        )
                    ) {
                        throw IOException("SHA-256 校验失败：$safePath，期望 $expectedSha256，实际 $actualSha256")
                    }
                    resolver.update(destination, ContentValues().apply {
                        put(MediaStore.MediaColumns.IS_PENDING, 0)
                    }, null, null)
                    "${Environment.DIRECTORY_DOWNLOADS}/Sourcebox/$safePath"
                } catch (error: Exception) {
                    resolver.delete(destination, null, null)
                    throw error
                }
            }
    }

    private fun validateLanShareReceiver(receiverUrl: String): String {
        val receiver = URI(receiverUrl.trim())
        require(
            receiver.scheme.equals("http", ignoreCase = true) &&
                !receiver.host.isNullOrBlank() && receiver.port in 1..65535 &&
                receiver.rawUserInfo == null && receiver.rawPath.orEmpty() in setOf("", "/") &&
                receiver.rawQuery == null && receiver.rawFragment == null
        ) {
            "接收地址必须是带端口的 HTTP 地址，例如 http://192.168.1.20:43127"
        }
        return URI("http", null, receiver.host, receiver.port, null, null, null).toASCIIString()
    }

    suspend fun uploadLanShareFile(context: Context, receiverUrl: String, token: String, uri: Uri): String =
        uploadLanShareFile(context, receiverUrl, token, uri) { _, _, _ -> }

    suspend fun uploadLanShareFile(
        context: Context,
        receiverUrl: String,
        token: String,
        uri: Uri,
        onProgress: (String, Long, Long) -> Unit
    ): String =
        withContext(Dispatchers.IO) {
            require(token.isNotBlank()) { "请输入接收端令牌" }
            val receiver = URI(receiverUrl.trim())
            require(
                receiver.scheme.equals("http", ignoreCase = true) &&
                    !receiver.host.isNullOrBlank() && receiver.port in 1..65535
            ) {
                "接收地址必须是带端口的 HTTP 地址，例如 http://192.168.1.20:43127"
            }
            val uploadUrl = URI(
                "http",
                null,
                receiver.host,
                receiver.port,
                "/upload",
                null,
                null
            ).toASCIIString()
            val displayName = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                }
                ?.takeIf(String::isNotBlank)
                ?: uri.lastPathSegment?.substringAfterLast('/')?.takeIf(String::isNotBlank)
                ?: "upload-${UUID.randomUUID()}"
            val size = context.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize }
                ?.takeIf { it >= 0 } ?: -1L
            onProgress(displayName, 0, size)
            val digest = MessageDigest.getInstance("SHA-256")
            val input = context.contentResolver.openInputStream(uri)
                ?: throw IOException("无法读取文件：$displayName")
            DigestInputStream(input, digest).use { stream ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (stream.read(buffer) != -1) {}
            }
            val sha256 = digest.digest().joinToString("") { "%02x".format(it) }
            val body = object : okhttp3.RequestBody() {
                override fun contentType() = "application/octet-stream".toMediaType()
                override fun contentLength() = size
                override fun writeTo(sink: BufferedSink) {
                    val input = context.contentResolver.openInputStream(uri)
                        ?: throw IOException("无法读取文件：$displayName")
                    input.use { stream ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var transferred = 0L
                        var lastReportedAt = 0L
                        while (true) {
                            val count = stream.read(buffer)
                            if (count == -1) break
                            sink.write(buffer, 0, count)
                            transferred += count
                            val now = SystemClock.elapsedRealtime()
                            if (now - lastReportedAt >= 100 || (size >= 0 && transferred >= size)) {
                                onProgress(displayName, transferred, size)
                                lastReportedAt = now
                            }
                        }
                        onProgress(displayName, transferred, size)
                    }
                }
            }
            val relativePath = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(displayName.toByteArray(Charsets.UTF_8))
            val request = Request.Builder()
                .url(uploadUrl)
                .header("Authorization", "Bearer $token")
                .header("X-Sourcebox-Transfer", "file")
                .header("X-Sourcebox-Path", relativePath)
                .header("X-Sourcebox-SHA256", sha256)
                .post(body)
                .build()
            client.newBuilder()
                .writeTimeout(30, TimeUnit.MINUTES)
                .readTimeout(30, TimeUnit.MINUTES)
                .callTimeout(30, TimeUnit.MINUTES)
                .build()
                .newCall(request)
                .execute()
                .use { response ->
                    val message = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        recordApiFailure("POST", "/upload", response.code, message)
                        throw IOException("上传 $displayName 失败：HTTP ${response.code} ${message.take(300)}")
                    }
                    "$displayName：${message.ifBlank { "上传成功" }}"
                }
        }

    suspend fun exportAllActions(): List<ActionExport> =
        json.decodeFromString<List<ActionExport>>(request("/action/exportAll"))

    suspend fun actionTypes(): ActionTypeResponse =
        json.decodeFromString(request("/action/types"))

    suspend fun findRecordCandidates(name: String): List<ActionRecord> {
        val query = Uri.encode(name.trim())
        return json.decodeFromString(
            request("/action?type=record&name=$query&page=1&size=10")
        )
    }

    suspend fun saveHabit(habit: HabitRecord): ActionRecord =
        json.decodeFromString(request("/action/habit", "POST", json.encodeToString(habit)))

    suspend fun findHabitRecord(actionId: String): HabitRecord =
        json.decodeFromString(request("/action/habit/${Uri.encode(actionId)}"))

    suspend fun saveHabitValue(actionId: String, checklist: ActionCheckList): ActionCheckList =
        json.decodeFromString(
            request("/action/habit/${Uri.encode(actionId)}", "POST", json.encodeToString(checklist))
        )

    suspend fun deleteHabit(actionId: String) {
        request("/action/habit/${Uri.encode(actionId)}", "DELETE")
    }

    suspend fun saveRecord(record: ActionRecord): ActionRecord {
        val payload = record.copy(type = "record", labels = record.labels.orEmpty())
        return saveAction(payload)
    }

    suspend fun saveAction(action: ActionRecord): ActionRecord =
        json.decodeFromString(request("/action", "POST", json.encodeToString(action)))

    suspend fun addRecordComment(actionId: String, comment: ActionComment): ActionComment {
        return json.decodeFromString(
            request("/action/comment/${Uri.encode(actionId)}", "POST", json.encodeToString(comment))
        )
    }

    suspend fun saveRecordChecklist(actionId: String, checklist: ActionCheckList): ActionCheckList =
        json.decodeFromString(
            request("/action/checklist/${Uri.encode(actionId)}", "POST", json.encodeToString(checklist))
        )

    suspend fun deleteRecordChecklist(id: String) {
        request("/action/checklist/${Uri.encode(id)}", "DELETE")
    }

    suspend fun uploadTextFile(fileName: String, content: String): String =
        withContext(Dispatchers.IO) {
            val base = baseUrl()
            if (base.isBlank()) throw IOException("请先在设置中配置后端根地址")
            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    fileName,
                    content.toRequestBody("text/plain; charset=utf-8".toMediaType())
                )
                .build()
            val builder = Request.Builder()
                .url("$base/upload")
                .header("Accept", "application/json")
                .post(body)
            val user = username()
            if (user.isNotBlank()) {
                builder.header("Authorization", Credentials.basic(user, password()))
            }
            client.newCall(builder.build()).execute().use { response ->
                val responseContent = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    recordApiFailure("POST", "/upload", response.code, responseContent)
                    throw ServerResponseException(
                        response.code,
                        "HTTP ${response.code}: ${responseContent.take(300)}"
                    )
                }
                responseContent
            }
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
                    recordApiFailure(method, path, response.code, content)
                    throw ServerResponseException(
                        response.code,
                        "HTTP ${response.code}: ${content.take(300)}"
                    )
                }
                content
            }
        }

    private fun recordApiFailure(method: String, path: String, statusCode: Int, responseBody: String) {
        try {
            crashLogStore.recordApiFailure(
                method = method,
                endpoint = path.substringBefore('?').replace('\n', ' ').replace('\r', ' '),
                statusCode = statusCode,
                responseBody = responseBody
            )
        } catch (exception: Exception) {
            Log.e("SourceboxApi", "Failed to save API error log", exception)
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

class ServerResponseException(val statusCode: Int, message: String) : Exception(message) {
    val isRetryable: Boolean = statusCode == 408 || statusCode == 429 || statusCode >= 500
}
