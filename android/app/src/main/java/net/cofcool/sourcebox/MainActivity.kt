package net.cofcool.sourcebox

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.cofcool.sourcebox.data.ActionCheckList
import net.cofcool.sourcebox.data.ActionRecord
import net.cofcool.sourcebox.data.NoteRecord
import net.cofcool.sourcebox.data.TodoRecord
import net.cofcool.sourcebox.data.ToolInfo
import net.cofcool.sourcebox.ui.ToolboxViewModel

class MainActivity : ComponentActivity() {
    private val sharedText = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedText.value = intent.sharedPlainText()
        setContent {
            SourceboxApp(
                sharedText = sharedText.value,
                onShareConsumed = { sharedText.value = null }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        sharedText.value = intent.sharedPlainText()
    }
}

private fun Intent.sharedPlainText(): String? {
    val mimeType = type?.substringBefore(';')?.trim()
    if (action != Intent.ACTION_SEND || !mimeType.equals("text/plain", ignoreCase = true)) return null
    return getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()?.takeIf(String::isNotBlank)
}

private val pages = listOf(
    "tools" to "工具",
    "todos" to "待办",
    "notes" to "便笺",
    "task" to "任务"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SourceboxApp(
    sharedText: String?,
    onShareConsumed: () -> Unit,
    vm: ToolboxViewModel = viewModel()
) {
    var page by remember { mutableStateOf("tools") }
    var showShareTargetDialog by remember { mutableStateOf(false) }
    var selectedTool by remember { mutableStateOf<ToolInfo?>(null) }
    var selectedRecord by remember { mutableStateOf<ActionRecord?>(null) }
    var recordReturnPage by remember { mutableStateOf("task") }
    var appliedCategory by remember { mutableStateOf<String?>(null) }
    var appliedLabel by remember { mutableStateOf<String?>(null) }
    var settingsUrl by remember { mutableStateOf(vm.baseUrl) }
    var settingsUser by remember { mutableStateOf(vm.username) }
    var settingsPassword by remember { mutableStateOf(vm.password) }
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) coroutineScope.launch {
            snackbar.showSnackbar("通知权限未开启，番茄钟阶段结束时无法显示通知")
        }
    }
    val requestNotificationPermission = {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(vm.message) {
        if (vm.message.isNotBlank()) snackbar.showSnackbar(vm.message)
    }
    LaunchedEffect(vm.savedRecord) {
        val saved = vm.savedRecord
        if (saved != null && selectedRecord?.id == saved.id) selectedRecord = saved
    }
    LaunchedEffect(sharedText) {
        showShareTargetDialog = !sharedText.isNullOrBlank()
    }
    BackHandler(enabled = page == "tool") {
        page = "tools"
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
                                "tool" -> selectedTool?.id ?: "工具"
                                "task" -> "任务"
                                "record-detail" -> "编辑任务"
                                "timer" -> "番茄钟"
                                "files" -> "文件工具"
                                "settings" -> "连接设置"
                                "crash-logs" -> "崩溃日志"
                                else -> pages.firstOrNull { it.first == page }?.second ?: "The Source Box"
                            }
                        )
                    },
                    navigationIcon = {
                        if (page !in pages.map { it.first }) {
                            IconButton(onClick = {
                                page = if (page == "record-detail") {
                                    recordReturnPage
                                } else if (page == "crash-logs") {
                                    "settings"
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
                    onTimer = { page = "timer" },
                    onFiles = { page = "files" }
                )
                "tool" -> selectedTool?.let { tool ->
                    WebToolScreen(
                        url = "${vm.baseUrl.trimEnd('/')}/web/#/tools/${tool.id}",
                        username = vm.username,
                        password = vm.password,
                        modifier = Modifier.padding(padding)
                    )
                }
                "record-detail" -> selectedRecord?.let { record ->
                    RecordDetails(
                        modifier = Modifier.padding(padding),
                        record = record,
                        categories = vm.recordCategories,
                        labels = vm.recordLabels,
                        comments = vm.recordComments,
                        checklists = vm.recordChecklists,
                        busy = vm.busy,
                        onRecordChange = { selectedRecord = it },
                        onSave = { vm.saveRecord(record, "", 1, appliedCategory, appliedLabel) },
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
                    appliedCategory = appliedCategory,
                    appliedLabel = appliedLabel,
                    sharedText = sharedText,
                    onShareConsumed = onShareConsumed,
                    onFilterChanged = { category, label ->
                        appliedCategory = category
                        appliedLabel = label
                    },
                    onOpenRecord = {
                        selectedRecord = it
                        recordReturnPage = "task"
                        vm.loadRecordDetails(it)
                        page = "record-detail"
                    }
                )
                "todos" -> TodosScreen(
                    vm = vm,
                    modifier = Modifier.padding(padding),
                    sharedText = sharedText,
                    onShareConsumed = onShareConsumed
                )
                "notes" -> NotesScreen(vm, Modifier.padding(padding))
                "timer" -> TimerScreen(
                    vm = vm,
                    modifier = Modifier.padding(padding),
                    onRequestNotificationPermission = requestNotificationPermission
                )
                "files" -> FilesScreen(
                    modifier = Modifier.padding(padding),
                    vm = vm
                )
                "settings" -> SettingsScreen(
                    modifier = Modifier.padding(padding),
                    url = settingsUrl,
                    onUrl = { settingsUrl = it },
                    username = settingsUser,
                    onUsername = { settingsUser = it },
                    password = settingsPassword,
                    onPassword = { settingsPassword = it },
                    onSave = { vm.saveConnection(settingsUrl, settingsUser, settingsPassword); page = "tools" },
                    onViewCrashLogs = { page = "crash-logs" }
                )
                "crash-logs" -> CrashLogsScreen(Modifier.padding(padding))
            }
        }
        if (showShareTargetDialog && !sharedText.isNullOrBlank()) {
            AlertDialog(
                onDismissRequest = {
                    showShareTargetDialog = false
                    onShareConsumed()
                },
                title = { Text("分享文本") },
                text = { Text("将分享的内容添加到哪里？") },
                confirmButton = {
                    Row {
                        TextButton(onClick = {
                            page = "todos"
                            showShareTargetDialog = false
                        }) {
                            Text("待办")
                        }
                        TextButton(onClick = {
                            page = "task"
                            showShareTargetDialog = false
                        }) {
                            Text("任务")
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showShareTargetDialog = false
                        onShareConsumed()
                    }) {
                        Text("取消")
                    }
                }
            )
        }
    }
}

@Composable
private fun HomeScreen(
    vm: ToolboxViewModel,
    modifier: Modifier,
    onSelectTool: (ToolInfo) -> Unit,
    onTimer: () -> Unit,
    onFiles: () -> Unit
) {
    Column(modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp)) {
        Text("TheSourceBox", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            if (vm.baseUrl.isBlank()) "尚未连接后端" else "后端：${vm.baseUrl}",
            style = MaterialTheme.typography.bodySmall
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 12.dp)) {
            OutlinedButton(onClick = onTimer, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Timer, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("番茄钟")
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
                items(
                    vm.tools.filterNot { it.id in setOf("note", "todo", "hisRecord", "timer") },
                    key = { it.id }
                ) { tool ->
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

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun WebToolScreen(
    url: String,
    username: String,
    password: String,
    modifier: Modifier
) {
    val context = LocalContext.current
    var webView by remember(url) { mutableStateOf<WebView?>(null) }
    var loading by remember(url) { mutableStateOf(true) }
    var loadError by remember(url) { mutableStateOf<String?>(null) }

    DisposableEffect(url) {
        onDispose {
            webView?.apply {
                stopLoading()
                destroy()
            }
        }
    }

    Box(modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView, newProgress: Int) {
                            loading = newProgress < 100
                        }

                        override fun onConsoleMessage(message: android.webkit.ConsoleMessage): Boolean {
                            if (message.messageLevel() == android.webkit.ConsoleMessage.MessageLevel.ERROR) {
                                loadError = "网页脚本错误：${message.message()}（第 ${message.lineNumber()} 行）"
                            }
                            return true
                        }
                    }
                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView, pageUrl: String?, favicon: android.graphics.Bitmap?) {
                            loading = true
                            loadError = null
                        }

                        override fun onPageFinished(view: WebView, pageUrl: String?) {
                            loading = false
                        }

                        override fun onReceivedError(
                            view: WebView,
                            request: android.webkit.WebResourceRequest,
                            error: android.webkit.WebResourceError
                        ) {
                            if (request.isForMainFrame) {
                                loading = false
                                loadError = "网页加载失败：${error.description}（错误码 ${error.errorCode}）"
                            }
                        }

                        override fun onReceivedHttpError(
                            view: WebView,
                            request: android.webkit.WebResourceRequest,
                            response: android.webkit.WebResourceResponse
                        ) {
                            if (request.isForMainFrame && response.statusCode >= 400) {
                                loading = false
                                loadError = "网页请求失败：HTTP ${response.statusCode}"
                            }
                        }

                        override fun onReceivedHttpAuthRequest(
                            view: WebView,
                            handler: android.webkit.HttpAuthHandler,
                            host: String,
                            realm: String
                        ) {
                            if (username.isNotBlank()) handler.proceed(username, password)
                            else handler.cancel()
                        }
                    }
                    setBackgroundColor(android.graphics.Color.WHITE)
                    webView = this
                    loadUrl(url)
                }
            },
            update = { view ->
                Log.d("webview", "WebToolScreen: $url")
                if (view.url != url) {
                    loading = true
                    loadError = null
                    view.loadUrl(url)
                }
            }
        )
        if (loading && loadError == null) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        }
        loadError?.let { error ->
            Column(
                Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(error, color = MaterialTheme.colorScheme.error)
                Text("页面地址：$url", style = MaterialTheme.typography.bodySmall)
                OutlinedButton(
                    onClick = {
                        loadError = null
                        loading = true
                        webView?.reload()
                    }
                ) {
                    Text("重新加载")
                }
            }
        }
    }
}

