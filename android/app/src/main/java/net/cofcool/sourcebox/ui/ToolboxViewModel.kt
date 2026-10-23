package net.cofcool.sourcebox.ui

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.cofcool.sourcebox.data.CommandRecord
import net.cofcool.sourcebox.data.ActionCheckList
import net.cofcool.sourcebox.data.ActionComment
import net.cofcool.sourcebox.data.ActionRecord
import net.cofcool.sourcebox.data.NoteRecord
import net.cofcool.sourcebox.data.SourceboxApi
import net.cofcool.sourcebox.data.ToolInfo
import net.cofcool.sourcebox.data.TodoRecord
import java.util.UUID

@kotlinx.serialization.Serializable
data class HistoryEntry(
    val id: String,
    val title: String,
    val input: String,
    val output: String,
    val time: Long = System.currentTimeMillis(),
    val source: String = "server"
)

class ToolboxViewModel(application: Application) : AndroidViewModel(application) {
    private val api = SourceboxApi(application)
    private val preferences = application.getSharedPreferences("sourcebox", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

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
    var history by mutableStateOf(loadHistory())
        private set
    var busy by mutableStateOf(false)
        private set
    var baseUrl by mutableStateOf(api.baseUrl())
        private set
    var username by mutableStateOf(api.username())
        private set
    var password by mutableStateOf(api.password())
        private set

    init {
        refreshTools()
    }

    fun refreshTools() = perform {
        tools = api.tools()
        message = "已连接，加载 ${tools.size} 个工具"
    }

    fun runTool(tool: ToolInfo, values: Map<String, String>) {
        lastToolOutput = ""
        perform(
            action = {
            val missing = tool.arguments.firstOrNull { it.required && values[it.key].isNullOrBlank() }
            require(missing == null) { "请填写必填参数：${missing?.key}" }
            val output = api.executeTool(tool.id, values)
            history = (listOf(
                HistoryEntry(
                    id = UUID.randomUUID().toString(),
                    title = tool.id,
                    input = values.filterValues { it.isNotEmpty() }.toString(),
                    output = output
                )
            ) + history).take(100)
            persistHistory()
            message = "工具执行完成"
            output
            },
            onSuccess = { lastToolOutput = it }
        )
    }

    fun refreshNotes() = perform { notes = api.notes() }

    fun saveNote(note: NoteRecord) = perform {
        api.saveNote(note)
        notes = api.notes()
        message = "便笺已保存"
    }

    fun deleteNote(id: String) = perform {
        api.deleteNote(id)
        notes = api.notes()
        message = "便笺已删除"
    }

    fun refreshTodos(query: String = "") = perform { todos = api.todos(query) }

    fun addTodo(name: String, remark: String) = perform {
        require(name.isNotBlank()) { "待办名称不能为空" }
        api.addTodo(name.trim(), remark.trim())
        todos = api.todos()
        message = "待办已添加"
    }

    fun completeTodo(todo: TodoRecord) = perform {
        api.completeTodo(todo.id)
        todos = api.todos()
        message = "待办已完成"
    }

    fun refreshCommands(query: String = "") = perform { commands = api.commands(query) }

    fun saveCommand(command: CommandRecord) = perform {
        require(command.cmd.isNotBlank()) { "命令内容不能为空" }
        api.saveCommand(command)
        commands = api.commands("")
        message = "命令已保存"
    }

    fun deleteCommand(id: String) = perform {
        api.deleteCommand(id)
        commands = api.commands("")
        message = "命令已删除"
    }

    fun storeCommands() = perform {
        api.storeCommands()
        commands = api.commands("")
        message = "历史命令已收集"
    }

    fun refreshRecords(query: String = "", page: Int = 1) = perform {
        val result = api.recordList(query, page, 11)
        records = result.take(10)
        recordHasNextPage = result.size > 10
    }

    fun refreshRecordCategories() = perform {
        recordCategories = api.recordTypes()
    }

    fun loadRecordDetails(record: ActionRecord) = perform {
        recordComments = emptyList()
        recordChecklists = emptyList()
        val (comments, checklists) = coroutineScope {
            val commentsDeferred = async { api.recordComments(record.id) }
            val checklistsDeferred = async { api.recordChecklists(record.id) }
            commentsDeferred.await() to checklistsDeferred.await()
        }
        recordComments = comments
        recordChecklists = checklists
    }

    fun createRecord(name: String, query: String, page: Int) = perform(
        onSuccess = {
            savedRecord = it
            refreshRecords(query, page)
            message = "记录已添加"
        }
    ) {
        require(name.isNotBlank()) { "记录名称不能为空" }
        api.saveRecord(
            ActionRecord(
                id = UUID.randomUUID().toString().replace("-", ""),
                name = name.trim(),
                start = currentRecordDateTime()
            )
        )
    }

    fun saveRecord(record: ActionRecord, query: String, page: Int) = perform(
        onSuccess = {
            savedRecord = it
            refreshRecords(query, page)
            message = "记录已保存"
        }
    ) {
        require(record.name.isNotBlank()) { "记录名称不能为空" }
        api.saveRecord(record)
    }

    fun addRecordComment(actionId: String, content: String) = perform(
        onSuccess = { recordComments = recordComments + it; message = "回复已添加" }
    ) {
        require(content.isNotBlank()) { "回复内容不能为空" }
        api.addRecordComment(actionId, content.trim())
    }

    fun saveRecordChecklist(actionId: String, checklist: ActionCheckList) = perform(
        onSuccess = { saved ->
            recordChecklists = recordChecklists.map { if (it.id == saved.id) saved else it }
            if (recordChecklists.none { it.id == saved.id }) recordChecklists = recordChecklists + saved
            message = "Checklist 已保存"
        }
    ) {
        require(checklist.content.isNotBlank()) { "Checklist 名称不能为空" }
        require(checklist.first <= checklist.current && checklist.current <= checklist.last) {
            "当前值必须在开始和结束范围内"
        }
        api.saveRecordChecklist(actionId, checklist)
    }

    fun deleteRecordChecklist(id: String) = perform {
        api.deleteRecordChecklist(id)
        recordChecklists = recordChecklists.filterNot { it.id == id }
        message = "Checklist 已删除"
    }

    fun saveConnection(url: String, user: String, secret: String) {
        api.saveConnection(url, user, secret)
        baseUrl = api.baseUrl()
        username = api.username()
        password = api.password()
        refreshTools()
    }

    fun addLocalHistory(title: String, input: String, output: String, source: String = "local") {
        history = (listOf(
            HistoryEntry(UUID.randomUUID().toString(), title, input, output, source = source)
        ) + history).take(100)
        persistHistory()
    }

    fun clearHistory() {
        history = emptyList()
        persistHistory()
        message = "历史记录已清空"
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

    private fun loadHistory(): List<HistoryEntry> {
        val stored = preferences.getString("history", "[]") ?: "[]"
        return try {
            json.decodeFromString<List<HistoryEntry>>(stored)
        } catch (error: SerializationException) {
            message = "本地历史记录无法解析：${error.message}"
            emptyList()
        }
    }

    private fun persistHistory() {
        preferences.edit().putString("history", json.encodeToString(history)).apply()
    }

    private fun currentRecordDateTime(): String =
        java.time.LocalDateTime.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
}
