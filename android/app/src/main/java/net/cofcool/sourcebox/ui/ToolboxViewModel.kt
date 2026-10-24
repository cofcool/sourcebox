package net.cofcool.sourcebox.ui

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import net.cofcool.sourcebox.data.ActionCheckList
import net.cofcool.sourcebox.data.ActionComment
import net.cofcool.sourcebox.data.ActionExport
import net.cofcool.sourcebox.data.ActionExportDatabase
import net.cofcool.sourcebox.data.ActionRecord
import net.cofcool.sourcebox.data.CommandRecord
import net.cofcool.sourcebox.data.NoteRecord
import net.cofcool.sourcebox.data.ServerResponseException
import net.cofcool.sourcebox.data.SourceboxApi
import net.cofcool.sourcebox.data.TodoRecord
import net.cofcool.sourcebox.data.ToolInfo
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.UUID

private const val DIRTY_SYNC_RETRY_INTERVAL = 30_000L

class ToolboxViewModel(application: Application) : AndroidViewModel(application) {
    private val api = SourceboxApi(application)
    private val actionDatabase = ActionExportDatabase(application)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val syncMutex = Mutex()
    private val connectivityManager =
        application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            viewModelScope.launch { retryPendingChanges() }
        }
    }

    var tools by mutableStateOf<List<ToolInfo>>(emptyList())
        private set
    var notes by mutableStateOf<List<NoteRecord>>(emptyList())
        private set
    var todos by mutableStateOf<List<TodoRecord>>(emptyList())
        private set
    var commands by mutableStateOf<List<CommandRecord>>(emptyList())
        private set
    var records by mutableStateOf<List<ActionRecord>>(emptyList())
        private set
    var recordCategories by mutableStateOf<List<String>>(emptyList())
        private set
    var recordComments by mutableStateOf<List<ActionComment>>(emptyList())
        private set
    var recordChecklists by mutableStateOf<List<ActionCheckList>>(emptyList())
        private set
    var recordHasNextPage by mutableStateOf(false)
        private set
    var savedRecord by mutableStateOf<ActionRecord?>(null)
        private set
    var message by mutableStateOf("")
        private set
    var busy by mutableStateOf(false)
        private set
    var baseUrl by mutableStateOf(api.baseUrl())
        private set
    var username by mutableStateOf(api.username())
        private set
    var password by mutableStateOf(api.password())
        private set
    var localActionCount by mutableStateOf(actionDatabase.actionCount())
        private set
    var lastActionSync by mutableStateOf(actionDatabase.lastSyncedAt())
        private set
    var localNoteCount by mutableStateOf(actionDatabase.noteCount())
        private set
    var lastNoteSync by mutableStateOf(actionDatabase.lastSyncedAt("notes"))
        private set
    var localCommandCount by mutableStateOf(actionDatabase.commandCount())
        private set
    var pendingChangeCount by mutableStateOf(actionDatabase.pendingChangeCount())
        private set
    var lastCommandSync by mutableStateOf(actionDatabase.lastSyncedAt("commands"))
        private set

    init {
        connectivityManager.registerDefaultNetworkCallback(networkCallback)
        refreshTools()
        syncAllData()
        viewModelScope.launch {
            while (isActive) {
                delay(DIRTY_SYNC_RETRY_INTERVAL)
                retryPendingChanges()
            }
        }
    }

    fun refreshTools() = perform {
        tools = api.tools()
        message = "已连接，加载 ${tools.size} 个工具"
    }

    fun syncAllData() {
        if (api.baseUrl().isBlank()) return
        perform {
            syncMutex.withLock {
                uploadPendingChanges()
                val (exportedActions, exportedNotes, exportedCommands) = coroutineScope {
                    val actions = async { api.exportAllActions() }
                    val notes = async { api.exportAllNotes() }
                    val commands = async { api.exportAllCommands() }
                    Triple(actions.await(), notes.await(), commands.await())
                }
                val syncedAt = currentRecordDateTime()
                withContext(Dispatchers.IO) {
                    actionDatabase.replaceExportData(
                        exportedActions,
                        exportedNotes,
                        exportedCommands,
                        syncedAt
                    )
                }
                localActionCount = exportedActions.size
                lastActionSync = syncedAt
                localNoteCount = exportedNotes.size
                lastNoteSync = syncedAt
                localCommandCount = exportedCommands.size
                lastCommandSync = syncedAt
                refreshPendingCount()
                message = "已同步 ${exportedActions.size} 条行动、${exportedNotes.size} 条便笺、${exportedCommands.size} 条命令"
            }
        }
    }

    fun syncAllActions() = syncAllData()

    private suspend fun <T> writeWithOfflineFallback(
        server: suspend () -> T,
        offline: suspend () -> T
    ): Pair<T, Boolean> = syncMutex.withLock {
        if (withContext(Dispatchers.IO) { actionDatabase.pendingChangeCount() } > 0) {
            try {
                uploadPendingChanges()
            } catch (error: Exception) {
                message = "待同步修改上传失败：${error.message ?: "连接失败"}"
            }
            if (withContext(Dispatchers.IO) { actionDatabase.pendingChangeCount() } > 0) {
                return@withLock offline() to true
            }
        }
        try {
            server() to false
        } catch (error: Exception) {
            if (error !is IOException && (error !is ServerResponseException || !error.isRetryable)) {
                throw error
            }
            offline() to true
        }
    }

    private suspend fun writeDeleteWithOfflineFallback(
        server: suspend () -> Unit,
        offline: suspend () -> Unit
    ): Boolean = syncMutex.withLock {
        if (withContext(Dispatchers.IO) { actionDatabase.pendingChangeCount() } > 0) {
            try {
                uploadPendingChanges()
            } catch (error: Exception) {
                message = "待同步修改上传失败：${error.message ?: "连接失败"}"
            }
            if (withContext(Dispatchers.IO) { actionDatabase.pendingChangeCount() } > 0) {
                offline()
                return@withLock true
            }
        }
        try {
            server()
            false
        } catch (error: Exception) {
            if (error !is IOException && (error !is ServerResponseException || !error.isRetryable)) {
                throw error
            }
            offline()
            true
        }
    }

    private suspend fun retryPendingChanges() {
        if (api.baseUrl().isBlank() || withContext(Dispatchers.IO) { actionDatabase.pendingChangeCount() } == 0) {
            refreshPendingCount()
            return
        }
        try {
            syncMutex.withLock { uploadPendingChanges() }
            refreshPendingCount()
            if (pendingChangeCount == 0) message = "离线修改已全部同步"
        } catch (error: Exception) {
            refreshPendingCount()
            message = "待同步修改上传失败，${pendingChangeCount} 条仍保存在本地：${error.message ?: "连接失败"}"
        }
    }

    private suspend fun uploadPendingChanges() {
        val changes = withContext(Dispatchers.IO) { actionDatabase.pendingChanges() }
        for (change in changes) {
            var canonicalPayload: String? = null
            when (change.entity to change.operation) {
                "note" to "save" -> {
                    val note = json.decodeFromString<NoteRecord>(change.payload)
                    canonicalPayload = json.encodeToString(api.saveNote(note))
                }
                "note" to "delete" -> api.deleteNote(change.entityId)
                "command" to "save" -> {
                    val command = json.decodeFromString<CommandRecord>(change.payload)
                    canonicalPayload = json.encodeToString(api.saveCommand(command))
                }
                "command" to "delete" -> api.deleteCommand(change.entityId)
                "action" to "save" -> {
                    val action = json.decodeFromString<ActionExport>(change.payload).record
                    val saved = api.saveAction(action)
                    val local = withContext(Dispatchers.IO) { actionDatabase.actionDetails(change.entityId) }
                        ?: ActionExport(saved)
                    canonicalPayload = json.encodeToString(local.copy(record = saved))
                }
                "action" to "comment" -> {
                    val comment = json.decodeFromString<ActionComment>(change.payload)
                    api.addRecordComment(change.entityId, comment.content)
                }
                "action" to "checklist_save" -> {
                    val checklist = json.decodeFromString<ActionCheckList>(change.payload)
                    api.saveRecordChecklist(change.entityId, checklist)
                }
                "action" to "checklist_delete" -> api.deleteRecordChecklist(change.payload)
                else -> throw IllegalStateException(
                    "无法处理待同步操作：${change.entity}/${change.operation}"
                )
            }
            withContext(Dispatchers.IO) {
                actionDatabase.completePendingChange(change, canonicalPayload)
            }
        }
        localActionCount = withContext(Dispatchers.IO) { actionDatabase.actionCount() }
        localNoteCount = withContext(Dispatchers.IO) { actionDatabase.noteCount() }
        localCommandCount = withContext(Dispatchers.IO) { actionDatabase.commandCount() }
    }

    private suspend fun refreshPendingCount() {
        pendingChangeCount = withContext(Dispatchers.IO) { actionDatabase.pendingChangeCount() }
    }

    fun runTool(tool: ToolInfo, values: Map<String, String>) {
        lastToolOutput = ""
        perform(
            action = {
            val missing = tool.arguments.firstOrNull { it.required && values[it.key].isNullOrBlank() }
            require(missing == null) { "请填写必填参数：${missing?.key}" }
            val output = api.executeTool(tool.id, values)
            message = "工具执行完成"
            output
            },
            onSuccess = { lastToolOutput = it }
        )
    }

    fun refreshNotes() = perform {
        notes = withContext(Dispatchers.IO) { actionDatabase.notes() }
    }

    fun saveNote(note: NoteRecord) = perform {
        val prepared = note.copy(
            id = note.id.ifBlank { UUID.randomUUID().toString() },
            date = currentRecordDateTime(),
            state = "NORMAL"
        )
        val (saved, isDirty) = writeWithOfflineFallback(
            server = {
                api.saveNote(prepared).also { saved ->
                    withContext(Dispatchers.IO) { actionDatabase.saveNote(saved) }
                }
            },
            offline = {
                withContext(Dispatchers.IO) { actionDatabase.markNoteDirty(prepared) }
                prepared
            }
        )
        notes = withContext(Dispatchers.IO) { actionDatabase.notes() }
        localNoteCount = withContext(Dispatchers.IO) { actionDatabase.noteCount() }
        refreshPendingCount()
        message = if (isDirty) "便笺已保存到本地，等待联网同步" else "便笺已保存"
    }

    fun deleteNote(id: String) = perform {
        val isDirty = writeDeleteWithOfflineFallback(
            server = {
                api.deleteNote(id)
                withContext(Dispatchers.IO) { actionDatabase.deleteNote(id) }
            },
            offline = { withContext(Dispatchers.IO) { actionDatabase.markNoteDeleted(id) } }
        )
        notes = withContext(Dispatchers.IO) { actionDatabase.notes() }
        localNoteCount = withContext(Dispatchers.IO) { actionDatabase.noteCount() }
        refreshPendingCount()
        message = if (isDirty) "便笺已在本地删除，等待联网同步" else "便笺已删除"
    }

    fun refreshTodos(query: String = "") = perform {
        val actionQuery = buildString {
            append("state=todo")
            if (query.isNotBlank()) append("&name=${java.net.URLEncoder.encode(query, "UTF-8")}")
        }
        todos = withContext(Dispatchers.IO) {
            actionDatabase.actions("todo", actionQuery).map { it.toTodoRecord() }
        }
    }

    fun addTodo(name: String, remark: String) = perform {
        require(name.isNotBlank()) { "待办名称不能为空" }
        val pendingAction = ActionRecord(
            id = UUID.randomUUID().toString().replace("-", ""),
            name = name.trim(),
            remark = remark.trim(),
            state = "todo",
            type = "todo",
            start = currentRecordDateTime(),
            end = currentRecordDateTime()
        )
        val (saved, isDirty) = writeWithOfflineFallback(
            server = {
                api.addTodo(pendingAction.name, pendingAction.remark.orEmpty(), pendingAction.id)
                    .also { saved ->
                        withContext(Dispatchers.IO) { actionDatabase.saveAction(saved) }
                    }
            },
            offline = {
                withContext(Dispatchers.IO) { actionDatabase.markActionDirty(pendingAction) }
                pendingAction
            }
        )
        localActionCount = withContext(Dispatchers.IO) { actionDatabase.actionCount() }
        refreshPendingCount()
        todos = withContext(Dispatchers.IO) {
            actionDatabase.actions("todo", "state=todo").map { it.toTodoRecord() }
        }
        message = if (isDirty) "待办已添加到本地，等待联网同步" else "待办已添加"
    }

    fun completeTodo(todo: TodoRecord) = perform {
        val current = withContext(Dispatchers.IO) { actionDatabase.actionDetails(todo.id) }
            ?.record ?: todo.toActionRecord()
        val updatedLocal = current.copy(state = "done")
        val (updated, isDirty) = writeWithOfflineFallback(
            server = {
                api.completeTodo(todo.id).also { saved ->
                    withContext(Dispatchers.IO) { actionDatabase.saveAction(saved) }
                }
            },
            offline = {
                withContext(Dispatchers.IO) { actionDatabase.markActionDirty(updatedLocal) }
                updatedLocal
            }
        )
        localActionCount = withContext(Dispatchers.IO) { actionDatabase.actionCount() }
        refreshPendingCount()
        todos = withContext(Dispatchers.IO) {
            actionDatabase.actions("todo", "state=todo").map { it.toTodoRecord() }
        }
        message = if (isDirty) "待办已在本地完成，等待联网同步" else "待办已完成"
    }

    fun refreshCommands(query: String = "") = perform {
        commands = withContext(Dispatchers.IO) { actionDatabase.commands(query) }
    }

    fun saveCommand(command: CommandRecord) = perform {
        require(command.cmd.isNotBlank()) { "命令内容不能为空" }
        val prepared = command.withStableId()
        val (saved, isDirty) = writeWithOfflineFallback(
            server = {
                api.saveCommand(prepared).also { saved ->
                    withContext(Dispatchers.IO) { actionDatabase.saveCommand(saved) }
                }
            },
            offline = {
                withContext(Dispatchers.IO) { actionDatabase.markCommandDirty(prepared) }
                prepared
            }
        )
        localCommandCount = withContext(Dispatchers.IO) { actionDatabase.commandCount() }
        commands = withContext(Dispatchers.IO) { actionDatabase.commands("") }
        refreshPendingCount()
        message = if (isDirty) "命令已保存到本地，等待联网同步" else "命令已保存"
    }

    fun deleteCommand(id: String) = perform {
        val isDirty = writeDeleteWithOfflineFallback(
            server = {
                api.deleteCommand(id)
                withContext(Dispatchers.IO) { actionDatabase.deleteCommand(id) }
            },
            offline = { withContext(Dispatchers.IO) { actionDatabase.markCommandDeleted(id) } }
        )
        localCommandCount = withContext(Dispatchers.IO) { actionDatabase.commandCount() }
        commands = withContext(Dispatchers.IO) { actionDatabase.commands("") }
        refreshPendingCount()
        message = if (isDirty) "命令已在本地删除，等待联网同步" else "命令已删除"
    }

    fun refreshRecords(query: String = "", page: Int = 1) = perform {
        val result = withContext(Dispatchers.IO) {
            actionDatabase.actions("record", query, page, 11)
        }
        records = result.take(10)
        recordHasNextPage = result.size > 10
    }

    fun refreshRecordCategories() = perform {
        recordCategories = withContext(Dispatchers.IO) { actionDatabase.actionCategories() }
    }

    fun loadRecordDetails(record: ActionRecord) = perform {
        val details = withContext(Dispatchers.IO) {
            actionDatabase.actionDetails(record.id)
                ?: throw IllegalStateException("本地找不到行动记录：${record.id}，请先同步数据")
        }
        recordComments = details.comments
        recordChecklists = details.checklists
    }

    fun createRecord(name: String, query: String, page: Int) = perform(
        onSuccess = {
            savedRecord = it.first
            refreshRecords(query, page)
            message = if (it.second) "记录已添加到本地，等待联网同步" else "记录已添加"
        }
    ) {
        require(name.isNotBlank()) { "记录名称不能为空" }
        val pendingRecord = ActionRecord(
            id = UUID.randomUUID().toString().replace("-", ""),
            name = name.trim(),
            start = currentRecordDateTime()
        )
        val (saved, isDirty) = writeWithOfflineFallback(
            server = {
                api.saveRecord(pendingRecord).also { saved ->
                    withContext(Dispatchers.IO) { actionDatabase.saveAction(saved) }
                }
            },
            offline = {
                withContext(Dispatchers.IO) { actionDatabase.markActionDirty(pendingRecord) }
                pendingRecord
            }
        )
        localActionCount = withContext(Dispatchers.IO) { actionDatabase.actionCount() }
        refreshPendingCount()
        saved to isDirty
    }

    fun saveRecord(record: ActionRecord, query: String, page: Int) = perform(
        onSuccess = {
            savedRecord = it.first
            refreshRecords(query, page)
            message = if (it.second) "记录已保存到本地，等待联网同步" else "记录已保存"
        }
    ) {
        require(record.name.isNotBlank()) { "记录名称不能为空" }
        val prepared = record.copy(type = "record", labels = record.labels.orEmpty())
        val (saved, isDirty) = writeWithOfflineFallback(
            server = {
                api.saveRecord(prepared).also { saved ->
                    withContext(Dispatchers.IO) { actionDatabase.saveAction(saved) }
                }
            },
            offline = {
                withContext(Dispatchers.IO) { actionDatabase.markActionDirty(prepared) }
                prepared
            }
        )
        localActionCount = withContext(Dispatchers.IO) { actionDatabase.actionCount() }
        refreshPendingCount()
        saved to isDirty
    }

    fun addRecordComment(actionId: String, content: String) = perform(
        onSuccess = {
            recordComments = recordComments + it.first
            message = if (it.second) "回复已保存到本地，等待联网同步" else "回复已添加"
        }
    ) {
        require(content.isNotBlank()) { "回复内容不能为空" }
        val comment = ActionComment(
            id = UUID.randomUUID().toString(),
            actionId = actionId,
            content = content.trim(),
            createTime = currentRecordDateTime()
        )
        val (saved, isDirty) = writeWithOfflineFallback(
            server = {
                api.addRecordComment(actionId, comment.content).also { saved ->
                    withContext(Dispatchers.IO) { actionDatabase.saveActionComment(actionId, saved) }
                }
            },
            offline = {
                withContext(Dispatchers.IO) { actionDatabase.markActionCommentDirty(actionId, comment) }
                comment
            }
        )
        refreshPendingCount()
        saved to isDirty
    }

    fun saveRecordChecklist(actionId: String, checklist: ActionCheckList) = perform(
        onSuccess = { (saved, progressComment, isDirty) ->
            recordChecklists = recordChecklists.map { if (it.id == saved.id) saved else it }
            if (recordChecklists.none { it.id == saved.id }) recordChecklists = recordChecklists + saved
            recordComments = recordComments + progressComment
            message = if (isDirty) "Checklist 已保存到本地，等待联网同步" else "Checklist 已保存"
        }
    ) {
        require(checklist.content.isNotBlank()) { "Checklist 名称不能为空" }
        require(checklist.first <= checklist.current && checklist.current <= checklist.last) {
            "当前值必须在开始和结束范围内"
        }
        val prepared = checklist.copy(
            id = checklist.id.ifBlank { UUID.randomUUID().toString().replace("-", "") },
            actionId = actionId
        )
        val progressComment = ActionComment(
            id = UUID.randomUUID().toString(),
            actionId = actionId,
            content = "当前进度为 ${prepared.content} - ${prepared.current}",
            createTime = currentRecordDateTime()
        )
        val (saved, isDirty) = writeWithOfflineFallback(
            server = {
                api.saveRecordChecklist(actionId, prepared).also { saved ->
                    withContext(Dispatchers.IO) {
                        actionDatabase.saveActionChecklist(actionId, saved)
                        actionDatabase.saveActionComment(actionId, progressComment)
                    }
                }
            },
            offline = {
                withContext(Dispatchers.IO) {
                    actionDatabase.markActionChecklistDirty(actionId, prepared, progressComment)
                }
                prepared
            }
        )
        refreshPendingCount()
        Triple(saved, progressComment, isDirty)
    }

    fun deleteRecordChecklist(id: String) = perform {
        val isDirty = writeDeleteWithOfflineFallback(
            server = {
                api.deleteRecordChecklist(id)
                withContext(Dispatchers.IO) { actionDatabase.deleteActionChecklist(id) }
            },
            offline = {
                withContext(Dispatchers.IO) { actionDatabase.markActionChecklistDeleted(id) }
            }
        )
        recordChecklists = recordChecklists.filterNot { it.id == id }
        refreshPendingCount()
        message = if (isDirty) "Checklist 已在本地删除，等待联网同步" else "Checklist 已删除"
    }

    fun saveConnection(url: String, user: String, secret: String) {
        api.saveConnection(url, user, secret)
        baseUrl = api.baseUrl()
        username = api.username()
        password = api.password()
        refreshTools()
        syncAllData()
    }

    private fun <T> perform(onSuccess: (T) -> Unit = {}, action: suspend () -> T) {
        viewModelScope.launch {
            busy = true
            try {
                onSuccess(action())
            } catch (error: Exception) {
                message = error.message ?: "操作失败"
            } finally {
                busy = false
            }
        }
    }

    var lastToolOutput by mutableStateOf("")
        private set

    override fun onCleared() {
        connectivityManager.unregisterNetworkCallback(networkCallback)
        actionDatabase.close()
        super.onCleared()
    }

    private fun currentRecordDateTime(): String =
        java.time.LocalDateTime.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

    private fun ActionRecord.toTodoRecord() = TodoRecord(
        id = id,
        name = name,
        state = state,
        remark = remark,
        type = type,
        start = start,
        end = end
    )

    private fun TodoRecord.toActionRecord() = ActionRecord(
        id = id,
        name = name,
        state = state,
        remark = remark,
        type = type,
        start = start,
        end = end
    )

    private fun CommandRecord.withStableId() = copy(
        id = id ?: alias?.takeIf(String::isNotBlank) ?: cmd.md5()
    )

    private fun String.md5(): String =
        MessageDigest.getInstance("MD5")
            .digest(toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