@Composable
private fun RecordBoardScreen(
    vm: ToolboxViewModel,
    modifier: Modifier,
    appliedCategory: String?,
    appliedLabel: String?,
    sharedText: String?,
    onShareConsumed: () -> Unit,
    onFilterChanged: (String?, String?) -> Unit,
    onOpenRecord: (ActionRecord) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var appliedQuery by remember { mutableStateOf("") }
    var page by remember { mutableStateOf(1) }
    var newRecordName by remember { mutableStateOf("") }
    var copyQuery by remember { mutableStateOf("") }
    var copySource by remember { mutableStateOf<ActionRecord?>(null) }

    LaunchedEffect(sharedText) {
        if (!sharedText.isNullOrBlank()) {
            newRecordName = sharedText
            onShareConsumed()
        }
    }

    LaunchedEffect(copyQuery) {
        delay(250)
        vm.searchRecordCandidates(copyQuery)
    }

    LaunchedEffect(appliedQuery, appliedCategory, appliedLabel, page) {
        vm.refreshRecords(appliedQuery, page, appliedCategory, appliedLabel)
    }
    LaunchedEffect(Unit) {
        vm.refreshRecordCategories()
    }
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
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
        Text("行动 ${vm.localActionCount} · 便笺 ${vm.localNoteCount}", style = MaterialTheme.typography.labelSmall)
        Text(
            "便笺：${vm.lastNoteSync ?: "尚未同步"} · 类型：${vm.lastTypeSync ?: "尚未同步"}",
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
            maxLines = 4
        )
        OutlinedTextField(
            value = copyQuery,
            onValueChange = {
                copyQuery = it
                copySource = null
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("搜索要复制的卡片") },
            placeholder = { Text("输入卡片名称") },
            singleLine = true
        )
        if (vm.recordCandidates.isNotEmpty()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 220.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                vm.recordCandidates.forEach { candidate ->
                    Card(
                        onClick = {
                            copySource = candidate
                            newRecordName = candidate.name
                            copyQuery = candidate.name
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            candidate.name,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
        Button(
            onClick = {
                vm.createRecord(newRecordName, appliedQuery, page, appliedCategory, appliedLabel, copySource)
                newRecordName = ""
                copyQuery = ""
                copySource = null
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
                    onFilterChanged(null, null)
                    page = 1
                },
                enabled = !vm.busy
            ) { Icon(Icons.Default.Search, contentDescription = "查询记录") }
        }

        if (vm.records.isEmpty()) {
            EmptyState(
                if (appliedQuery.isNotBlank() || appliedCategory != null || appliedLabel != null) {
                    "没有匹配的记录"
                } else {
                    "还没有记录，添加一条开始使用"
                }
            )
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
                            record.category?.takeIf(String::isNotBlank)?.let {
                                AssistChipLabel(it) {
                                    query = ""
                                    appliedQuery = ""
                                    onFilterChanged(it, null)
                                    page = 1
                                }
                            }
                            record.labels.orEmpty().split(",").map(String::trim).filter(String::isNotBlank)
                                .forEach { label ->
                                    AssistChipLabel(label) {
                                        query = ""
                                        appliedQuery = ""
                                        onFilterChanged(null, label)
                                        page = 1
                                    }
                                }
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
private fun AssistChipLabel(text: String, onClick: () -> Unit) {
    androidx.compose.material3.AssistChip(
        onClick = onClick,
        label = { Text(text) }
    )
}

@Composable
private fun RecordDetails(
    modifier: Modifier,
    record: ActionRecord,
    categories: List<String>,
    labels: List<String>,
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
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
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
            EditableDropdownField(
                value = record.labels.orEmpty(),
                options = labels,
                label = "标签",
                supportingText = "多个标签以逗号分隔；可选择已有标签或手动填写",
                onValueChange = { onRecordChange(record.copy(labels = it)) },
                onOptionSelected = { selected ->
                    val current = record.labels.orEmpty().split(",")
                        .map(String::trim)
                        .filter(String::isNotBlank)
                    val updated = (current + selected).distinct().joinToString(", ")
                    onRecordChange(record.copy(labels = updated))
                }
            )
            EditableDropdownField(
                value = record.category.orEmpty(),
                options = categories,
                label = "分类",
                supportingText = "可选择已有分类或手动填写",
                onValueChange = { onRecordChange(record.copy(category = it.ifBlank { null })) },
                onOptionSelected = { onRecordChange(record.copy(category = it)) }
            )
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
private fun EditableDropdownField(
    value: String,
    options: List<String>,
    label: String,
    supportingText: String,
    onValueChange: (String) -> Unit,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember(label) { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(label) },
            supportingText = { Text(supportingText) },
            singleLine = true,
            trailingIcon = if (options.isNotEmpty()) {
                {
                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "选择$label")
                    }
                }
            } else {
                null
            }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 280.dp)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
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
private fun TodosScreen(
    vm: ToolboxViewModel,
    modifier: Modifier,
    sharedText: String?,
    onShareConsumed: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var remark by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }

    LaunchedEffect(sharedText) {
        if (!sharedText.isNullOrBlank()) {
            name = sharedText
            onShareConsumed()
        }
    }

    Column(modifier
        .fillMaxSize()
        .padding(16.dp)) {
        OutlinedTextField(
            name,
            { name = it },
            Modifier.fillMaxWidth(),
            label = { Text("待办事项") },
            maxLines = 4
        )
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
            LazyColumn(Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                CopyableLinkText(todo.name, fontWeight = FontWeight.SemiBold)
                if (!todo.remark.isNullOrBlank()) {
                    CopyableLinkText(todo.remark, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun CopyableLinkText(
    text: String,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium,
    fontWeight: FontWeight? = null
) {
    val linkColor = MaterialTheme.colorScheme.primary
    val annotatedText = remember(text, linkColor) {
        val urlPattern = Regex("""(?i)(https?://[^\s<>"']+|www\.[^\s<>"']+)""")
        buildAnnotatedString {
            var position = 0
            urlPattern.findAll(text).forEach { match ->
                val rawUrl = match.value
                val link = rawUrl.trimEnd('.', ',', ';', ':', '!', '?', ')', ']', '}')
                val linkEnd = match.range.first + link.length
                if (linkEnd <= position) return@forEach
                append(text.substring(position, match.range.first))
                withLink(
                    LinkAnnotation.Url(
                        if (link.startsWith("www.", ignoreCase = true)) "https://$link" else link,
                        TextLinkStyles(
                            SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)
                        )
                    )
                ) {
                    append(link)
                }
                position = linkEnd
            }
            append(text.substring(position))
        }
    }

    SelectionContainer {
        Text(
            text = annotatedText,
            modifier = modifier,
            style = style,
            fontWeight = fontWeight
        )
    }
}

@Composable
private fun NotesScreen(vm: ToolboxViewModel, modifier: Modifier) {
    var selected by remember { mutableStateOf<NoteRecord?>(null) }
    var content by remember { mutableStateOf("") }
    Column(modifier
        .fillMaxSize()
        .padding(16.dp)) {
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
private fun TimerScreen(
    vm: ToolboxViewModel,
    modifier: Modifier,
    onRequestNotificationPermission: () -> Unit
) {
    val timer = vm.timerState
    var workInput by remember(timer.workMinutes) { mutableStateOf(timer.workMinutes.toString()) }
    var breakInput by remember(timer.breakMinutes) { mutableStateOf(timer.breakMinutes.toString()) }

    LaunchedEffect(Unit) {
        vm.refreshTimerRecords()
    }

    val totalFocusSeconds = vm.timerRecords
        .filter { it.name.startsWith("workTime-") }
        .sumOf { it.duration ?: 0 }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text(
            when {
                timer.isPaused -> if (timer.isWorkPhase) "专注已暂停" else "休息已暂停"
                timer.isRunning -> if (timer.isWorkPhase) "专注中" else "休息中"
                else -> if (timer.isWorkPhase) "准备专注" else "准备休息"
            },
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            "%02d:%02d".format(timer.remainingSeconds / 60, timer.remainingSeconds % 60),
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = workInput,
                onValueChange = { workInput = it.filter(Char::isDigit).take(3) },
                modifier = Modifier.weight(1f),
                label = { Text("专注（分钟）") },
                keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                enabled = !timer.isRunning && !timer.isPaused,
                singleLine = true
            )
            OutlinedTextField(
                value = breakInput,
                onValueChange = { breakInput = it.filter(Char::isDigit).take(3) },
                modifier = Modifier.weight(1f),
                label = { Text("休息（分钟）") },
                keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                enabled = !timer.isRunning && !timer.isPaused,
                singleLine = true
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = {
                    if (timer.isRunning) {
                        vm.pausePomodoro()
                    } else {
                        onRequestNotificationPermission()
                        vm.startPomodoro(
                            workInput.toIntOrNull() ?: 0,
                            breakInput.toIntOrNull() ?: 0
                        )
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(if (timer.isRunning) "暂停" else if (timer.isPaused) "继续" else "开始")
            }
            OutlinedButton(onClick = vm::resetPomodoro, modifier = Modifier.weight(1f)) {
                Text("重置")
            }
        }
        Text("已完成 ${timer.completedCycles} 个周期 · 累计专注 ${formatTimerDuration(totalFocusSeconds.toLong())}")

        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("最近阶段记录", style = MaterialTheme.typography.titleMedium)
            if (vm.timerRecords.isEmpty()) {
                Text("暂无计时记录", style = MaterialTheme.typography.bodySmall)
            } else {
                vm.timerRecords.forEach { record ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                when {
                                    record.name.startsWith("workTime-") -> "专注 · ${record.remark.orEmpty()}"
                                    else -> "休息 · ${record.remark.orEmpty()}"
                                }
                            )
                            Text(record.end.orEmpty(), style = MaterialTheme.typography.labelSmall)
                        }
                        Text(formatTimerDuration(record.duration?.toLong() ?: 0))
                    }
                }
            }
        }
    }
}

private fun formatTimerDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val remainingSeconds = seconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, remainingSeconds)
    else "%d:%02d".format(minutes, remainingSeconds)
}

@Composable
private fun FilesScreen(modifier: Modifier, vm: ToolboxViewModel) {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("lan_share", android.content.Context.MODE_PRIVATE) }
    var receiverUrl by remember { mutableStateOf(preferences.getString("receiver_url", "").orEmpty()) }
    var token by remember { mutableStateOf(preferences.getString("token", "").orEmpty()) }
    var selectedFiles by remember { mutableStateOf<List<Uri>>(emptyList()) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { files ->
        selectedFiles = files
    }

    fun uploadSelectedFiles() {
        preferences.edit()
            .putString("receiver_url", receiverUrl.trim())
            .putString("token", token.trim())
            .apply()
        vm.uploadLanShareFiles(receiverUrl.trim(), token.trim(), selectedFiles)
    }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            Icons.Default.FolderOpen,
            contentDescription = null,
            Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text("LAN 文件分享", style = MaterialTheme.typography.headlineSmall)
        Text("填写 lanShare 接收端输出的 Share URL 和 Token。接收端地址需包含端口，例如 http://192.168.1.20:43127。")
        OutlinedTextField(
            value = receiverUrl,
            onValueChange = { receiverUrl = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("接收端 Share URL") },
            placeholder = { Text("http://192.168.1.20:43127") },
            singleLine = true,
            enabled = !vm.busy
        )
        OutlinedTextField(
            value = token,
            onValueChange = { token = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Token") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            enabled = !vm.busy
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { picker.launch(arrayOf("*/*")) },
                enabled = !vm.busy
            ) {
                Text("选择多个文件")
            }
            Button(
                onClick = ::uploadSelectedFiles,
                enabled = selectedFiles.isNotEmpty() && receiverUrl.isNotBlank() && token.isNotBlank() && !vm.busy
            ) {
                Text("上传 ${selectedFiles.size} 个文件")
            }
        }
        if (selectedFiles.isNotEmpty()) {
            Text("已选择 ${selectedFiles.size} 个文件")
            selectedFiles.forEach { uri ->
                val fileName = remember(uri) {
                    context.contentResolver.query(
                        uri,
                        arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
                        null,
                        null,
                        null
                    )?.use { cursor ->
                        if (cursor.moveToFirst()) cursor.getString(0) else null
                    } ?: uri.lastPathSegment.orEmpty()
                }
                Text(fileName, style = MaterialTheme.typography.bodySmall)
            }
        }
        val uploadState = vm.lanShareUploadState
        if (uploadState.totalFiles > 0) {
            Text(
                when {
                    uploadState.error != null -> "上传失败：${uploadState.error}"
                    uploadState.isComplete -> "上传完成：${uploadState.completedFiles.size}/${uploadState.totalFiles}"
                    uploadState.currentFileIndex > 0 ->
                        "正在上传 ${uploadState.currentFileIndex}/${uploadState.totalFiles}：${uploadState.currentFileName}"
                    else -> "准备上传 ${uploadState.totalFiles} 个文件"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (uploadState.error != null) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurface
            )
            if (vm.busy && uploadState.currentFileIndex > 0) {
                val progress = if (uploadState.totalBytes > 0) {
                    (uploadState.bytesSent.toFloat() / uploadState.totalBytes).coerceIn(0f, 1f)
                } else {
                    null
                }
                if (progress != null) {
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                    Text(
                        "${formatFileSize(uploadState.bytesSent)} / ${formatFileSize(uploadState.totalBytes)} · ${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall
                    )
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text("${formatFileSize(uploadState.bytesSent)} 已发送", style = MaterialTheme.typography.labelSmall)
                }
            }
            if (uploadState.completedFiles.isNotEmpty()) {
                Text(
                    "已完成文件 (${uploadState.completedFiles.size}/${uploadState.totalFiles})",
                    style = MaterialTheme.typography.titleSmall
                )
                uploadState.completedFiles.forEach { completed ->
                    Text("✓ $completed", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val units = listOf("KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unitIndex = -1
    do {
        value /= 1024
        unitIndex++
    } while (value >= 1024 && unitIndex < units.lastIndex)
    return "%.1f %s".format(value, units[unitIndex])
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
    onSave: () -> Unit,
    onViewCrashLogs: () -> Unit
) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
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
        OutlinedButton(onClick = onViewCrashLogs, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.ErrorOutline, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("查看崩溃日志")
        }
        Text("接口与桌面端一致，使用 TheSourceBox 服务端 API。")
        Text("使用局域网或生产服务时，建议启用 HTTPS 并保护服务端访问凭据。", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun CrashLogsScreen(modifier: Modifier) {
    val context = LocalContext.current
    val store = remember(context) { CrashLogStore(context) }
    val scope = rememberCoroutineScope()
    var logs by remember { mutableStateOf<List<CrashLog>>(emptyList()) }
    var selectedLog by remember { mutableStateOf<CrashLog?>(null) }
    var selectedContent by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    fun refreshLogs() {
        scope.launch {
            loading = true
            error = ""
            try {
                logs = withContext(Dispatchers.IO) { store.list() }
                if (selectedLog?.name !in logs.map { it.name }) {
                    selectedLog = null
                    selectedContent = ""
                }
            } catch (exception: Exception) {
                error = "加载崩溃日志失败：${exception.message ?: "未知错误"}"
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(store) { refreshLogs() }

    Column(
        modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("崩溃日志（${logs.size}）", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
            IconButton(onClick = ::refreshLogs, enabled = !loading) {
                Icon(Icons.Default.Refresh, contentDescription = "刷新")
            }
            TextButton(
                onClick = {
                    scope.launch {
                        try {
                            withContext(Dispatchers.IO) { store.deleteAll() }
                            logs = emptyList()
                            selectedLog = null
                            selectedContent = ""
                            error = ""
                        } catch (exception: Exception) {
                            error = "清理崩溃日志失败：${exception.message ?: "未知错误"}"
                        }
                    }
                },
                enabled = logs.isNotEmpty() && !loading
            ) { Text("清空") }
        }
        Text(
            "崩溃日志仅保存在本机，最多保留最近 20 份。",
            style = MaterialTheme.typography.bodySmall
        )
        if (error.isNotBlank()) {
            Text(error, color = MaterialTheme.colorScheme.error)
        } else if (logs.isEmpty() && !loading) {
            EmptyState("暂无崩溃日志")
        }
        if (loading) CircularProgressIndicator(Modifier.size(24.dp))
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(logs, key = { it.name }) { log ->
                Card(
                    onClick = {
                        scope.launch {
                            try {
                                selectedContent = withContext(Dispatchers.IO) { store.read(log) }
                                selectedLog = log
                                error = ""
                            } catch (exception: Exception) {
                                error = "读取崩溃日志失败：${exception.message ?: "未知错误"}"
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier
                            .weight(1f)
                            .padding(vertical = 12.dp)) {
                            Text(log.name, fontWeight = FontWeight.SemiBold)
                            Text(
                                java.text.SimpleDateFormat(
                                    "yyyy-MM-dd HH:mm:ss",
                                    java.util.Locale.getDefault()
                                ).format(java.util.Date(log.timestamp)),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        IconButton(
                            onClick = {
                                scope.launch {
                                    try {
                                        withContext(Dispatchers.IO) { store.delete(log) }
                                        if (selectedLog?.name == log.name) {
                                            selectedLog = null
                                            selectedContent = ""
                                        }
                                        logs = withContext(Dispatchers.IO) { store.list() }
                                        error = ""
                                    } catch (exception: Exception) {
                                        error = "删除崩溃日志失败：${exception.message ?: "未知错误"}"
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "删除日志")
                        }
                    }
                }
            }
        }
        selectedLog?.let { log ->
            Text(log.name, style = MaterialTheme.typography.titleMedium)
            SelectionContainer {
                Text(
                    selectedContent,
                    Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .verticalScroll(rememberScrollState()),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun EmptyState(text: String) {
    Box(Modifier
        .fillMaxWidth()
        .padding(24.dp), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}
