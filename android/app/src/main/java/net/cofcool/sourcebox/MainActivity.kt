package net.cofcool.sourcebox

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import net.cofcool.sourcebox.data.ActionCheckList
import net.cofcool.sourcebox.data.ActionRecord
import net.cofcool.sourcebox.data.CommandRecord
import net.cofcool.sourcebox.data.NoteRecord
import net.cofcool.sourcebox.data.TodoRecord
import net.cofcool.sourcebox.data.ToolInfo
import net.cofcool.sourcebox.ui.ToolboxViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SourceboxApp() }
    }
}

private val pages = listOf(
    "tools" to "工具",
    "todos" to "待办",
    "notes" to "便笺",
    "commands" to "命令",
    "task" to "任务"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SourceboxApp(vm: ToolboxViewModel = viewModel()) {
    var page by remember { mutableStateOf("tools") }
    var selectedTool by remember { mutableStateOf<ToolInfo?>(null) }
    var selectedRecord by remember { mutableStateOf<ActionRecord?>(null) }
    var recordReturnPage by remember { mutableStateOf("task") }
    var settingsUrl by remember { mutableStateOf(vm.baseUrl) }
    var settingsUser by remember { mutableStateOf(vm.username) }
    var settingsPassword by remember { mutableStateOf(vm.password) }
    var fileResult by remember { mutableStateOf("") }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(vm.message) {
        if (vm.message.isNotBlank()) snackbar.showSnackbar(vm.message)
    }
    LaunchedEffect(vm.savedRecord) {
        val saved = vm.savedRecord
        if (saved != null && selectedRecord?.id == saved.id) selectedRecord = saved
    }

    MaterialTheme(
        colorScheme = if (androidx.compose.foundation.isSystemInDarkTheme()) {
            darkColorScheme()
        } else {
            lightColorScheme()
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            when (page) {
                                "tool" -> when (selectedTool?.id) {
                                    "hisRecord" -> "任务"
                                    else -> selectedTool?.id ?: "工具"
                                }
                                "task" -> "任务"
                                "record-detail" -> "编辑任务"
                                "json" -> "JSON 格式化"
                                "files" -> "文件工具"
                                "file-result" -> "文件内容"
                                "settings" -> "连接设置"
                                else -> pages.firstOrNull { it.first == page }?.second ?: "The Source Box"
                            }
                        )
                    },
                    navigationIcon = {
                        if (page !in pages.map { it.first }) {
                            IconButton(onClick = {
                                page = if (page == "record-detail") {
                                    recordReturnPage
                                } else {
                                    "tools"
                                }
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                            }
                        }
                    },
                    actions = {
                        if (page == "tools") {
                            IconButton(onClick = {
                                settingsUrl = vm.baseUrl
                                settingsUser = vm.username
                                settingsPassword = vm.password
                                page = "settings"
                            }) {
                                Icon(Icons.Default.Settings, contentDescription = "设置")
                            }
                        }
                    }
                )
            },
            bottomBar = {
                if (page != "record-detail") {
                    NavigationBar {
                        pages.forEach { (destination, label) ->
                            val icon = when (destination) {
                                "tools" -> Icons.Default.Home
                                "todos" -> Icons.AutoMirrored.Filled.List
                                "notes" -> Icons.Default.Description
                                "commands" -> Icons.Default.Code
                                else -> Icons.Default.Build
                            }
                            NavigationBarItem(
                                selected = page == destination,
                                onClick = {
                                    page = destination
                                    when (destination) {
                                        "tools" -> vm.refreshTools()
                                        "todos" -> vm.refreshTodos()
                                        "notes" -> vm.refreshNotes()
                                        "commands" -> vm.refreshCommands()
                                    }
                                },
                                icon = { Icon(icon, contentDescription = label) },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbar) }
        ) { padding ->
            when (page) {
                "tools" -> HomeScreen(
                    vm = vm,
                    modifier = Modifier.padding(padding),
                    onSelectTool = { selectedTool = it; page = "tool" },
                    onJson = { page = "json" },
                    onFiles = { page = "files" }
                )
                "tool" -> selectedTool?.let { tool ->
                    if (tool.id == "hisRecord") {
                        RecordBoardScreen(
                            vm,
                            Modifier.padding(padding),
                            onOpenRecord = {
                                selectedRecord = it
                                recordReturnPage = page
                                vm.loadRecordDetails(it)
                                page = "record-detail"
                            }
                        )
                    } else {
                        ToolScreen(tool, vm, Modifier.padding(padding))
                    }
                }
                "record-detail" -> selectedRecord?.let { record ->
                    RecordDetails(
                        modifier = Modifier.padding(padding),
                        record = record,
                        categories = vm.recordCategories,
                        comments = vm.recordComments,
                        checklists = vm.recordChecklists,
                        busy = vm.busy,
                        onRecordChange = { selectedRecord = it },
                        onSave = { vm.saveRecord(record, "", 1) },
                        onAddComment = { vm.addRecordComment(record.id, it) },
                        onAddChecklist = { content, first, last ->
                            vm.saveRecordChecklist(
                                record.id,
                                ActionCheckList(
                                    actionId = record.id,
                                    content = content,
                                    first = first,
                                    last = last,
                                    current = first
                                )
                            )
                        },
                        onSaveChecklist = { vm.saveRecordChecklist(record.id, it) },
                        onDeleteChecklist = vm::deleteRecordChecklist,
                        onClose = { page = recordReturnPage }
                    )
                }
                "task" -> RecordBoardScreen(
                    vm,
                    Modifier.padding(padding),
                    onOpenRecord = {
                        selectedRecord = it
                        recordReturnPage = "task"
                        vm.loadRecordDetails(it)
                        page = "record-detail"
                    }
                )
                "todos" -> TodosScreen(vm, Modifier.padding(padding))
                "notes" -> NotesScreen(vm, Modifier.padding(padding))
                "commands" -> CommandsScreen(vm, Modifier.padding(padding))
                "json" -> JsonScreen(Modifier.padding(padding))
                "files" -> FilesScreen(
                    modifier = Modifier.padding(padding),
                    onRead = { content ->
                        fileResult = content
                        page = "file-result"
                    }
                )
                "file-result" -> SelectionContainer {
                    Text(fileResult, Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()))
                }
                "settings" -> SettingsScreen(
                    modifier = Modifier.padding(padding),
                    url = settingsUrl,
                    onUrl = { settingsUrl = it },
                    username = settingsUser,
                    onUsername = { settingsUser = it },
                    password = settingsPassword,
                    onPassword = { settingsPassword = it },
                    onSave = { vm.saveConnection(settingsUrl, settingsUser, settingsPassword); page = "tools" }
                )
            }
        }
    }
}

@Composable
private fun HomeScreen(
    vm: ToolboxViewModel,
    modifier: Modifier,
    onSelectTool: (ToolInfo) -> Unit,
    onJson: () -> Unit,
    onFiles: () -> Unit
) {
    Column(modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text("开发工具箱", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            if (vm.baseUrl.isBlank()) "尚未连接后端" else "后端：${vm.baseUrl}",
            style = MaterialTheme.typography.bodySmall
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 12.dp)) {
            OutlinedButton(onClick = onJson, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.DataObject, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("JSON")
            }
            OutlinedButton(onClick = onFiles, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.FolderOpen, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("文件")
            }
            IconButton(onClick = vm::refreshTools) {
                Icon(Icons.Default.Refresh, contentDescription = "刷新工具")
            }
        }
        Text("服务端工具", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
        if (vm.busy && vm.tools.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (vm.tools.isEmpty()) {
            EmptyState("连接后端后会在这里显示可用工具。请检查连接设置和服务状态。")
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(vm.tools.filterNot { it.id == "note" || it.id == "todo" }, key = { it.id }) { tool ->
                    Card(onClick = { onSelectTool(tool) }, modifier = Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Build, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(tool.id, fontWeight = FontWeight.SemiBold)
                                if (tool.description.isNotBlank()) {
                                    Text(tool.description, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolScreen(tool: ToolInfo, vm: ToolboxViewModel, modifier: Modifier) {
    val values = remember(tool.id) {
        mutableStateMapOf<String, String>().apply {
            tool.arguments.forEach { put(it.key, it.defaultValue) }
        }
    }
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (tool.description.isNotBlank()) Text(tool.description)
        if (tool.arguments.isEmpty()) {
            Text("此工具没有可配置参数。")
        } else {
            tool.arguments.forEach { argument ->
                OutlinedTextField(
                    value = values[argument.key].orEmpty(),
                    onValueChange = { values[argument.key] = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(if (argument.required) "${argument.key} *" else argument.key) },
                    supportingText = {
                        val hint = argument.description.ifBlank { argument.demo }
                        if (hint.isNotBlank()) Text(hint)
                    },
                    minLines = if (argument.key in setOf("in", "input", "url", "config")) 2 else 1
                )
            }
        }
        Button(
            onClick = { vm.runTool(tool, values.toMap()) },
            enabled = !vm.busy,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (vm.busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            else Text("执行工具")
        }
        if (vm.lastToolOutput.isNotBlank()) {
            Text("执行结果", style = MaterialTheme.typography.titleMedium)
            SelectionContainer { Text(vm.lastToolOutput) }
        }
    }
}

@Composable
private fun RecordBoardScreen(
    vm: ToolboxViewModel,
    modifier: Modifier,
    onOpenRecord: (ActionRecord) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var appliedQuery by remember { mutableStateOf("") }
    var page by remember { mutableStateOf(1) }
    var newRecordName by remember { mutableStateOf("") }

    LaunchedEffect(appliedQuery, page) {
        vm.refreshRecords(appliedQuery, page)
    }
    LaunchedEffect(Unit) {
        vm.refreshRecordCategories()
    }
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("任务", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("记录事项、整理进度，并在卡片下持续补充回复。", style = MaterialTheme.typography.bodySmall)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("本地数据库：${vm.localActionCount} 条")
                Text(
                    vm.lastActionSync?.let { "最近同步：$it" } ?: "尚未同步",
                    style = MaterialTheme.typography.labelSmall
                )
            }
            OutlinedButton(onClick = vm::syncAllData, enabled = !vm.busy) {
                Text("立即同步")
            }
        }
        Text(
            "行动 ${vm.localActionCount} · 便笺 ${vm.localNoteCount} · 命令 ${vm.localCommandCount}",
            style = MaterialTheme.typography.labelSmall
        )
        Text(
            "便笺：${vm.lastNoteSync ?: "尚未同步"} · 命令：${vm.lastCommandSync ?: "尚未同步"}",
            style = MaterialTheme.typography.labelSmall
        )
        Text(
            "待上传的离线修改：${vm.pendingChangeCount}",
            style = MaterialTheme.typography.labelSmall,
            color = if (vm.pendingChangeCount > 0) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = newRecordName,
            onValueChange = { newRecordName = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("卡片名称") },
            singleLine = true
        )
        Button(
            onClick = {
                vm.createRecord(newRecordName, appliedQuery, page)
                newRecordName = ""
            },
            enabled = newRecordName.isNotBlank() && !vm.busy,
            modifier = Modifier.fillMaxWidth()
        ) { Text("添加卡片") }

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.weight(1f),
                label = { Text("按名称搜索") },
                placeholder = { Text("输入名称") },
                singleLine = true
            )
            IconButton(
                onClick = {
                    appliedQuery = query.trim()
                    page = 1
                },
                enabled = !vm.busy
            ) { Icon(Icons.Default.Search, contentDescription = "查询记录") }
        }

        if (vm.records.isEmpty()) {
            EmptyState(if (appliedQuery.isNotBlank()) "没有匹配的记录" else "还没有记录，添加一条开始使用")
        } else {
            vm.records.forEach { record ->
                Card(
                    onClick = { onOpenRecord(record) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(record.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        if (!record.remark.isNullOrBlank()) {
                            Text(record.remark, style = MaterialTheme.typography.bodySmall)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            record.category?.takeIf(String::isNotBlank)?.let { AssistChipLabel(it) }
                            record.labels.orEmpty().split(",").map(String::trim).filter(String::isNotBlank)
                                .forEach { AssistChipLabel(it) }
                        }
                        Text(record.start?.take(10) ?: "未设置日期", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = { if (page > 1) page-- }, enabled = page > 1 && !vm.busy) {
                Text("上一页")
            }
            Text("第 $page 页 · ${vm.records.size} 条")
            OutlinedButton(onClick = { page++ }, enabled = vm.recordHasNextPage && !vm.busy) {
                Text("下一页")
            }
        }

    }
}

@Composable
private fun AssistChipLabel(text: String) {
    androidx.compose.material3.AssistChip(
        onClick = {},
        label = { Text(text) }
    )
}

@Composable
private fun RecordDetails(
    modifier: Modifier,
    record: ActionRecord,
    categories: List<String>,
    comments: List<net.cofcool.sourcebox.data.ActionComment>,
    checklists: List<ActionCheckList>,
    busy: Boolean,
    onRecordChange: (ActionRecord) -> Unit,
    onSave: () -> Unit,
    onAddComment: (String) -> Unit,
    onAddChecklist: (String, Int, Int) -> Unit,
    onSaveChecklist: (ActionCheckList) -> Unit,
    onDeleteChecklist: (String) -> Unit,
    onClose: () -> Unit
) {
    var reply by remember(record.id) { mutableStateOf("") }
    var checklistName by remember(record.id) { mutableStateOf("") }
    var checklistFirst by remember(record.id) { mutableStateOf("0") }
    var checklistLast by remember(record.id) { mutableStateOf("") }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("编辑记录及其 Checklist、回复。", style = MaterialTheme.typography.bodySmall)
        Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("编辑卡片", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = onClose) { Text("关闭") }
            }
            OutlinedTextField(
                record.name,
                { onRecordChange(record.copy(name = it)) },
                Modifier.fillMaxWidth(),
                label = { Text("卡片名称") },
                singleLine = true
            )
            OutlinedTextField(
                record.remark.orEmpty(),
                { onRecordChange(record.copy(remark = it.ifBlank { null })) },
                Modifier.fillMaxWidth(),
                label = { Text("备注信息") },
                minLines = 3
            )
            OutlinedTextField(
                record.labels.orEmpty(),
                { onRecordChange(record.copy(labels = it)) },
                Modifier.fillMaxWidth(),
                label = { Text("标签") },
                supportingText = { Text("多个标签以逗号分隔") },
                singleLine = true
            )
            OutlinedTextField(
                record.category.orEmpty(),
                { onRecordChange(record.copy(category = it.ifBlank { null })) },
                Modifier.fillMaxWidth(),
                label = { Text("类别") },
                singleLine = true
            )
            if (categories.isNotEmpty()) {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { category ->
                        androidx.compose.material3.AssistChip(
                            onClick = { onRecordChange(record.copy(category = category)) },
                            label = { Text(category) }
                        )
                    }
                }
            }
            OutlinedTextField(
                record.start.orEmpty(),
                { onRecordChange(record.copy(start = it.ifBlank { null })) },
                Modifier.fillMaxWidth(),
                label = { Text("开始时间（yyyy-MM-dd HH:mm:ss）") },
                singleLine = true
            )
            OutlinedTextField(
                record.end.orEmpty(),
                { onRecordChange(record.copy(end = it.ifBlank { null })) },
                Modifier.fillMaxWidth(),
                label = { Text("结束时间（yyyy-MM-dd HH:mm:ss，可选）") },
                singleLine = true
            )
            OutlinedTextField(
                record.duration?.let { (it / 60).toString() }.orEmpty(),
                { value ->
                    val minutes = value.toIntOrNull()
                    onRecordChange(
                        record.copy(duration = minutes?.takeIf { it in 0..(Int.MAX_VALUE / 60) }?.times(60))
                    )
                },
                Modifier.fillMaxWidth(),
                label = { Text("持续时长（分钟）") },
                singleLine = true
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "todo" to "待办",
                    "doing" to "进行中",
                    "done" to "已完成",
                    "cancel" to "已取消"
                ).forEach { (state, label) ->
                    androidx.compose.material3.FilterChip(
                        selected = record.state == state,
                        onClick = { onRecordChange(record.copy(state = state)) },
                        label = { Text(label) }
                    )
                }
            }
            Button(
                onClick = onSave,
                enabled = !busy && record.name.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("保存信息") }

            androidx.compose.material3.HorizontalDivider()
            Text("Checklist", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                checklistName,
                { checklistName = it },
                Modifier.fillMaxWidth(),
                label = { Text("Checklist 名称") },
                singleLine = true
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    checklistFirst,
                    { checklistFirst = it },
                    Modifier.weight(1f),
                    label = { Text("开始") },
                    singleLine = true
                )
                OutlinedTextField(
                    checklistLast,
                    { checklistLast = it },
                    Modifier.weight(1f),
                    label = { Text("结束") },
                    singleLine = true
                )
            }
            val first = checklistFirst.toIntOrNull()
            val last = checklistLast.toIntOrNull()
            Button(
                onClick = {
                    if (first != null && last != null && first <= last && checklistName.isNotBlank()) {
                        onAddChecklist(checklistName.trim(), first, last)
                        checklistName = ""
                        checklistLast = ""
                    }
                },
                enabled = !busy && checklistName.isNotBlank() &&
                    first != null && last != null && first <= last,
                modifier = Modifier.fillMaxWidth()
            ) { Text("添加 Checklist") }
            if (checklists.isEmpty()) Text("还没有 Checklist", style = MaterialTheme.typography.bodySmall)
            checklists.forEach { checklist ->
                ChecklistItem(checklist, busy, onSaveChecklist, onDeleteChecklist)
            }

            androidx.compose.material3.HorizontalDivider()
            Text("回复", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                reply,
                { reply = it },
                Modifier.fillMaxWidth(),
                label = { Text("添加回复") },
                minLines = 2
            )
            Button(
                onClick = { onAddComment(reply); reply = "" },
                enabled = !busy && reply.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("添加回复") }
            if (comments.isEmpty()) Text("还没有回复", style = MaterialTheme.typography.bodySmall)
            comments.forEach { comment ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(comment.content)
                    Text(comment.createTime.orEmpty(), style = MaterialTheme.typography.labelSmall)
                    androidx.compose.material3.HorizontalDivider()
                }
            }
            }
        }
    }
}

@Composable
private fun ChecklistItem(
    checklist: ActionCheckList,
    busy: Boolean,
    onSave: (ActionCheckList) -> Unit,
    onDelete: (String) -> Unit
) {
    var current by remember(checklist.id, checklist.current) { mutableStateOf(checklist.current.toString()) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(checklist.content, fontWeight = FontWeight.SemiBold)
            Text("范围：${checklist.first} – ${checklist.last}")
            OutlinedTextField(
                current,
                { current = it },
                Modifier.fillMaxWidth(),
                label = { Text("当前进度") },
                singleLine = true
            )
            val value = current.toIntOrNull()
            Text(
                if (value == checklist.last) "已完成" else "进行中",
                color = if (value == checklist.last) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { value?.let { onSave(checklist.copy(current = it)) } },
                    enabled = !busy && value != null && value in checklist.first..checklist.last,
                    modifier = Modifier.weight(1f)
                ) { Text("保存进度") }
                OutlinedButton(
                    onClick = { onDelete(checklist.id) },
                    enabled = !busy,
                    modifier = Modifier.weight(1f)
                ) { Text("删除") }
            }
        }
    }
}

@Composable
private fun TodosScreen(vm: ToolboxViewModel, modifier: Modifier) {
    var name by remember { mutableStateOf("") }
    var remark by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    Column(modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("待办事项") }, singleLine = true)
        OutlinedTextField(remark, { remark = it }, Modifier.fillMaxWidth(), label = { Text("备注（可选）") }, minLines = 2)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { vm.addTodo(name, remark); name = ""; remark = "" },
                enabled = !vm.busy
            ) { Text("添加待办") }
            OutlinedButton(onClick = vm::refreshTodos) { Text("刷新") }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                query,
                { query = it },
                modifier = Modifier.weight(1f),
                label = { Text("搜索待办") },
                singleLine = true
            )
            IconButton(onClick = { vm.refreshTodos(query) }) {
                Icon(Icons.Default.Search, contentDescription = "搜索")
            }
        }
        if (vm.todos.isEmpty()) {
            EmptyState("暂无待办事项")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(vm.todos, key = { it.id }) { todo ->
                    TodoCard(todo, onComplete = { vm.completeTodo(todo) })
                }
            }
        }
    }
}

@Composable
private fun TodoCard(todo: TodoRecord, onComplete: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Switch(checked = todo.state == "done", onCheckedChange = { if (it) onComplete() })
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(todo.name, fontWeight = FontWeight.SemiBold)
                if (!todo.remark.isNullOrBlank()) {
                    Text(todo.remark, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun NotesScreen(vm: ToolboxViewModel, modifier: Modifier) {
    var selected by remember { mutableStateOf<NoteRecord?>(null) }
    var content by remember { mutableStateOf("") }
    Column(modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            content,
            { content = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("便笺内容") },
            minLines = 4
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    vm.saveNote(NoteRecord(id = selected?.id.orEmpty(), content = content))
                    selected = null
                    content = ""
                },
                enabled = content.isNotBlank() && !vm.busy
            ) { Text("保存") }
            OutlinedButton(onClick = { selected = null; content = "" }) { Text("新建") }
            IconButton(onClick = vm::refreshNotes) {
                Icon(Icons.Default.Refresh, contentDescription = "刷新便笺")
            }
            if (selected != null) {
                IconButton(onClick = {
                    vm.deleteNote(selected!!.id)
                    selected = null
                    content = ""
                }) {
                    Icon(Icons.Default.Delete, contentDescription = "删除便笺")
                }
            }
        }
        Text("已保存便笺", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 8.dp))
        if (vm.notes.isEmpty()) {
            EmptyState("暂无便笺")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(vm.notes, key = { it.id }) { note ->
                    Card(
                        onClick = { selected = note; content = note.content },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(note.content, maxLines = 4)
                            if (!note.date.isNullOrBlank()) {
                                Text(note.date, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommandsScreen(vm: ToolboxViewModel, modifier: Modifier) {
    var query by remember { mutableStateOf("") }
    var command by remember { mutableStateOf("") }
    var alias by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }
    Column(modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(command, { command = it }, Modifier.fillMaxWidth(), label = { Text("命令") }, minLines = 2)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(alias, { alias = it }, Modifier.weight(1f), label = { Text("别名（可选）") }, singleLine = true)
            OutlinedTextField(tags, { tags = it }, Modifier.weight(1f), label = { Text("标签，逗号分隔") }, singleLine = true)
        }
        Button(
            onClick = {
                vm.saveCommand(
                    CommandRecord(
                        cmd = command.trim(),
                        alias = alias.trim().ifBlank { null },
                        tags = tags.split(",").map(String::trim).filter(String::isNotBlank)
                    )
                )
                command = ""
                alias = ""
                tags = ""
            },
            enabled = command.isNotBlank() && !vm.busy
        ) { Text("保存命令") }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                query,
                { query = it },
                modifier = Modifier.weight(1f),
                label = { Text("搜索命令或标签") },
                singleLine = true
            )
            IconButton(onClick = { vm.refreshCommands(query) }) {
                Icon(Icons.Default.Search, contentDescription = "搜索命令")
            }
        }
        if (vm.commands.isEmpty()) {
            EmptyState("暂无命令记录")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(vm.commands, key = { it.id ?: it.cmd }) { item ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(item.cmd, fontWeight = FontWeight.Medium)
                                val details = listOfNotNull(
                                    item.alias,
                                    item.tags?.takeIf { it.isNotEmpty() }?.joinToString(" ")
                                )
                                    .joinToString(" · ")
                                if (details.isNotBlank()) Text(details, style = MaterialTheme.typography.bodySmall)
                            }
                            if (!item.id.isNullOrBlank()) {
                                IconButton(onClick = { vm.deleteCommand(item.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "删除命令")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun JsonScreen(modifier: Modifier) {
    var input by remember { mutableStateOf("") }
    var output by remember { mutableStateOf("") }
    var compact by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    val formatter = remember(compact) { Json { prettyPrint = !compact } }
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            input,
            { input = it; error = "" },
            modifier = Modifier.weight(1f).fillMaxWidth(),
            label = { Text("输入 JSON") },
            placeholder = { Text("{\"hello\":\"world\"}") },
            minLines = 4
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("压缩输出")
            Spacer(Modifier.width(8.dp))
            Switch(checked = compact, onCheckedChange = { compact = it })
            Spacer(Modifier.weight(1f))
            Button(onClick = {
                try {
                    val parsed = Json.parseToJsonElement(input)
                    output = formatter.encodeToString(JsonElement.serializer(), parsed)
                    error = ""
                } catch (exception: SerializationException) {
                    error = exception.message ?: "JSON 格式无效"
                    output = ""
                }
            }) { Text("格式化") }
        }
        if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
        OutlinedTextField(
            output,
            {},
            modifier = Modifier.weight(1f).fillMaxWidth(),
            label = { Text("输出") },
            readOnly = true,
            minLines = 4
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { input = ""; output = ""; error = "" }, modifier = Modifier.weight(1f)) {
                Text("清空")
            }
            OutlinedButton(onClick = { input = output }, modifier = Modifier.weight(1f)) {
                Text("输出转输入")
            }
        }
    }
}

@Composable
private fun FilesScreen(modifier: Modifier, onRead: (String) -> Unit) {
    val context = LocalContext.current
    var status by remember { mutableStateOf("通过系统文件选择器读取文本、JSON 或 XML 文件。") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            try {
                val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: error("无法打开所选文件")
                onRead(text)
                status = "已读取 ${text.length} 个字符"
            } catch (exception: Exception) {
                status = "读取失败：${exception.message ?: "未知错误"}"
            }
        }
    }
    Column(
        modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(Icons.Default.FolderOpen, contentDescription = null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
        Text("本地文件", style = MaterialTheme.typography.headlineSmall)
        Text("使用 Android 系统文件选择器，不需要申请共享存储权限。")
        Button(onClick = { picker.launch(arrayOf("text/*", "application/json", "application/xml")) }) {
            Text("选择文件")
        }
        Text(status, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun SettingsScreen(
    modifier: Modifier,
    url: String,
    onUrl: (String) -> Unit,
    username: String,
    onUsername: (String) -> Unit,
    password: String,
    onPassword: (String) -> Unit,
    onSave: () -> Unit
) {
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("后端连接", style = MaterialTheme.typography.headlineSmall)
        Text("Android 模拟器访问开发电脑通常使用 http://10.0.2.2:38080。")
        OutlinedTextField(
            url,
            onUrl,
            Modifier.fillMaxWidth(),
            label = { Text("后端根地址") },
            placeholder = { Text("http://10.0.2.2:38080") },
            singleLine = true
        )
        OutlinedTextField(username, onUsername, Modifier.fillMaxWidth(), label = { Text("用户名（可选）") }, singleLine = true)
        OutlinedTextField(
            password,
            onPassword,
            Modifier.fillMaxWidth(),
            label = { Text("密码（可选）") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true
        )
        Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) { Text("保存并连接") }
        Text("接口与桌面端一致：工具、便笺、待办和命令助手使用 Sourcebox 服务端 API。")
        Text("使用局域网或生产服务时，建议启用 HTTPS 并保护服务端访问凭据。", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun EmptyState(text: String) {
    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}
