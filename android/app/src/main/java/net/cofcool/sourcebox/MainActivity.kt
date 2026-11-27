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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
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
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.request.crossfade
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.cofcool.sourcebox.data.ActionCheckList
import net.cofcool.sourcebox.data.ActionDimensionCount
import net.cofcool.sourcebox.data.ActionRecord
import net.cofcool.sourcebox.data.ActionStatistics
import net.cofcool.sourcebox.data.NoteRecord
import net.cofcool.sourcebox.data.PhotoAlbum
import net.cofcool.sourcebox.data.PhotoBackupConfig
import net.cofcool.sourcebox.data.PhotoRecord
import net.cofcool.sourcebox.data.TodoRecord
import net.cofcool.sourcebox.data.ToolInfo
import net.cofcool.sourcebox.data.isHabitRequiredOnDate
import net.cofcool.sourcebox.ui.ToolboxViewModel
import okio.Path.Companion.toOkioPath
import java.io.IOException
import java.time.LocalDate

@OptIn(coil3.annotation.DelicateCoilApi::class)
private fun configurePhotoDiskCache(context: android.content.Context) {
    val preferences = context.getSharedPreferences("sourcebox", android.content.Context.MODE_PRIVATE)
    val sizeMb = preferences.getInt("photo_disk_cache_size_mb", 100).coerceIn(10, 1024)
    val diskCache = DiskCache.Builder()
        .directory(context.cacheDir.resolve("photo-disk-cache").toOkioPath())
        .maxSizeBytes(sizeMb.toLong() * 1024 * 1024)
        .build()
    val loader = ImageLoader.Builder(context)
        .diskCache(diskCache)
        .memoryCache {
            MemoryCache.Builder()
                .maxSizePercent(context, 0.25)
                .build()
        }
        .crossfade(true)
        .build()
    SingletonImageLoader.setUnsafe(loader)
}

class MainActivity : ComponentActivity() {
    private val sharedText = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configurePhotoDiskCache(applicationContext)
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
    "tools" to "今日",
    "todos" to "待办",
    "habits" to "习惯",
    "task" to "任务",
    "more" to "更多"
)

private val moreToolPages = setOf(
    "timer", "files", "files-download", "books", "photos", "photo-detail", "photo-backup",
    "settings", "server-tools", "statistics"
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
    var selectedTodo by remember { mutableStateOf<TodoRecord?>(null) }
    var selectedHabit by remember { mutableStateOf<ActionRecord?>(null) }
    var selectedPhotoAlbum by remember { mutableStateOf<PhotoAlbum?>(null) }
    var selectedPhoto by remember { mutableStateOf<PhotoRecord?>(null) }
    var todoReturnPage by remember { mutableStateOf("todos") }
    var habitReturnPage by remember { mutableStateOf("habits") }
    var showArchivedHabits by remember { mutableStateOf(false) }
    var showCompletedTodos by remember { mutableStateOf(false) }
    var recordReturnPage by remember { mutableStateOf("task") }
    var appliedCategory by remember { mutableStateOf<String?>(null) }
    var appliedLabel by remember { mutableStateOf<String?>(null) }
    var settingsUrl by remember { mutableStateOf(vm.baseUrl) }
    var settingsUser by remember { mutableStateOf(vm.username) }
    var settingsPassword by remember { mutableStateOf(vm.password) }
    var photoCacheSizeMb by remember { mutableStateOf(vm.photoDiskCacheSizeMb()) }
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
    val requestLocalNetPermission = {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN &&
            context.checkSelfPermission(Manifest.permission.ACCESS_LOCAL_NETWORK) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.ACCESS_LOCAL_NETWORK)
        }
    }
    fun navigateBack() {
        page = when (page) {
            "files-download" -> "files"
            "files-text-editor" -> "files"
            "photo-backup" -> "photos"
            "photo-detail" -> "photos"
            "photos" -> {
                if (selectedPhotoAlbum != null) {
                    selectedPhotoAlbum = null
                    "photos"
                } else {
                    "more"
                }
            }
            "record-detail" -> recordReturnPage
            "record-create" -> "task"
            "habit-detail" -> habitReturnPage
            "habit-create" -> "habits"
            "todo-detail" -> todoReturnPage
            "todo-create" -> "todos"
            "crash-logs" -> "settings"
            in moreToolPages -> "more"
            else -> "tools"
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
    LaunchedEffect(page) {
        if (page in setOf("todos", "task", "todo-detail", "record-detail")) {
            vm.refreshRecommendations()
        }
    }
    BackHandler(enabled = page !in pages.map { it.first }) {
        navigateBack()
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
                if (page != "photo-detail") TopAppBar(
                    title = {
                        Text(
                            when (page) {
                                "tool" -> selectedTool?.id ?: "工具"
                                "server-tools" -> "服务端工具"
                                "statistics" -> "数据统计"
                                "todo-detail" -> "待办详情"
                                "todo-create" -> "添加待办"
                                "task" -> "任务"
                                "habits" -> if (showArchivedHabits) "归档习惯" else "习惯"
                                "record-create" -> "添加任务"
                                "record-detail" -> "编辑任务"
                                "habit-detail" -> "习惯详情"
                                "habit-create" -> "添加习惯"
                                "books" -> "书籍记录"
                                "photos" -> "照片"
                                "photo-detail" -> "照片详情"
                                "photo-backup" -> "照片自动备份"
                                "timer" -> "番茄钟"
                                "files" -> "文件工具"
                                "files-text-editor" -> "文本编辑"
                                "files-download" -> "下载共享文件"
                                "settings" -> "连接设置"
                                "crash-logs" -> "异常日志"
                                else -> pages.firstOrNull { it.first == page }?.second ?: "The Source Box"
                            }
                        )
                    },
                    navigationIcon = {
                        if (page !in pages.map { it.first }) {
                            IconButton(onClick = {
                                navigateBack()
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                            }
                        }
                    },
                    actions = {
                        when (page) {
                            "photos" -> IconButton(onClick = { page = "photo-backup" }) {
                                Icon(Icons.Default.Backup, contentDescription = "自动备份")
                            }
                            "todo-detail" -> selectedTodo?.let { todo ->
                                IconButton(
                                    onClick = { vm.setRecommended(todo.id, todo.id !in vm.recommendedIds) },
                                    enabled = !vm.busy
                                ) {
                                    Icon(
                                        if (todo.id in vm.recommendedIds) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = if (todo.id in vm.recommendedIds) "取消推荐" else "推荐到首页"
                                    )
                                }
                            }
                            "record-detail" -> selectedRecord?.let { record ->
                                IconButton(
                                    onClick = { vm.setRecommended(record.id, record.id !in vm.recommendedIds) },
                                    enabled = !vm.busy
                                ) {
                                    Icon(
                                        if (record.id in vm.recommendedIds) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = if (record.id in vm.recommendedIds) "取消推荐" else "推荐到首页"
                                    )
                                }
                            }
                            "task" -> IconButton(onClick = { page = "record-create" }) {
                                Icon(Icons.Default.Add, contentDescription = "添加任务")
                            }
                            "files" -> IconButton(onClick = { page = "files-text-editor" }) {
                                Icon(Icons.Default.Edit, contentDescription = "文本编辑")
                            }
                            "todos" -> {
                                IconButton(onClick = { showCompletedTodos = !showCompletedTodos }) {
                                    Icon(
                                        if (showCompletedTodos) Icons.Default.Unarchive else Icons.Default.Archive,
                                        contentDescription = if (showCompletedTodos) "返回待办" else "查看已完成"
                                    )
                                }
                                IconButton(onClick = {
                                    showCompletedTodos = false
                                    selectedTodo = TodoRecord()
                                    page = "todo-create"
                                }) {
                                    Icon(Icons.Default.Add, contentDescription = "添加待办")
                                }
                            }
                            "habits" -> {
                                IconButton(onClick = { showArchivedHabits = !showArchivedHabits }) {
                                    Icon(
                                        if (showArchivedHabits) Icons.Default.Unarchive else Icons.Default.Archive,
                                        contentDescription = if (showArchivedHabits) {
                                            "查看进行中的习惯"
                                        } else {
                                            "查看归档习惯"
                                        }
                                    )
                                }
                                IconButton(onClick = {
                                    showArchivedHabits = false
                                    page = "habit-create"
                                }) {
                                    Icon(Icons.Default.Add, contentDescription = "添加习惯")
                                }
                            }
                        }
                    }
                )
            },
            bottomBar = {
                if (page !in setOf(
                        "record-detail", "record-create", "habit-detail", "habit-create",
                        "todo-detail", "todo-create", "files-text-editor", "photo-detail"
                    )
                ) {
                    NavigationBar {
                        pages.forEach { (destination, label) ->
                            val icon = when (destination) {
                                "tools" -> Icons.Default.LightMode
                                "todos" -> Icons.AutoMirrored.Filled.List
                                "habits" -> Icons.Default.Alarm
                                "task" -> Icons.Default.Build
                                else -> Icons.Default.MoreHoriz
                            }
                            NavigationBarItem(
                                selected = page == destination ||
                                    (destination == "more" && page in moreToolPages),
                                onClick = {
                                    page = destination
                                    when (destination) {
                                        "tools" -> vm.refreshTodayActions()
                                        "todos" -> vm.refreshTodos()
                                        "habits" -> vm.refreshHabits()
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
                    onOpenAction = { action ->
                        when (action.type) {
                            "todo" -> {
                                todoReturnPage = "tools"
                                selectedTodo = TodoRecord(
                                    id = action.id,
                                    name = action.name,
                                    state = action.state,
                                    remark = action.remark,
                                    type = action.type,
                                    category = action.category,
                                    refs = action.refs,
                                    start = action.start,
                                    end = action.end,
                                    createTime = action.createTime
                                )
                                page = "todo-detail"
                            }
                            "record" -> {
                                selectedRecord = action
                                recordReturnPage = "tools"
                                vm.loadRecordDetails(action)
                                page = "record-detail"
                            }
                            "habit" -> {
                                habitReturnPage = "tools"
                                selectedHabit = action
                                vm.loadHabitDetails(action)
                                page = "habit-detail"
                            }
                            "timer" -> {
                                vm.refreshTimerRecords()
                                page = "timer"
                            }
                            "book" -> {
                                vm.refreshBooks()
                                page = "books"
                            }
                        }
                    }
                )
                "more" -> MoreScreen(
                    vm = vm,
                    modifier = Modifier.padding(padding),
                    onNotes = { page = "notes"; vm.refreshNotes() },
                    onTimer = { page = "timer"; vm.refreshTimerRecords() },
                    onFiles = { page = "files" },
                    onBooks = { page = "books"; vm.refreshBooks() },
                    onPhotos = { page = "photos" },
                    onStatistics = { page = "statistics" },
                    onServerTools = { vm.refreshTools(); page = "server-tools" },
                    onSettings = {
                        settingsUrl = vm.baseUrl
                        settingsUser = vm.username
                        settingsPassword = vm.password
                        page = "settings"
                    },
                    onSyncData = requestLocalNetPermission
                )
                "photos" -> PhotosScreen(
                    vm = vm,
                    modifier = Modifier.padding(padding),
                    initialAlbum = selectedPhotoAlbum,
                    onCloseAlbum = { selectedPhotoAlbum = null },
                    onOpenAlbum = { selectedPhotoAlbum = it },
                    onOpenPhoto = { album, photo ->
                        selectedPhotoAlbum = album
                        selectedPhoto = photo
                        page = "photo-detail"
                    }
                )
                "statistics" -> StatisticsScreen(
                    vm = vm,
                    modifier = Modifier.padding(padding)
                )
                "photo-detail" -> selectedPhoto?.let { photo ->
                    PhotoDetailScreen(
                        vm = vm,
                        photo = photo,
                        albumId = selectedPhotoAlbum?.id.orEmpty(),
                        modifier = Modifier.padding(padding),
                        onPhotoChange = { selectedPhoto = it },
                        onDeleted = { page = "photos" }
                    )
                }
                "photo-backup" -> PhotoBackupScreen(
                    vm = vm,
                    modifier = Modifier.padding(padding)
                )
                "server-tools" -> ServerToolsScreen(
                    vm = vm,
                    modifier = Modifier.padding(padding),
                    onSelectTool = { selectedTool = it; page = "tool" }
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
                "record-create" -> RecordCreateScreen(
                    vm = vm,
                    modifier = Modifier.padding(padding),
                    sharedText = sharedText,
                    onShareConsumed = onShareConsumed,
                    onCancel = { page = "task" },
                    onCreated = {
                        appliedCategory = null
                        appliedLabel = null
                        page = "task"
                    }
                )
                "habit-create" -> HabitCreateScreen(
                    vm = vm,
                    modifier = Modifier.padding(padding),
                    onCancel = { page = "habits" },
                    onCreated = { page = "habits" }
                )
                "habit-detail" -> selectedHabit?.let { habit ->
                    HabitDetailsScreen(
                        modifier = Modifier.padding(padding),
                        habit = vm.habitRecordDetails,
                        busy = vm.busy,
                        onSaveHabit = vm::saveHabitDetails,
                        onSaveValue = { value -> vm.saveHabitValue(habit.id, value) }
                    )
                }
                "task" -> RecordBoardScreen(
                    vm,
                    Modifier.padding(padding),
                    appliedCategory = appliedCategory,
                    appliedLabel = appliedLabel,
                    onCreateRecord = { page = "record-create" },
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
                "habits" -> HabitBoardScreen(
                    vm = vm,
                    modifier = Modifier.padding(padding),
                    showArchived = showArchivedHabits,
                    onOpenHabit = { habit ->
                        habitReturnPage = "habits"
                        selectedHabit = habit
                        vm.loadHabitDetails(habit)
                        page = "habit-detail"
                    }
                )
                "books" -> BookRecordScreen(
                    vm = vm,
                    modifier = Modifier.padding(padding)
                )
                "todos" -> TodosScreen(
                    vm = vm,
                    modifier = Modifier.padding(padding),
                    showCompleted = showCompletedTodos,
                    onOpenTodo = {
                        todoReturnPage = "todos"
                        selectedTodo = it
                        page = "todo-detail"
                    }
                )
                "todo-detail" -> selectedTodo?.let { todo ->
                    TodoDetailsScreen(
                        todo = todo,
                        modifier = Modifier.padding(padding),
                        busy = vm.busy,
                        onSave = { updated ->
                            vm.saveTodo(updated) {
                                selectedTodo = updated
                                page = todoReturnPage
                            }
                        }
                    )
                }
                "todo-create" -> selectedTodo?.let { todo ->
                    TodoDetailsScreen(
                        todo = todo,
                        modifier = Modifier.padding(padding),
                        busy = vm.busy,
                        isNew = true,
                        onSave = { updated ->
                            vm.createTodo(updated) {
                                selectedTodo = updated
                                page = "todos"
                            }
                        }
                    )
                }
                "notes" -> NotesScreen(vm, Modifier.padding(padding))
                "timer" -> TimerScreen(
                    vm = vm,
                    modifier = Modifier.padding(padding),
                    onRequestNotificationPermission = requestNotificationPermission
                )
                "files" -> FilesScreen(
                    modifier = Modifier.padding(padding),
                    vm = vm,
                    downloadMode = false,
                    onDownload = {
                        vm.resetLanShareDownloadState()
                        page = "files-download"
                    },
                    onBackToUpload = {
                        vm.resetLanShareDownloadState()
                        page = "files"
                    }
                )
                "files-text-editor" -> FilesTextEditorScreen(
                    modifier = Modifier.padding(padding),
                    vm = vm
                )
                "files-download" -> FilesScreen(
                    modifier = Modifier.padding(padding),
                    vm = vm,
                    downloadMode = true,
                    onDownload = {
                        vm.resetLanShareDownloadState()
                        page = "files-download"
                    },
                    onBackToUpload = {
                        vm.resetLanShareDownloadState()
                        page = "files"
                    }
                )
                "settings" -> SettingsScreen(
                    modifier = Modifier.padding(padding),
                    url = settingsUrl,
                    onUrl = { settingsUrl = it },
                    username = settingsUser,
                    onUsername = { settingsUser = it },
                    password = settingsPassword,
                    onPassword = { settingsPassword = it },
                    onSave = { vm.saveConnection(settingsUrl, settingsUser, settingsPassword); page = "more" },
                    photoCacheSizeMb = photoCacheSizeMb,
                    onPhotoCacheSizeMb = { size ->
                        photoCacheSizeMb = size
                        vm.savePhotoDiskCacheSizeMb(size)
                        configurePhotoDiskCache(context)
                    },
                    onClearLocalDatabase = vm::clearLocalDatabase,
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
                            showCompletedTodos = false
                            selectedTodo = TodoRecord(name = sharedText)
                            page = "todo-create"
                            showShareTargetDialog = false
                            onShareConsumed()
                        }) {
                            Text("待办")
                        }
                        TextButton(onClick = {
                            page = "record-create"
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
    onOpenAction: (ActionRecord) -> Unit
) {
    LaunchedEffect(Unit) { vm.refreshTodayActions() }
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (vm.todayActions.isEmpty()) {
            EmptyState("今日暂无行动")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                vm.todayActions.forEach { action ->
                    TodayActionCard(
                        action = action,
                        isRecommended = action.id in vm.recommendedIds,
                        onClick = { onOpenAction(action) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TodayActionCard(action: ActionRecord, isRecommended: Boolean, onClick: () -> Unit) {
    val typeLabel = when (action.type) {
        "todo" -> "待办"
        "record" -> "任务"
        "habit" -> "习惯"
        "timer" -> "番茄钟"
        "book" -> "书籍"
        else -> action.type
    }
    val stateLabel = when (action.state) {
        "todo" -> "待办"
        "doing" -> "进行中"
        "done" -> "已完成"
        "cancel" -> "已取消"
        else -> action.state
    }
    val content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit = {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(action.name, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(8.dp))
                if (isRecommended) {
                    Icon(Icons.Default.Star, contentDescription = "star", tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(6.dp))
                }
                Text("$typeLabel · $stateLabel", style = MaterialTheme.typography.labelSmall)
            }
            action.remark?.takeIf(String::isNotBlank)?.let {
                CopyableLinkText(it, style = MaterialTheme.typography.bodySmall)
            }
            Text(action.start.orEmpty(), style = MaterialTheme.typography.labelSmall)
        }
    }
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), content = content)
}

@Composable
private fun ServerToolsScreen(
    vm: ToolboxViewModel,
    modifier: Modifier,
    onSelectTool: (ToolInfo) -> Unit
) {
    Column(
        modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            if (vm.baseUrl.isBlank()) "尚未连接后端" else "后端：${vm.baseUrl}",
            style = MaterialTheme.typography.bodySmall
        )
        if (vm.busy && vm.tools.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (vm.tools.isEmpty()) {
            EmptyState("请检查连接设置和服务状态。")
        } else {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                vm.tools.filterNot {
                    it.id in setOf("todo", "hisRecord", "bookRecord", "timer", "habits", "photo")
                }.forEach { tool ->
                    Card(onClick = { onSelectTool(tool) }, modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
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
private fun MoreScreen(
    vm: ToolboxViewModel,
    modifier: Modifier,
    onNotes: () -> Unit,
    onTimer: () -> Unit,
    onFiles: () -> Unit,
    onBooks: () -> Unit,
    onPhotos: () -> Unit,
    onStatistics: () -> Unit,
    onServerTools: () -> Unit,
    onSettings: () -> Unit,
    onSyncData: () -> Unit
) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("行动 ${vm.localActionCount}")
                    }
                    OutlinedButton(
                        onClick = {
                            onSyncData()
                            vm.syncAllData()
                        },
                        enabled = !vm.busy
                    ) {
                        Text("立即同步")
                    }
                }

                Text(
                    vm.lastActionSync?.let { "最近同步：$it" } ?: "尚未同步",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "待上传的离线修改：${vm.pendingChangeCount}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (vm.pendingChangeCount > 0) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        MoreToolCard(
            icon = Icons.Default.Timer,
            title = "番茄钟",
            description = "专注计时",
            onClick = onTimer
        )
        MoreToolCard(
            icon = Icons.Default.FolderOpen,
            title = "文件",
            description = "局域网文件同步",
            onClick = onFiles
        )
        MoreToolCard(
            icon = Icons.Default.Description,
            title = "书籍",
            description = "查看书籍记录",
            onClick = onBooks
        )
        MoreToolCard(
            icon = Icons.Default.Archive,
            title = "照片",
            description = "相册、照片查看与备份",
            onClick = onPhotos
        )
        MoreToolCard(
            icon = Icons.Default.Assessment,
            title = "数据统计",
            description = "按年份、状态和类别统计任务",
            onClick = onStatistics
        )
        MoreToolCard(
            icon = Icons.Default.Build,
            title = "服务端工具",
            description = "后端提供的其他工具",
            onClick = onServerTools
        )
        MoreToolCard(
            icon = Icons.Default.Settings,
            title = "设置",
            description = "管理后端连接等",
            onClick = onSettings
        )
    }
}

@Composable
private fun StatisticsScreen(
    vm: ToolboxViewModel,
    modifier: Modifier
) {
    val currentYear = remember { LocalDate.now().year }
    val years = remember(currentYear) { (currentYear downTo 2019).map(Int::toString) }
    var year by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var statistics by remember { mutableStateOf<ActionStatistics?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(year, state, category) {
        loading = true
        error = null
        try {
            statistics = vm.actionStatistics(year, state, category)
        } catch (e: Exception) {
            error = e.message ?: "加载统计数据失败"
        } finally {
            loading = false
        }
    }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatisticsFilter(
                    label = "年份",
                    selected = year,
                    allLabel = "全部年份",
                    options = years,
                    onSelected = { year = it }
                )
                StatisticsFilter(
                    label = "状态",
                    selected = state,
                    allLabel = "全部状态",
                    options = statistics?.statusOptions.orEmpty(),
                    onSelected = { state = it }
                )
                StatisticsFilter(
                    label = "类别",
                    selected = category,
                    allLabel = "全部类别",
                    options = statistics?.categoryOptions.orEmpty(),
                    onSelected = { category = it }
                )
            }
        }

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        if (loading && statistics == null) {
            Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            statistics?.let { result ->
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("筛选后的任务总数", style = MaterialTheme.typography.titleMedium)
                        Text(
                            result.total.toString(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                StatisticsGroup("类型统计", result.typeCounts)
                StatisticsGroup("类别统计", result.categoryCounts)
                StatisticsGroup("状态统计", result.statusCounts)
            }
        }
    }
}

@Composable
private fun StatisticsFilter(
    label: String,
    selected: String,
    allLabel: String,
    options: List<String>,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (selected.isBlank()) allLabel else selected)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.heightIn(max = 320.dp)
            ) {
                DropdownMenuItem(
                    text = { Text(allLabel) },
                    onClick = {
                        onSelected("")
                        expanded = false
                    }
                )
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun StatisticsGroup(title: String, counts: List<ActionDimensionCount>) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (counts.isEmpty()) {
                Text("暂无统计数据", style = MaterialTheme.typography.bodyMedium)
            } else {
                counts.forEach { entry ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(entry.name.ifBlank { "未分类" })
                        Text(entry.count.toString(), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotosScreen(
    vm: ToolboxViewModel,
    modifier: Modifier,
    initialAlbum: PhotoAlbum?,
    onCloseAlbum: () -> Unit,
    onOpenAlbum: (PhotoAlbum) -> Unit,
    onOpenPhoto: (PhotoAlbum, PhotoRecord) -> Unit
) {
    var albums by remember { mutableStateOf<List<PhotoAlbum>>(emptyList()) }
    var photos by remember { mutableStateOf<List<PhotoRecord>>(emptyList()) }
    var selectedAlbum by remember(initialAlbum?.id) { mutableStateOf(initialAlbum) }
    var showCreate by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try { albums = vm.photoAlbums() } catch (e: Exception) { error = e.message }
        initialAlbum?.let { album ->
            try { photos = vm.photoAlbumPhotos(album.id) } catch (e: Exception) { error = e.message }
        }
    }
    fun openAlbum(album: PhotoAlbum) {
        selectedAlbum = album
        onOpenAlbum(album)
        photos = emptyList()
        scope.launch {
            try { photos = vm.photoAlbumPhotos(album.id) } catch (e: Exception) { error = e.message }
        }
    }

    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (selectedAlbum == null) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("相册", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                OutlinedButton(onClick = { showCreate = true }) { Text("新建相册") }
            }
            if (error != null) Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
            if (albums.isEmpty()) {
                EmptyState("暂无相册。可新建相册并配置一个或多个图片目录。")
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(albums, key = { it.id }) { album ->
                        Card(onClick = { openAlbum(album) }, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Archive, contentDescription = null)
                                Text(album.name, fontWeight = FontWeight.SemiBold)
                                Text(album.sourceRoots.replace('\n', ','), maxLines = 2,
                                    style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        } else {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(selectedAlbum!!.name, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                OutlinedButton(onClick = {
                    scope.launch {
                        try {
                            vm.scanPhotoAlbum(selectedAlbum!!.id)
                            photos = vm.photoAlbumPhotos(selectedAlbum!!.id)
                        } catch (e: Exception) { error = e.message }
                    }
                }) { Text("扫描") }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = {
                    selectedAlbum = null
                    photos = emptyList()
                    onCloseAlbum()
                }) { Text("返回") }
            }
            if (photos.isEmpty()) {
                EmptyState("相册中暂无照片")
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(photos, key = { it.id }) { photo ->
                        Card(onClick = { onOpenPhoto(selectedAlbum!!, photo) },
                            modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
                            AsyncImage(
                                model = vm.photoThumbnailUrl(photo.id),
                                contentDescription = photo.fileName,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreate) {
        var name by remember { mutableStateOf("") }
        var roots by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text("新建照片相册") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("相册名称") })
                    OutlinedTextField(roots, { roots = it },
                        label = { Text("目录列表，每行一个目录") }, minLines = 3)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isBlank()) return@TextButton
                    scope.launch {
                        try {
                            val album = vm.savePhotoAlbum(PhotoAlbum(
                                name = name.trim(),
                                sourceRoots = roots.lineSequence().joinToString("\n") { it.trim() }
                            ))
                            vm.scanPhotoAlbum(album.id)
                            albums = vm.photoAlbums()
                            showCreate = false
                            openAlbum(album)
                        } catch (e: Exception) { error = e.message; showCreate = false }
                    }
                }) { Text("创建并扫描") }
            },
            dismissButton = { TextButton(onClick = { showCreate = false }) { Text("取消") } }
        )
    }
}

@Composable
private fun PhotoBackupScreen(
    vm: ToolboxViewModel,
    modifier: Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var albums by remember { mutableStateOf<List<PhotoAlbum>>(emptyList()) }
    var configs by remember { mutableStateOf(vm.photoBackupConfigs()) }
    var config by remember { mutableStateOf(configs.first()) }
    var logs by remember { mutableStateOf<List<String>>(emptyList()) }
    var status by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var albumMenu by remember { mutableStateOf(false) }
    var configMenu by remember { mutableStateOf(false) }

    fun appendLog(message: String) {
        val entry = "${java.time.LocalDateTime.now()}: $message"
        logs = (logs + entry).takeLast(200)
    }

    val sourcePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            try {
                context.contentResolver.takePersistableUriPermission(uri, flags)
            } catch (_: Exception) {}
            config = config.copy(sourceUri = uri.toString())
        }
    }

    LaunchedEffect(Unit) {
        try { albums = vm.photoAlbums() } catch (e: Exception) { error = e.message }
        if (config.albumId.isBlank()) config = config.copy(albumId = albums.firstOrNull()?.id.orEmpty())
    }

    fun saveConfig() {
        val updated = configs.filterNot { it.id == config.id } + config
        configs = updated
        vm.savePhotoBackupConfigs(updated)
    }

    fun isWifiConnected(): Boolean {
        val cm = context.getSystemService(android.net.ConnectivityManager::class.java)
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)
    }

    fun collectFiles(directory: DocumentFile, relativePrefix: String = ""): List<Pair<DocumentFile, String>> {
        val files = mutableListOf<Pair<DocumentFile, String>>()
        directory.listFiles().forEach { child ->
            val name = child.name ?: return@forEach
            val relativePath = if (relativePrefix.isBlank()) name else "$relativePrefix/$name"
            if (child.isDirectory) {
                files += collectFiles(child, relativePath)
            } else if (child.isFile && (
                name.substringAfterLast('.', "").lowercase()
                    in setOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "heic", "heif")
            )) {
                files += child to relativePath
            }
        }
        return files
    }

    Column(
        modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) {
                        OutlinedButton(onClick = { configMenu = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(config.name.ifBlank { "备份配置" }, maxLines = 1)
                        }
                        DropdownMenu(expanded = configMenu, onDismissRequest = { configMenu = false }) {
                            configs.forEach { item ->
                                DropdownMenuItem(
                                    text = { Text(item.name.ifBlank { "备份配置" }) },
                                    onClick = {
                                        saveConfig()
                                        config = item
                                        configMenu = false
                                    }
                                )
                            }
                        }
                    }
                    TextButton(onClick = {
                        saveConfig()
                        var nextIndex = configs.size + 1
                        while (configs.any { it.name == "备份配置 $nextIndex" }) nextIndex++
                        val next = PhotoBackupConfig(
                            name = "备份配置 $nextIndex",
                            albumId = albums.firstOrNull()?.id.orEmpty()
                        )
                        configs = configs + next
                        vm.savePhotoBackupConfigs(configs)
                        config = next
                    }) { Text("新增") }
                    IconButton(
                        enabled = configs.size > 1,
                        onClick = {
                            val updated = configs.filterNot { it.id == config.id }
                            configs = updated
                            vm.savePhotoBackupConfigs(updated)
                            config = updated.first()
                        }
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "删除备份配置")
                    }
                }
                OutlinedTextField(
                    value = config.name,
                    onValueChange = { config = config.copy(name = it) },
                    label = { Text("配置名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedButton(onClick = { sourcePicker.launch(null) }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (config.sourceUri.isBlank()) "选择照片目录" else "已选择：${config.sourceUri}")
                }
                Box {
                    OutlinedButton(onClick = { albumMenu = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(albums.firstOrNull { it.id == config.albumId }?.name ?: "选择目标相册")
                    }
                    DropdownMenu(expanded = albumMenu, onDismissRequest = { albumMenu = false }) {
                        albums.forEach { album ->
                            DropdownMenuItem(
                                text = { Text(album.name) },
                                onClick = { config = config.copy(albumId = album.id); albumMenu = false }
                            )
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("仅 Wi-Fi 备份", Modifier.weight(1f))
                    Switch(
                        checked = config.wifiOnly,
                        onCheckedChange = { config = config.copy(wifiOnly = it) }
                    )
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("启用自动备份", Modifier.weight(1f))
                    Switch(
                        checked = config.autoEnabled,
                        onCheckedChange = { config = config.copy(autoEnabled = it) }
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { saveConfig(); status = "配置已保存" }) { Text("保存配置") }
                    Button(
                        enabled = !busy,
                        onClick = {
                            val selectedConfig = config
                            saveConfig()
                            if (selectedConfig.sourceUri.isBlank() || selectedConfig.albumId.isBlank()) {
                                error = "请选择照片目录和目标相册"
                                return@Button
                            }
                            if (selectedConfig.wifiOnly && !isWifiConnected()) {
                                error = "当前不是 Wi-Fi 网络"
                                return@Button
                            }
                            busy = true
                            status = "正在扫描本地照片..."
                            scope.launch {
                                try {
                                    val uri = Uri.parse(selectedConfig.sourceUri)
                                    val directory = DocumentFile.fromTreeUri(context, uri)
                                        ?: throw IOException("无法读取照片目录")
                                    val files = collectFiles(directory)
                                        .filter { (file, relativePath) ->
                                            vm.photoNeedsIncrementalBackup(
                                                selectedConfig.sourceUri,
                                                selectedConfig.albumId,
                                                relativePath,
                                                file.length(),
                                                file.lastModified()
                                            )
                                        }
                                    if (files.isEmpty()) {
                                        status = "没有需要增量备份的照片"
                                        busy = false
                                        appendLog(status)
                                        return@launch
                                    }
                                    val job = vm.createPhotoBackup(
                                        selectedConfig.albumId,
                                        selectedConfig.sourceUri
                                    )
                                    appendLog("开始备份，任务 ${job.jobId}，共 ${files.size} 张")
                                    for ((index, item) in files.withIndex()) {
                                        val file = item.first
                                        val relativePath = item.second
                                        val fileUri = file.uri
                                        status = "正在备份 ${index + 1}/${files.size}"
                                        vm.uploadPhotoBackupItem(
                                            job.jobId,
                                            selectedConfig.albumId,
                                            fileUri,
                                            relativePath
                                        )
                                        vm.markPhotoIncrementalBackup(
                                            selectedConfig.sourceUri,
                                            selectedConfig.albumId,
                                            relativePath,
                                            file.length(),
                                            file.lastModified()
                                        )
                                    }
                                    val finished = vm.finishPhotoBackup(job.jobId)
                                    var finalJob = finished
                                    repeat(100) {
                                        if (finalJob.status.contains("COMPLETED")) return@repeat
                                        kotlinx.coroutines.delay(100)
                                        finalJob = vm.photoBackupJob(job.jobId)
                                    }
                                    status = if (finalJob.status.contains("COMPLETED")) "备份完成"
                                        else "备份结束：${finalJob.status}"
                                    appendLog(status)
                                } catch (e: Exception) {
                                    error = e.message
                                    appendLog("备份失败：${e.message}")
                                } finally {
                                    busy = false
                                }
                            }
                        }
                    ) { Text(if (busy) "备份中..." else "立即备份") }
                }
                if (status.isNotBlank()) Text(status, style = MaterialTheme.typography.bodySmall)
                if (error != null) Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("备份日志", fontWeight = FontWeight.SemiBold)
                if (logs.isEmpty()) {
                    Text("暂无日志", style = MaterialTheme.typography.bodySmall)
                } else {
                    logs.asReversed().take(50).forEach { log ->
                        Text(log, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoDetailScreen(
    vm: ToolboxViewModel,
    photo: PhotoRecord,
    albumId: String,
    modifier: Modifier,
    onPhotoChange: (PhotoRecord) -> Unit,
    onDeleted: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = androidx.compose.ui.platform.LocalDensity.current
    val swipeThreshold = with(density) { 64.dp.toPx() }
    var albumPhotos by remember(albumId) { mutableStateOf<List<PhotoRecord>>(emptyList()) }
    var controlsVisible by remember(photo.id) { mutableStateOf(false) }

    LaunchedEffect(albumId) {
        if (albumId.isNotBlank()) {
            albumPhotos = vm.photoAlbumPhotos(albumId)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(albumPhotos, photo.id) {
                var horizontalDrag = 0f
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        horizontalDrag += dragAmount
                    },
                    onDragEnd = {
                        val currentIndex = albumPhotos.indexOfFirst { it.id == photo.id }
                        if (currentIndex >= 0 && kotlin.math.abs(horizontalDrag) >= swipeThreshold) {
                            val nextIndex = if (horizontalDrag < 0) currentIndex + 1 else currentIndex - 1
                            albumPhotos.getOrNull(nextIndex)?.let(onPhotoChange)
                        }
                        horizontalDrag = 0f
                    },
                    onDragCancel = { horizontalDrag = 0f }
                )
            }
            .clickable { controlsVisible = !controlsVisible }
    ) {
        AsyncImage(
            model = vm.photoContentUrl(photo.id),
            contentDescription = photo.fileName,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
        if (controlsVisible) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    val share = Intent(Intent.ACTION_SEND).apply {
                        type = photo.mimeType ?: "image/*"
                        putExtra(Intent.EXTRA_TEXT, vm.photoContentUrl(photo.id))
                    }
                    context.startActivity(Intent.createChooser(share, "分享照片"))
                }) {
                    Icon(Icons.Default.Share, contentDescription = "分享",
                        tint = Color.White)
                }
                Spacer(Modifier.width(32.dp))
                IconButton(onClick = {
                    scope.launch {
                        try { vm.trashPhoto(photo.id); onDeleted() } catch (_: Exception) {}
                    }
                }) {
                    Icon(Icons.Default.Delete, contentDescription = "删除",
                        tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun TodoDetailsScreen(
    todo: TodoRecord,
    modifier: Modifier,
    busy: Boolean,
    isNew: Boolean = false,
    onSave: (TodoRecord) -> Unit
) {
    var name by remember(todo.id) { mutableStateOf(todo.name) }
    var remark by remember(todo.id) { mutableStateOf(todo.remark.orEmpty()) }
    var start by remember(todo.id) { mutableStateOf(todo.start.orEmpty()) }
    var status by remember(todo.id) { mutableStateOf(todo.state) }
    var statusMenuExpanded by remember(todo.id) { mutableStateOf(false) }
    val initialSchedule = remember(todo.id) { todoScheduleConfig(todo.refs) }
    var repeatCount by remember(todo.id) { mutableStateOf(initialSchedule.first) }
    var repeatUnit by remember(todo.id) { mutableStateOf(initialSchedule.second) }
    var repeatUnitMenuExpanded by remember(todo.id) { mutableStateOf(false) }
    val parsedRepeatCount = repeatCount.toIntOrNull()
    val repeatUnits = listOf("D" to "日", "W" to "周", "M" to "月", "Y" to "年")
    val todoStates = listOf("todo" to "待办", "doing" to "进行中", "done" to "已完成", "cancel" to "已取消")

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("标题") },
            singleLine = true,
            enabled = !busy
        )
        OutlinedTextField(
            value = remark,
            onValueChange = { remark = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("备注") },
            minLines = 3,
            enabled = !busy
        )
        if (!isNew) {
            Box {
                OutlinedButton(
                    onClick = { statusMenuExpanded = true },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("状态：${todoStates.firstOrNull { it.first == status }?.second ?: status}")
                }
                DropdownMenu(
                    expanded = statusMenuExpanded,
                    onDismissRequest = { statusMenuExpanded = false }
                ) {
                    todoStates.forEach { (value, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                status = value
                                statusMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = repeatCount,
                onValueChange = { repeatCount = it },
                modifier = Modifier.weight(1f),
                label = { Text("重复间隔") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                enabled = !busy
            )
            Box {
                OutlinedButton(
                    onClick = { repeatUnitMenuExpanded = true },
                    enabled = !busy
                ) {
                    Text(repeatUnits.first { it.first == repeatUnit }.second)
                }
                DropdownMenu(
                    expanded = repeatUnitMenuExpanded,
                    onDismissRequest = { repeatUnitMenuExpanded = false }
                ) {
                    repeatUnits.forEach { (unit, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                repeatUnit = unit
                                repeatUnitMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }
        RecordDateTimePicker(
            value = start,
            label = "开始日期",
            onValueChange = { start = it }
        )
        if (!isNew) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "创建时间：${todo.createTime}",
                    style = MaterialTheme.typography.bodySmall
                )
                todo.end?.takeIf(String::isNotBlank)?.let {
                    Text(
                        "完成时间：${todo.end}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
        Button(
            onClick = {
                val count = parsedRepeatCount ?: return@Button
                onSave(
                    todo.copy(
                        name = name.trim(),
                        state = status,
                        remark = remark.trim().ifBlank { null },
                        start = start.ifBlank { null }
                    ).withTodoSchedule(
                        count,
                        repeatUnit
                    )
                )
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !busy && name.isNotBlank() && parsedRepeatCount != null && parsedRepeatCount >= 0
        ) {
            Text(if (isNew) "添加" else "保存")
        }
    }
}

private fun todoScheduleConfig(refs: String?): Pair<String, String> {
    val schedule = refs.orEmpty()
        .split(";")
        .firstNotNullOfOrNull { it.trim().takeIf { ref -> ref.startsWith("schedule:") } }
        ?.substringAfter("schedule:")
    val match = Regex("^P(\\d+)([DWMY])$").matchEntire(schedule.orEmpty()) ?: return "0" to "D"
    return match.groupValues[1] to match.groupValues[2]
}

private fun TodoRecord.withTodoSchedule(count: Int, unit: String): TodoRecord {
    val otherRefs = refs.orEmpty()
        .split(";")
        .map(String::trim)
        .filter { it.isNotBlank() && it.substringBefore(':') != "schedule" }
    val schedule = if (count > 0) listOf("schedule:P$count$unit") else emptyList()
    return copy(
        category = if (count > 0) "repeat" else category?.takeUnless { it == "repeat" },
        refs = (otherRefs + schedule).joinToString(";").ifBlank { null }
    )
}

@Composable
private fun MoreToolCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(description, style = MaterialTheme.typography.bodySmall)
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
private fun RecordCreateScreen(
    vm: ToolboxViewModel,
    modifier: Modifier,
    sharedText: String?,
    onShareConsumed: () -> Unit,
    onCancel: () -> Unit,
    onCreated: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var copyQuery by remember { mutableStateOf("") }
    var copySource by remember { mutableStateOf<ActionRecord?>(null) }

    LaunchedEffect(sharedText) {
        if (!sharedText.isNullOrBlank()) {
            name = sharedText
            onShareConsumed()
        }
    }
    LaunchedEffect(copyQuery) {
        delay(250)
        vm.searchRecordCandidates(copyQuery)
    }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("任务名称") },
            maxLines = 4,
            enabled = !vm.busy
        )
        OutlinedTextField(
            value = copyQuery,
            onValueChange = {
                copyQuery = it
                copySource = null
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("复制已有行动（可选）") },
            placeholder = { Text("搜索要复制的行动") },
            singleLine = true,
            enabled = !vm.busy
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
                            name = candidate.name
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
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onCancel,
                enabled = !vm.busy,
                modifier = Modifier.weight(1f)
            ) {
                Text("取消")
            }
            Button(
                onClick = {
                    vm.createRecord(
                        name = name,
                        query = "",
                        page = 1,
                        copySource = copySource,
                        onCreated = onCreated
                    )
                },
                enabled = name.isNotBlank() && !vm.busy,
                modifier = Modifier.weight(1f)
            ) {
                Text("添加任务")
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
    onCreateRecord: () -> Unit,
    onFilterChanged: (String?, String?) -> Unit,
    onOpenRecord: (ActionRecord) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var appliedQuery by remember { mutableStateOf("") }
    var categoryFilter by remember(appliedCategory) { mutableStateOf(appliedCategory.orEmpty()) }
    var stateFilter by remember { mutableStateOf("") }
    var appliedState by remember { mutableStateOf("") }
    var startYearFilter by remember { mutableStateOf("") }
    var appliedStartYear by remember { mutableStateOf("") }
    var endYearFilter by remember { mutableStateOf("") }
    var appliedEndYear by remember { mutableStateOf("") }
    var page by remember { mutableStateOf(1) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val years = remember {
        (2019..java.time.Year.now().value).map { it.toString() }
    }

    LaunchedEffect(appliedQuery, appliedCategory, appliedLabel, appliedState, appliedStartYear, appliedEndYear, page) {
        vm.refreshRecords(
            appliedQuery,
            page,
            appliedCategory,
            appliedLabel,
            appliedState.takeIf(String::isNotBlank),
            appliedStartYear.toIntOrNull(),
            appliedEndYear.toIntOrNull()
        )
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
                    keyboardController?.hide()
                    appliedQuery = query.trim()
                    onFilterChanged(categoryFilter.takeIf(String::isNotBlank), null)
                    appliedState = stateFilter
                    appliedStartYear = startYearFilter
                    appliedEndYear = endYearFilter
                    page = 1
                },
                enabled = !vm.busy
            ) { Icon(Icons.Default.Search, contentDescription = "查询记录") }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            RecordFilterDropdown(
                title = "状态",
                selected = stateFilter,
                options = listOf(
                    "" to "全部",
                    "todo" to "待办",
                    "doing" to "进行中",
                    "done" to "已完成",
                    "cancel" to "已取消"
                ),
                modifier = Modifier.weight(1f),
                onSelected = { stateFilter = it }
            )
            RecordFilterDropdown(
                title = "分类",
                selected = categoryFilter,
                options = listOf("" to "全部") + vm.recordCategories.map { it to it },
                modifier = Modifier.weight(1f),
                onSelected = { categoryFilter = it }
            )
            RecordFilterDropdown(
                title = "开始",
                selected = startYearFilter,
                options = listOf("" to "全部") + years.map { it to it },
                modifier = Modifier.weight(1f),
                onSelected = { startYearFilter = it }
            )
            RecordFilterDropdown(
                title = "结束",
                selected = endYearFilter,
                options = listOf("" to "全部") + years.map { it to it },
                modifier = Modifier.weight(1f),
                onSelected = { endYearFilter = it }
            )
        }

        if (vm.records.isEmpty()) {
            EmptyState(
                if (
                    appliedQuery.isNotBlank() || appliedCategory != null || appliedLabel != null ||
                    appliedState.isNotBlank() || appliedStartYear.isNotBlank() || appliedEndYear.isNotBlank()
                ) {
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                record.name,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                when (record.state) {
                                    "todo" -> "待办"
                                    "doing" -> "进行中"
                                    "done" -> "已完成"
                                    "cancel" -> "已取消"
                                    else -> record.state
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (!record.remark.isNullOrBlank()) {
                            Text(record.remark, style = MaterialTheme.typography.bodySmall, maxLines = 1)
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
                            record.labels.orEmpty().split(",").map(String::trim)
                                .filter(String::isNotBlank)
                                .take(3)
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
private fun RecordFilterDropdown(
    title: String,
    selected: String,
    options: List<Pair<String, String>>,
    modifier: Modifier = Modifier,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.first == selected }?.second ?: options.first().second
    Box(modifier) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text("$title\n$selectedLabel", maxLines = 2)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (value, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onSelected(value)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun HabitBoardScreen(
    vm: ToolboxViewModel,
    modifier: Modifier,
    showArchived: Boolean,
    onOpenHabit: (ActionRecord) -> Unit
) {
    var appliedQuery by remember { mutableStateOf("") }
    val today = LocalDate.now()
    val recentDays = (0..4).map { today.minusDays(it.toLong()) }
    val visibleHabits = vm.habits.filter {
        it.state == (if (showArchived) "cancel" else "doing") &&
            (appliedQuery.isBlank() || it.name.contains(appliedQuery, ignoreCase = true))
    }

    LaunchedEffect(Unit) {
        vm.refreshHabits()
    }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (visibleHabits.isEmpty()) {
            EmptyState(
                when {
                    appliedQuery.isNotBlank() -> if (showArchived) "没有匹配的归档习惯" else "没有匹配的习惯"
                    showArchived -> "还没有归档习惯"
                    else -> "还没有习惯，点击右上角添加"
                }
            )
        } else {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(Modifier.weight(1f))
                recentDays.forEach { date ->
                    Column(
                        Modifier.width(50.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            date.dayOfWeek.getDisplayName(
                                java.time.format.TextStyle.SHORT,
                                java.util.Locale.ENGLISH
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            date.dayOfMonth.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            visibleHabits.forEach { habit ->
                val export = vm.habitExports[habit.id]
                val history = remember(export?.checklists) {
                    export?.checklists.orEmpty()
                        .mapNotNull { item ->
                            item.createTime?.take(10)?.let { date ->
                                runCatching { java.time.LocalDate.parse(date) }.getOrNull()
                                    ?.let { it to item }
                            }
                        }
                        .groupBy({ it.first }, { it.second })
                        .mapValues { (_, entries) -> entries.maxBy { it.createTime.orEmpty() } }
                }
                val completedDates = remember(export?.checklists) {
                    export?.checklists.orEmpty()
                        .asSequence()
                        .filter { it.current >= 1 }
                        .mapNotNull { entry ->
                            entry.createTime?.take(10)?.let {
                                runCatching {
                                    LocalDate.parse(it)
                                }.getOrNull()
                            }
                        }
                        .toSortedSet()
                }
                Card(
                    onClick = { onOpenHabit(habit) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !vm.busy
                ) {
                    Row(
                        Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                habit.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (!habit.remark.isNullOrBlank()) {
                                Text(habit.remark, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                            }
                        }
                        recentDays.forEach { date ->
                            val entry = history[date]
                            val isValue = export?.habit?.category == "value" || habit.category == "value"
                            val achieved = if (isValue) {
                                entry != null && isHabitValueAchieved(
                                    entry.current,
                                    export?.habit?.targetValue,
                                    export?.habit?.targetType
                                )
                            } else {
                                entry?.current == 1
                            }
                            val required = isHabitRequiredOnDate(
                                LocalDate.of(1970, 1, 1),
                                export?.habit?.freqNum,
                                export?.habit?.freqDen,
                                date,
                                completedDates
                            )
                            val statusColor = when {
                                achieved -> Color(0xFF1976D2)
                                entry == null && !required -> Color(0xFF64B5F6)
                                else -> Color(0xFF8A8D93)
                            }
                            Column(
                                Modifier.width(50.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    if (isValue) entry?.let { formatHabitListValue(it.current.toLong()) } ?: "–"
                                    else when {
                                        entry?.current == 1 -> "√"
                                        entry == null && !required -> "–"
                                        else -> "×"
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    color = statusColor
                                )
                            }
                        }
                    }
                }
            }
        }

        Text("共 ${visibleHabits.size} 个习惯", style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun BookRecordScreen(
    vm: ToolboxViewModel,
    modifier: Modifier
) {
    var query by remember { mutableStateOf("") }
    var appliedQuery by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var categoryMenuOpen by remember { mutableStateOf(false) }
    var page by remember { mutableStateOf(1) }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(appliedQuery, category, page) {
        vm.refreshBooks(appliedQuery, category, page)
    }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.weight(1f),
                label = { Text("按书名搜索") },
                placeholder = { Text("输入书名") },
                singleLine = true
            )
            IconButton(
                onClick = {
                    keyboardController?.hide()
                    appliedQuery = query.trim()
                    page = 1
                },
                enabled = !vm.busy
            ) {
                Icon(Icons.Default.Search, contentDescription = "查询书籍")
            }
        }

        Box {
            OutlinedButton(onClick = { categoryMenuOpen = true }, enabled = !vm.busy) {
                Text(when (category) {
                    "ebook" -> "类别：电子书"
                    "comic" -> "类别：漫画"
                    else -> "全部类别"
                })
            }
            DropdownMenu(
                expanded = categoryMenuOpen,
                onDismissRequest = { categoryMenuOpen = false }
            ) {
                listOf("" to "全部类别", "ebook" to "电子书", "comic" to "漫画").forEach { (value, label) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            category = value
                            categoryMenuOpen = false
                            page = 1
                        }
                    )
                }
            }
        }

        if (vm.books.isEmpty()) {
            EmptyState(
                if (appliedQuery.isNotBlank() || category.isNotBlank()) "没有匹配的书籍记录"
                else "还没有书籍记录"
            )
        } else {
            vm.books.forEach { book ->
                Card(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                book.name,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                if (book.category == "comic") "漫画" else "电子书",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (!book.remark.isNullOrBlank()) {
                            Text(book.remark, style = MaterialTheme.typography.bodySmall)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                book.labels?.takeIf(String::isNotBlank)?.let { "$it" } ?: "无后缀",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
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
            Text("第 $page 页 · ${vm.books.size} 条")
            OutlinedButton(onClick = { page++ }, enabled = vm.bookHasNextPage && !vm.busy) {
                Text("下一页")
            }
        }
    }
}

@Composable
private fun HabitCreateScreen(
    vm: ToolboxViewModel,
    modifier: Modifier,
    onCancel: () -> Unit,
    onCreated: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("check") }
    var note by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("") }
    var freqNum by remember { mutableStateOf("1") }
    var freqDen by remember { mutableStateOf("1") }
    var question by remember { mutableStateOf("") }
    var targetValue by remember { mutableStateOf("") }
    var targetType by remember { mutableStateOf("AtLeast") }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("习惯名称") },
                    singleLine = true,
                    enabled = !vm.busy
                )
                Text("习惯类别", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (category == "check") {
                        Button(onClick = { category = "check" }, enabled = !vm.busy) { Text("完成打卡") }
                    } else {
                        OutlinedButton(onClick = { category = "check" }, enabled = !vm.busy) { Text("完成打卡") }
                    }
                    if (category == "value") {
                        Button(onClick = { category = "value" }, enabled = !vm.busy) { Text("记录数值") }
                    } else {
                        OutlinedButton(onClick = { category = "value" }, enabled = !vm.busy) { Text("记录数值") }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = freqNum,
                        onValueChange = { freqNum = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("频率次数") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        enabled = !vm.busy
                    )
                    OutlinedTextField(
                        value = freqDen,
                        onValueChange = { freqDen = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("每多少天") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        enabled = !vm.busy
                    )
                }
                if (category == "check") {
                    OutlinedTextField(
                        value = question,
                        onValueChange = { question = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("打卡问题（可选）") },
                        enabled = !vm.busy
                    )
                } else {
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("单位（可选）") },
                        enabled = !vm.busy
                    )
                    OutlinedTextField(
                        value = targetValue,
                        onValueChange = { targetValue = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("目标值") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        enabled = !vm.busy
                    )
                    Text("目标类型", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (targetType == "AtLeast") {
                            Button(onClick = { targetType = "AtLeast" }, enabled = !vm.busy) { Text("至少达到") }
                        } else {
                            OutlinedButton(onClick = { targetType = "AtLeast" }, enabled = !vm.busy) { Text("至少达到") }
                        }
                        if (targetType == "AtMost") {
                            Button(onClick = { targetType = "AtMost" }, enabled = !vm.busy) { Text("最多不超过") }
                        } else {
                            OutlinedButton(onClick = { targetType = "AtMost" }, enabled = !vm.busy) { Text("最多不超过") }
                        }
                    }
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("备注（可选）") },
                    minLines = 2,
                    enabled = !vm.busy
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                enabled = !vm.busy
            ) {
                Text("取消")
            }
            Button(
                onClick = {
                    val numerator = freqNum.toIntOrNull()
                    val denominator = freqDen.toIntOrNull()
                    val target = targetValue.toIntOrNull()
                    if (name.isBlank()) {
                        vm.showMessage("习惯名称不能为空")
                    } else if (numerator == null || denominator == null || numerator < 1 || denominator < 1) {
                        vm.showMessage("频率次数和天数必须是正整数")
                    } else if (category == "value" && target == null) {
                        vm.showMessage("数值习惯的目标值必须是整数")
                    } else {
                        vm.createHabit(
                            name = name,
                            category = category,
                            note = note,
                            freqNum = numerator,
                            freqDen = denominator,
                            unit = unit,
                            question = question,
                            targetValue = target,
                            targetType = if (category == "value") targetType else null,
                            onCreated = onCreated
                        )
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = name.isNotBlank() && !vm.busy
            ) {
                Text("创建")
            }
        }
    }
}

@Composable
private fun HabitDetailsScreen(
    modifier: Modifier,
    habit: net.cofcool.sourcebox.data.HabitRecord?,
    busy: Boolean,
    onSaveHabit: (net.cofcool.sourcebox.data.HabitRecord) -> Unit,
    onSaveValue: (Int) -> Unit
) {
    var value by remember(habit?.action?.id) { mutableStateOf("") }
    var editing by remember(habit?.action?.id) { mutableStateOf(false) }
    var name by remember(habit?.action?.id) { mutableStateOf(habit?.action?.name.orEmpty()) }
    var category by remember(habit?.action?.id) { mutableStateOf(habit?.habit?.category ?: "check") }
    var note by remember(habit?.action?.id) { mutableStateOf(habit?.habit?.note.orEmpty()) }
    var unit by remember(habit?.action?.id) { mutableStateOf(habit?.habit?.unit.orEmpty()) }
    var freqNum by remember(habit?.action?.id) { mutableStateOf(habit?.habit?.freqNum?.toString() ?: "1") }
    var freqDen by remember(habit?.action?.id) { mutableStateOf(habit?.habit?.freqDen?.toString() ?: "1") }
    var question by remember(habit?.action?.id) { mutableStateOf(habit?.habit?.question.orEmpty()) }
    var targetValue by remember(habit?.action?.id) { mutableStateOf(habit?.habit?.targetValue?.toString().orEmpty()) }
    var targetType by remember(habit?.action?.id) { mutableStateOf(habit?.habit?.targetType ?: "AtLeast") }
    val definition = habit?.habit
    val validFrequency = (freqNum.toIntOrNull() ?: 0) > 0 && (freqDen.toIntOrNull() ?: 0) > 0
    val validTarget = category != "value" || targetValue.toIntOrNull() != null

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (habit == null) {
            if (busy) {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                EmptyState("无法加载习惯详情，请返回后重试")
            }
            return@Column
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(habit.action.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(if (definition?.category == "value") "数值习惯" else "打卡习惯")
                Text("频率：${definition?.freqNum ?: 1} / ${definition?.freqDen ?: 1} 天")
                definition?.note?.takeIf(String::isNotBlank)?.let { Text("备注：$it") }
                if (definition?.category == "value") {
                    Text(
                        "目标：${if (definition.targetType == "AtMost") "最多" else "至少"} " +
                            "${definition.targetValue ?: "未设置"} ${definition.unit.orEmpty()}"
                    )
                }
                definition?.question?.takeIf(String::isNotBlank)?.let { Text("打卡问题：$it") }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { editing = !editing },
                        enabled = !busy,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (editing) "收起" else "修改")
                    }
                    OutlinedButton(
                        onClick = {
                            onSaveHabit(
                                habit.copy(
                                    action = habit.action.copy(
                                        state = if (habit.action.state == "cancel") "doing" else "cancel"
                                    )
                                )
                            )
                        },
                        enabled = !busy,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (habit.action.state == "cancel") "激活" else "归档")
                    }
                }
            }
        }

        if (editing) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        name,
                        { name = it },
                        Modifier.fillMaxWidth(),
                        label = { Text("习惯名称") },
                        singleLine = true,
                        enabled = !busy
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (category == "check") {
                            Button(onClick = { category = "check" }, enabled = !busy) { Text("完成打卡") }
                        } else {
                            OutlinedButton(onClick = { category = "check" }, enabled = !busy) { Text("完成打卡") }
                        }
                        if (category == "value") {
                            Button(onClick = { category = "value" }, enabled = !busy) { Text("记录数值") }
                        } else {
                            OutlinedButton(onClick = { category = "value" }, enabled = !busy) { Text("记录数值") }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            freqNum,
                            { freqNum = it },
                            Modifier.weight(1f),
                            label = { Text("频率次数") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            enabled = !busy
                        )
                        OutlinedTextField(
                            freqDen,
                            { freqDen = it },
                            Modifier.weight(1f),
                            label = { Text("每多少天") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            enabled = !busy
                        )
                    }
                    if (category == "check") {
                        OutlinedTextField(
                            question,
                            { question = it },
                            Modifier.fillMaxWidth(),
                            label = { Text("打卡问题（可选）") },
                            enabled = !busy
                        )
                    } else {
                        OutlinedTextField(
                            unit,
                            { unit = it },
                            Modifier.fillMaxWidth(),
                            label = { Text("单位（可选）") },
                            enabled = !busy
                        )
                        OutlinedTextField(
                            targetValue,
                            { targetValue = it },
                            Modifier.fillMaxWidth(),
                            label = { Text("目标值") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            enabled = !busy
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (targetType == "AtLeast") {
                                Button(onClick = { targetType = "AtLeast" }, enabled = !busy) { Text("至少达到") }
                            } else {
                                OutlinedButton(onClick = { targetType = "AtLeast" }, enabled = !busy) { Text("至少达到") }
                            }
                            if (targetType == "AtMost") {
                                Button(onClick = { targetType = "AtMost" }, enabled = !busy) { Text("最多不超过") }
                            } else {
                                OutlinedButton(onClick = { targetType = "AtMost" }, enabled = !busy) { Text("最多不超过") }
                            }
                        }
                    }
                    OutlinedTextField(
                        note,
                        { note = it },
                        Modifier.fillMaxWidth(),
                        label = { Text("备注（可选）") },
                        minLines = 2,
                        enabled = !busy
                    )
                    Button(
                        onClick = {
                            val numerator = freqNum.toIntOrNull()
                            val denominator = freqDen.toIntOrNull()
                            val target = targetValue.toIntOrNull()
                            if (name.isBlank()) return@Button
                            if (numerator == null || numerator < 1 || denominator == null || denominator < 1) return@Button
                            if (category == "value" && target == null) return@Button
                            val currentHabit = habit
                            onSaveHabit(
                                currentHabit.copy(
                                    action = currentHabit.action.copy(name = name.trim(), category = category, remark = note.trim().ifBlank { null }),
                                    habit = currentHabit.habit.copy(
                                        category = category,
                                        note = note.trim().ifBlank { null },
                                        unit = if (category == "value") unit.trim().ifBlank { null } else null,
                                        freqNum = numerator,
                                        freqDen = denominator,
                                        question = if (category == "check") question.trim().ifBlank { null } else null,
                                        targetValue = if (category == "value") target else null,
                                        targetType = if (category == "value") targetType else null
                                    )
                                )
                            )
                            editing = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !busy && name.isNotBlank() && validFrequency && validTarget
                    ) {
                        Text("保存")
                    }
                }
            }
        }

        val today = java.time.LocalDate.now().toString()
        val todayEntry = habit.checkLists.firstOrNull { it.createTime?.take(10) == today }
        Text(
            if (todayEntry == null) "记录今日习惯" else "更新今日记录",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        if (definition?.category == "value") {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("记录数值${definition.unit?.let { "（$it）" }.orEmpty()}") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                enabled = !busy
            )
            val storedValue = parseHabitDisplayValue(value)
            Button(
                onClick = { storedValue?.let(onSaveValue) },
                modifier = Modifier.fillMaxWidth(),
                enabled = storedValue != null && !busy
            ) {
                Text("保存记录")
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onSaveValue(1) },
                    modifier = Modifier.weight(1f),
                    enabled = !busy
                ) {
                    Text("已完成")
                }
                OutlinedButton(
                    onClick = { onSaveValue(0) },
                    modifier = Modifier.weight(1f),
                    enabled = !busy
                ) {
                    Text("未完成")
                }
            }
        }

        HabitCalendar(
            checkLists = habit.checkLists,
            category = definition?.category ?: "check",
            targetValue = definition?.targetValue,
            targetType = definition?.targetType,
            unit = definition?.unit,
            start = habit.action.start,
            actionCreateTime = habit.action.createTime,
            habitCreateTime = definition?.createTime,
            freqNum = definition?.freqNum,
            freqDen = definition?.freqDen
        )
    }
}

@Composable
private fun HabitCalendar(
    checkLists: List<ActionCheckList>,
    category: String,
    targetValue: Int?,
    targetType: String?,
    unit: String?,
    start: String?,
    actionCreateTime: String?,
    habitCreateTime: String?,
    freqNum: Int?,
    freqDen: Int?
) {
    val today = remember { java.time.LocalDate.now() }
    var selectedDate by remember { mutableStateOf(today) }
    var displayedMonth by remember { mutableStateOf(java.time.YearMonth.from(today)) }
    var statsPeriod by remember { mutableStateOf("month") }

    val entriesByDate = remember(checkLists) {
        checkLists.mapNotNull { entry ->
            entry.createTime?.take(10)?.let { date ->
                runCatching { java.time.LocalDate.parse(date) }.getOrNull()?.let { it to entry }
            }
        }.groupBy({ it.first }, { it.second })
            .mapValues { (_, entries) -> entries.maxBy { it.createTime.orEmpty() } }
    }
    val completedDates = remember(checkLists) {
        checkLists.asSequence()
            .filter { it.current >= 1 }
            .mapNotNull { entry ->
                entry.createTime?.take(10)?.let {
                    runCatching {
                        LocalDate.parse(it)
                    }.getOrNull()
                }
            }
            .toSortedSet()
    }
    val habitStartDate = remember(
        start,
        actionCreateTime,
        habitCreateTime
    ) {
        listOf(start, actionCreateTime, habitCreateTime)
            .firstNotNullOfOrNull { value ->
                value?.take(10)?.let {
                    runCatching { LocalDate.parse(it) }.getOrNull()
                }
            }
    }

    val firstDayOffset = displayedMonth.atDay(1).dayOfWeek.value - 1
    val calendarDays = buildList<java.time.LocalDate?> {
        repeat(firstDayOffset) { add(null) }
        for (day in 1..displayedMonth.lengthOfMonth()) {
            add(displayedMonth.atDay(day))
        }
        while (size % 7 != 0) add(null)
    }
    val selectedEntry = entriesByDate[selectedDate]
    val periodStart: java.time.LocalDate
    val periodEnd: java.time.LocalDate
    when (statsPeriod) {
        "week" -> {
            periodStart = selectedDate.minusDays((selectedDate.dayOfWeek.value - 1).toLong())
            periodEnd = periodStart.plusDays(6)
        }
        "year" -> {
            periodStart = selectedDate.withDayOfYear(1)
            periodEnd = selectedDate.withDayOfYear(selectedDate.lengthOfYear())
        }
        else -> {
            periodStart = selectedDate.withDayOfMonth(1)
            periodEnd = selectedDate.withDayOfMonth(selectedDate.lengthOfMonth())
        }
    }
    val periodEntries = entriesByDate.filterKeys { !it.isBefore(periodStart) && !it.isAfter(periodEnd) }.values
    val isValueHabit = category == "value"
    val statistic = if (isValueHabit) {
        periodEntries.sumOf { it.current.toLong() }
    } else {
        periodEntries.count { it.current == 1 }.toLong()
    }
    val chartValues = if (statsPeriod == "year") {
        (0L..11L).map { monthOffset ->
            val month = java.time.YearMonth.from(periodStart).plusMonths(monthOffset)
            val monthEntries = entriesByDate.filterKeys { java.time.YearMonth.from(it) == month }.values
            if (isValueHabit) monthEntries.sumOf { it.current }.toFloat()
            else monthEntries.count { it.current == 1 }.toFloat()
        }
    } else {
        val days = java.time.temporal.ChronoUnit.DAYS.between(periodStart, periodEnd).toInt() + 1
        (0 until days).map { offset ->
            val entry = entriesByDate[periodStart.plusDays(offset.toLong())]
            if (isValueHabit) entry?.current?.toFloat() ?: 0f
            else if (entry?.current == 1) 1f else 0f
        }
    }
    val chartLabelIndices = if (chartValues.size <= 3) {
        chartValues.indices.toList()
    } else {
        listOf(0, chartValues.size / 2, chartValues.lastIndex)
    }.distinct()
    val chartLabels = chartLabelIndices.map { index ->
        if (statsPeriod == "year") "${index + 1}月"
        else periodStart.plusDays(index.toLong()).dayOfMonth.toString()
    }
    val selectedChartIndex = if (statsPeriod == "year") {
        selectedDate.monthValue - 1
    } else {
        java.time.temporal.ChronoUnit.DAYS.between(periodStart, selectedDate).toInt()
    }.coerceIn(chartValues.indices)
    val chartLineColor = MaterialTheme.colorScheme.primary
    val chartGridColor = MaterialTheme.colorScheme.outlineVariant

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("记录日历", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = {
                        displayedMonth = displayedMonth.minusMonths(1)
                        selectedDate = displayedMonth.atDay(1)
                    }) { Text("‹") }
                    Text(
                        "${displayedMonth.year}年${displayedMonth.monthValue}月",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(onClick = {
                        displayedMonth = displayedMonth.plusMonths(1)
                        selectedDate = displayedMonth.atDay(1)
                    }) { Text("›") }
                }
            }

            Row(Modifier.fillMaxWidth()) {
                listOf("一", "二", "三", "四", "五", "六", "日").forEach { weekday ->
                    Text(
                        weekday,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
            calendarDays.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth()) {
                    week.forEach { date ->
                        if (date == null) {
                            Spacer(Modifier.weight(1f).height(48.dp).padding(2.dp))
                        } else {
                            val entry = entriesByDate[date]
                            val achieved = entry != null && if (isValueHabit) {
                                isHabitValueAchieved(entry.current, targetValue, targetType)
                            } else entry.current == 1
                            val required = isHabitRequiredOnDate(
                                habitStartDate,
                                freqNum,
                                freqDen,
                                date,
                                completedDates
                            )
                            val shape = RoundedCornerShape(8.dp)
                            val background = when {
                                achieved -> Color(0xFF1976D2)
                                entry == null && !required -> Color(0xFFBBDEFB)
                                else -> Color(0xFFE7E9ED)
                            }
                            val foreground = if (achieved) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            Column(
                                Modifier.weight(1f)
                                    .padding(2.dp)
                                    .heightIn(min = 48.dp)
                                    .background(background, shape)
                                    .then(
                                        if (date == selectedDate) Modifier.border(
                                            2.dp,
                                            MaterialTheme.colorScheme.primary,
                                            shape
                                        ) else Modifier
                                    )
                                    .clickable {
                                        selectedDate = date
                                        displayedMonth = java.time.YearMonth.from(date)
                                    }
                                    .padding(vertical = 5.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    date.dayOfMonth.toString(),
                                    color = foreground,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (date == today) FontWeight.Bold else FontWeight.Normal
                                )
                                if (entry != null && isValueHabit) {
                                    Text(
                                        formatHabitStoredValue(entry.current.toLong()),
                                        color = foreground,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "统计",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                listOf("week" to "周", "month" to "月", "year" to "年").forEach { (period, label) ->
                    if (statsPeriod == period) {
                        Button(
                            onClick = { statsPeriod = period },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) { Text(label) }
                    } else {
                        OutlinedButton(
                            onClick = { statsPeriod = period },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) { Text(label) }
                    }
                }
            }
            Text(
                if (isValueHabit) {
                    "${periodStart} 至 ${periodEnd}：数值之和 ${formatHabitStoredValue(statistic)}${unit?.let { " $it" }.orEmpty()}"
                } else {
                    "${periodStart} 至 ${periodEnd}：达成 $statistic 次"
                },
                style = MaterialTheme.typography.bodyMedium
            )
            Canvas(
                modifier = Modifier.fillMaxWidth().height(150.dp)
            ) {
                if (chartValues.isEmpty()) return@Canvas
                val left = 8.dp.toPx()
                val right = size.width - 8.dp.toPx()
                val top = 8.dp.toPx()
                val bottom = size.height - 8.dp.toPx()
                val maxValue = chartValues.maxOrNull()?.coerceAtLeast(1f) ?: 1f
                val minValue = chartValues.minOrNull()?.coerceAtMost(0f) ?: 0f
                val valueRange = (maxValue - minValue).coerceAtLeast(1f)
                repeat(3) { line ->
                    val y = top + (bottom - top) * line / 2f
                    drawLine(
                        color = chartGridColor,
                        start = androidx.compose.ui.geometry.Offset(left, y),
                        end = androidx.compose.ui.geometry.Offset(right, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                val points = chartValues.mapIndexed { index, value ->
                    val x = if (chartValues.size == 1) (left + right) / 2f
                    else left + (right - left) * index / chartValues.lastIndex
                    val y = bottom - (value - minValue) / valueRange * (bottom - top)
                    androidx.compose.ui.geometry.Offset(x, y)
                }
                val path = Path().apply {
                    points.forEachIndexed { index, point ->
                        if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
                    }
                }
                drawPath(path, chartLineColor, style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
                val valuePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    color = chartLineColor.toArgb()
                    textSize = 10.sp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                }
                points.forEachIndexed { index, point ->
                    drawCircle(chartLineColor, radius = 3.dp.toPx(), center = point)
                    if (isValueHabit && (statsPeriod == "year" || index == selectedChartIndex)) {
                        val value = formatHabitStoredValue(chartValues[index].toLong()) +
                            unit?.let { " $it" }.orEmpty()
                        val labelY = if (point.y < top + 16.dp.toPx()) {
                            point.y + 16.dp.toPx()
                        } else {
                            point.y - 8.dp.toPx()
                        }
                        drawContext.canvas.nativeCanvas.drawText(value, point.x, labelY, valuePaint)
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                chartLabels.forEach { label ->
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                "${selectedDate}：${selectedEntry?.let { entry ->
                    if (isValueHabit) "${formatHabitStoredValue(entry.current.toLong())}${unit?.let { " $it" }.orEmpty()}"
                    else if (entry.current == 1) "已完成" else "未完成"
                } ?: "无记录"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.size(12.dp).background(Color(0xFF1976D2), RoundedCornerShape(3.dp)))
                    Text(if (isValueHabit) "达标" else "已完成", style = MaterialTheme.typography.labelSmall)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.size(12.dp).background(Color(0xFFE7E9ED), RoundedCornerShape(3.dp)))
                    Text("未达标", style = MaterialTheme.typography.labelSmall)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.size(12.dp).background(Color(0xFFBBDEFB), RoundedCornerShape(3.dp)))
                    Text("无需打卡", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

private fun isHabitValueAchieved(current: Int, targetValue: Int?, targetType: String?): Boolean {
    val target = targetValue ?: return false
    return if (targetType == "AtMost") {
        current.toLong() <= target.toLong() * 1000
    } else {
        current.toLong() >= target.toLong() * 1000
    }
}

private fun parseHabitDisplayValue(value: String): Int? =
    runCatching {
        java.math.BigDecimal(value.trim())
            .multiply(java.math.BigDecimal("1000"))
            .intValueExact()
    }.getOrNull()

private fun formatHabitStoredValue(value: Long): String =
    java.math.BigDecimal.valueOf(value, 3)
        .stripTrailingZeros()
        .toPlainString()

private fun formatHabitListValue(value: Long): String {
    val displayValue = java.math.BigDecimal.valueOf(value, 3)
    if (displayValue <= java.math.BigDecimal("1000")) {
        return displayValue.stripTrailingZeros().toPlainString()
    }
    return displayValue.divide(java.math.BigDecimal("1000"))
        .setScale(1, java.math.RoundingMode.HALF_UP)
        .stripTrailingZeros()
        .toPlainString() + "k"
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
    var selectedCommentTab by remember(record.id) { mutableStateOf(0) }
    var durationHours by remember(record.id) {
        mutableStateOf(
            record.duration?.let {
                java.math.BigDecimal.valueOf(it.toLong())
                    .divide(java.math.BigDecimal.valueOf(3600), 6, java.math.RoundingMode.HALF_UP)
                    .stripTrailingZeros()
                    .toPlainString()
            }.orEmpty()
        )
    }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                record.name,
                { onRecordChange(record.copy(name = it)) },
                Modifier.fillMaxWidth(),
                label = { Text("任务名称") },
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
                    val updated = (current + selected).distinct().joinToString(",")
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
            RecordDateTimePicker(
                value = record.start.orEmpty(),
                label = "开始时间",
                onValueChange = { onRecordChange(record.copy(start = it)) }
            )
            RecordDateTimePicker(
                value = record.end.orEmpty(),
                label = "结束时间（可选）",
                onValueChange = { onRecordChange(record.copy(end = it)) }
            )
            OutlinedTextField(
                durationHours,
                { value ->
                    durationHours = value
                    val seconds = value.toBigDecimalOrNull()
                        ?.takeIf { it >= java.math.BigDecimal.ZERO }
                        ?.multiply(java.math.BigDecimal.valueOf(3600))
                        ?.setScale(0, java.math.RoundingMode.HALF_UP)
                        ?.takeIf { it <= java.math.BigDecimal.valueOf(Int.MAX_VALUE.toLong()) }
                    onRecordChange(
                        record.copy(duration = seconds?.toInt())
                    )
                },
                Modifier.fillMaxWidth(),
                label = { Text("持续时长（小时）") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
                ChecklistItem(
                    checklist,
                    busy,
                    onSaveChecklist,
                    onDeleteChecklist,
                    onLongClick = {
                        val reference = "@cl:${checklist.id} "
                        reply += if (reply.isBlank() || reply.last().isWhitespace()) {
                            reference
                        } else {
                            " $reference"
                        }
                    }
                )
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
            val replyComments = comments.filterNot { it.content.startsWith("sys-") }
            val systemComments = comments.filter { it.content.startsWith("sys-") }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            ) {
                listOf("回复", "系统").forEachIndexed { index, title ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedCommentTab = index }
                            .background(
                                if (selectedCommentTab == index) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    Color.Transparent
                                }
                            )
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            title,
                            color = if (selectedCommentTab == index) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
            }
            val visibleComments = if (selectedCommentTab == 0) replyComments else systemComments
            if (visibleComments.isEmpty()) {
                Text(
                    if (selectedCommentTab == 0) "还没有回复" else "还没有系统消息",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            visibleComments.forEach { comment ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val content = buildAnnotatedString {
                        var position = 0
                        Regex("@cl:([^\\s]+)").findAll(comment.content).forEach { match ->
                            append(comment.content.substring(position, match.range.first))
                            val checklistId = match.groupValues[1]
                            val checklistContent = checklists.firstOrNull { it.id == checklistId }?.content
                            if (checklistContent != null) {
                                withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                                    append(checklistContent)
                                }
                            } else {
                                append(match.value)
                            }
                            position = match.range.last + 1
                        }
                        append(comment.content.substring(position))
                    }
                    Text(content)
                    Text(comment.createTime.orEmpty(), style = MaterialTheme.typography.labelSmall)
                    androidx.compose.material3.HorizontalDivider()
                }
            }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecordDateTimePicker(
    value: String,
    label: String,
    onValueChange: (String) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    val dateTime = remember(value) {
        runCatching {
            java.time.LocalDateTime.parse(
                value,
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            )
        }.getOrElse {
            runCatching { java.time.LocalDateTime.parse(value) }
                .getOrElse { java.time.LocalDateTime.now() }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(
            onClick = {
                selectedDateMillis = dateTime.toLocalDate()
                    .atStartOfDay(java.time.ZoneOffset.UTC)
                    .toInstant()
                    .toEpochMilli()
                showDatePicker = true
            },
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(label, style = MaterialTheme.typography.labelSmall)
                Text(value.ifBlank { "点击选择日期和时间" })
            }
        }
        if (value.isNotBlank()) {
            TextButton(onClick = { onValueChange("") }) {
                Text("清除")
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDateMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedDateMillis = datePickerState.selectedDateMillis
                            ?: selectedDateMillis
                        showDatePicker = false
                        showTimePicker = true
                    }
                ) {
                    Text("下一步")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("取消")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = dateTime.hour,
            initialMinute = dateTime.minute,
            is24Hour = true
        )
        Dialog(onDismissRequest = { showTimePicker = false }) {
            androidx.compose.material3.Surface(
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("选择时间", style = MaterialTheme.typography.titleLarge)
                    TimePicker(state = timePickerState)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showTimePicker = false }) {
                            Text("取消")
                        }
                        TextButton(
                            onClick = {
                                val date = java.time.Instant.ofEpochMilli(selectedDateMillis)
                                    .atZone(java.time.ZoneOffset.UTC)
                                    .toLocalDate()
                                onValueChange(
                                    date.atTime(timePickerState.hour, timePickerState.minute)
                                        .format(
                                            java.time.format.DateTimeFormatter.ofPattern(
                                                "yyyy-MM-dd HH:mm:ss"
                                            )
                                        )
                                )
                                showTimePicker = false
                            }
                        ) {
                            Text("确定")
                        }
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
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
private fun ChecklistItem(
    checklist: ActionCheckList,
    busy: Boolean,
    onSave: (ActionCheckList) -> Unit,
    onDelete: (String) -> Unit,
    onLongClick: () -> Unit
) {
    var current by remember(checklist.id, checklist.current) { mutableStateOf(checklist.current.toString()) }
    val isTodo = checklist.first == 0 && checklist.last == 1
    val displayContent = checklist.content +
        if (checklist.last > 0 && !isTodo) " (${checklist.first}-${checklist.last})" else ""
    Card(
        Modifier.fillMaxWidth().combinedClickable(
            onClick = {},
            onLongClick = onLongClick
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isTodo) {
                androidx.compose.material3.Checkbox(
                    checked = checklist.current == 1,
                    onCheckedChange = { checked ->
                        if (!busy) onSave(checklist.copy(current = if (checked) 1 else 0))
                    },
                    enabled = !busy
                )
                Text(
                    displayContent,
                    modifier = Modifier.weight(1f),
                    fontWeight = if (checklist.current == 1) FontWeight.Normal else FontWeight.SemiBold,
                    color = if (checklist.current == 1) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface
                )
            } else {
                Text(
                    displayContent,
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                OutlinedTextField(
                    value = current,
                    onValueChange = { current = it },
                    modifier = Modifier.width(76.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                val value = current.toIntOrNull()
                IconButton(
                    onClick = { value?.let { onSave(checklist.copy(current = it)) } },
                    enabled = !busy && value != null && value in checklist.first..checklist.last
                ) {
                    Icon(Icons.Default.Check, contentDescription = "保存进度")
                }
            }
            IconButton(onClick = { onDelete(checklist.id) }, enabled = !busy) {
                Icon(Icons.Default.Delete, contentDescription = "删除")
            }
        }
    }
}

@Composable
private fun TodosScreen(
    vm: ToolboxViewModel,
    modifier: Modifier,
    showCompleted: Boolean,
    onOpenTodo: (TodoRecord) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var appliedQuery by remember { mutableStateOf("") }
    var page by remember { mutableStateOf(1) }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(appliedQuery, page, showCompleted) {
        vm.refreshTodos(appliedQuery, page, if (showCompleted) "done" else "todo")
    }
    LaunchedEffect(showCompleted) {
        page = 1
    }

    Column(modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(16.dp)) {
        Row(Modifier.padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                query,
                { query = it },
                modifier = Modifier.weight(1f),
                label = { Text(if (showCompleted) "搜索已完成" else "搜索待办") },
                singleLine = true
            )
            IconButton(onClick = {
                keyboardController?.hide()
                appliedQuery = query.trim()
                page = 1
            }) {
                Icon(Icons.Default.Search, contentDescription = "搜索")
            }
        }
        if (vm.todos.isEmpty()) {
            EmptyState(
                when {
                    appliedQuery.isNotBlank() && showCompleted -> "没有匹配的已完成事项"
                    appliedQuery.isNotBlank() -> "没有匹配的待办事项"
                    showCompleted -> "暂无已完成事项"
                    else -> "暂无待办事项"
                }
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                vm.todos.forEach { todo ->
                    key(todo.id) {
                        TodoCard(
                            todo,
                            onComplete = { vm.completeTodo(todo) },
                            completed = showCompleted,
                            onClick = { onOpenTodo(todo) }
                        )
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { page-- },
                enabled = page > 1 && !vm.busy
            ) {
                Text("上一页")
            }
            Text("第 $page 页 · ${vm.todos.size} 条")
            OutlinedButton(
                onClick = { page++ },
                enabled = vm.todoHasNextPage && !vm.busy
            ) {
                Text("下一页")
            }
        }
    }
}

@Composable
private fun TodoCard(
    todo: TodoRecord,
    onComplete: () -> Unit,
    completed: Boolean = false,
    onClick: () -> Unit = {}
) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (todo.state == "todo") {
                Switch(
                    checked = false,
                    onCheckedChange = { if (it) onComplete() },
                    enabled = !completed
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                CopyableLinkText(todo.name, fontWeight = FontWeight.SemiBold)
                if (!todo.remark.isNullOrBlank()) {
                    CopyableLinkText(todo.remark, style = MaterialTheme.typography.bodySmall)
                }
                todo.createTime?.takeIf(String::isNotBlank)?.let {
                    Text("$it", style = MaterialTheme.typography.labelSmall)
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
            fontWeight = fontWeight,
            maxLines = 2
        )
    }
}

@Composable
private fun NotesScreen(vm: ToolboxViewModel, modifier: Modifier) {
    var selected by remember { mutableStateOf<NoteRecord?>(null) }
    var content by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    Column(modifier
        .fillMaxSize()
        .padding(16.dp)) {
        OutlinedTextField(
            content,
            { content = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("便笺内容") },
            maxLines = 4
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
            IconButton(onClick = {
                keyboardController?.hide()
                vm.refreshNotes()
            }) {
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

    val focusRecords = vm.timerRecords.filter { it.name.startsWith("workTime-") }
    val dailyFocusStats = focusRecords
        .groupBy { it.end?.take(10) ?: it.start?.take(10) ?: "日期未知" }
        .map { (date, records) ->
            Triple(date, records.sumOf { it.duration ?: 0 }, records.size)
        }
        .sortedByDescending { it.first }
    val totalFocusSeconds = focusRecords.sumOf { it.duration ?: 0 }

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
            Text("每日专注统计", style = MaterialTheme.typography.titleMedium)
            if (dailyFocusStats.isEmpty()) {
                Text("暂无专注记录", style = MaterialTheme.typography.bodySmall)
            } else {
                dailyFocusStats.forEach { (date, duration, count) ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(date, modifier = Modifier.weight(1f))
                        Text("${formatTimerDuration(duration.toLong())} · $count 次")
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
private fun FilesTextEditorScreen(
    modifier: Modifier,
    vm: ToolboxViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val file = remember(context) { java.io.File(context.filesDir, "text-editor.txt") }
    var content by remember { mutableStateOf("") }
    var loaded by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(file) {
        try {
            content = withContext(Dispatchers.IO) {
                if (file.isFile) file.readText() else ""
            }
        } catch (error: java.io.IOException) {
            vm.showMessage("读取文本失败：${error.message ?: "未知错误"}")
        } finally {
            loaded = true
        }
    }

    Column(
        modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = content,
            onValueChange = { content = it },
            modifier = Modifier.fillMaxWidth().weight(1f),
            label = { Text("文本内容") },
            placeholder = { Text("在此输入文本") },
            enabled = loaded && !saving && !vm.busy
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    saving = true
                    scope.launch {
                        try {
                            withContext(Dispatchers.IO) { file.writeText(content) }
                            vm.showMessage("已保存到应用目录：${file.name}")
                        } catch (error: java.io.IOException) {
                            vm.showMessage("保存文本失败：${error.message ?: "未知错误"}")
                        } finally {
                            saving = false
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = loaded && !saving && !vm.busy
            ) {
                Text("保存")
            }
            OutlinedButton(
                onClick = { content = "" },
                modifier = Modifier.weight(1f),
                enabled = loaded && !saving && !vm.busy
            ) {
                Text("清空")
            }
            OutlinedButton(
                onClick = { vm.uploadTextFile(content) },
                modifier = Modifier.weight(1f),
                enabled = loaded && !saving && !vm.busy
            ) {
                Text("上传")
            }
        }
    }
}

@Composable
private fun FilesScreen(
    modifier: Modifier,
    vm: ToolboxViewModel,
    downloadMode: Boolean,
    onDownload: () -> Unit,
    onBackToUpload: () -> Unit
) {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("lan_share", android.content.Context.MODE_PRIVATE) }
    var receiverUrl by remember { mutableStateOf(preferences.getString("receiver_url", "").orEmpty()) }
    var token by remember { mutableStateOf(preferences.getString("token", "").orEmpty()) }
    var selectedFiles by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var remoteDirectory by remember { mutableStateOf("") }
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

    fun listRemoteFiles() {
        preferences.edit()
            .putString("receiver_url", receiverUrl.trim())
            .putString("token", token.trim())
            .apply()
        vm.listLanShareFiles(receiverUrl.trim(), token.trim(), remoteDirectory.trim())
    }

    fun downloadSelectedFiles() {
        vm.downloadLanShareFiles(
            receiverUrl.trim(),
            token.trim(),
            vm.lanShareDownloadState.selectedFiles.sorted()
        )
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
        if (!downloadMode) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { picker.launch(arrayOf("*/*")) },
                    enabled = !vm.busy
                ) {
                    Text("选择文件")
                }
                Button(
                    onClick = ::uploadSelectedFiles,
                    enabled = selectedFiles.isNotEmpty() && receiverUrl.isNotBlank() && token.isNotBlank() && !vm.busy
                ) {
                    Text("上传 ${selectedFiles.size}")
                }
                OutlinedButton(
                    onClick = onDownload,
                    enabled = !vm.busy
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("下载")
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
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = onBackToUpload, enabled = !vm.busy) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("返回上传")
                }
                Text("下载共享文件", style = MaterialTheme.typography.titleMedium)
            }
            OutlinedTextField(
                value = remoteDirectory,
                onValueChange = { remoteDirectory = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("服务端目录") },
                placeholder = { Text("留空表示共享根目录") },
                singleLine = true,
                enabled = !vm.busy
            )
            Button(
                onClick = ::listRemoteFiles,
                enabled = receiverUrl.isNotBlank() && token.isNotBlank() && !vm.busy
            ) {
                Text("列出文件")
            }
            val downloadState = vm.lanShareDownloadState
            if (downloadState.isListing || (vm.busy && downloadState.currentFileIndex > 0)) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            if (downloadState.error != null) {
                Text(downloadState.error, color = MaterialTheme.colorScheme.error)
            }
            if (downloadState.files.isNotEmpty()) {
                Text("文件列表 (${downloadState.files.size})", style = MaterialTheme.typography.titleSmall)
                downloadState.files.forEach { remoteFile ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable(enabled = !vm.busy) {
                            vm.toggleLanShareDownloadFile(remoteFile, remoteFile !in downloadState.selectedFiles)
                        },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = remoteFile in downloadState.selectedFiles,
                            onCheckedChange = { selected -> vm.toggleLanShareDownloadFile(remoteFile, selected) },
                            enabled = !vm.busy
                        )
                        Text(remoteFile, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Button(
                    onClick = ::downloadSelectedFiles,
                    enabled = downloadState.selectedFiles.isNotEmpty() && !vm.busy
                ) {
                    Text("下载所选 (${downloadState.selectedFiles.size})")
                }
            } else if (!downloadState.isListing && downloadState.error == null) {
                Text("输入服务端目录并列出文件")
            }
            if (downloadState.totalFiles > 0) {
                Text(
                    when {
                        downloadState.error != null -> "下载失败：${downloadState.error}"
                        downloadState.isComplete -> "下载完成：${downloadState.completedFiles.size}/${downloadState.totalFiles}"
                        else -> "正在下载 ${downloadState.currentFileIndex}/${downloadState.totalFiles}：${downloadState.currentFileName}"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (downloadState.error != null) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurface
                )
                if (vm.busy && downloadState.currentFileIndex > 0) {
                    val progress = if (downloadState.totalBytes > 0) {
                        (downloadState.bytesReceived.toFloat() / downloadState.totalBytes).coerceIn(0f, 1f)
                    } else null
                    if (progress != null) {
                        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                        Text(
                            "${formatFileSize(downloadState.bytesReceived)} / ${formatFileSize(downloadState.totalBytes)} · ${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall
                        )
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text("${formatFileSize(downloadState.bytesReceived)} 已接收", style = MaterialTheme.typography.labelSmall)
                    }
                }
                if (downloadState.completedFiles.isNotEmpty()) {
                    Text("已保存到 Downloads/Sourcebox", style = MaterialTheme.typography.titleSmall)
                    downloadState.completedFiles.forEach { completed ->
                        Text(completed, style = MaterialTheme.typography.bodySmall)
                    }
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
    photoCacheSizeMb: Int,
    onPhotoCacheSizeMb: (Int) -> Unit,
    onClearLocalDatabase: () -> Unit,
    onViewCrashLogs: () -> Unit
) {
    var showClearDatabaseConfirmation by remember { mutableStateOf(false) }
    var cacheSizeText by remember(photoCacheSizeMb) {
        mutableStateOf(photoCacheSizeMb.toString())
    }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("后端连接", style = MaterialTheme.typography.headlineSmall)
        Text("例如 http://10.0.2.2:38080。")
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
        Text("照片缓存", style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = cacheSizeText,
                onValueChange = { cacheSizeText = it.filter(Char::isDigit).take(4) },
                modifier = Modifier.weight(1f),
                label = { Text("磁盘缓存大小（MB）") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
            Button(onClick = {
                val size = cacheSizeText.toIntOrNull()?.coerceIn(10, 1024) ?: 100
                cacheSizeText = size.toString()
                onPhotoCacheSizeMb(size)
            }) {
                Text("保存")
            }
        }
        Text("默认 100 MB", style = MaterialTheme.typography.bodySmall)
        OutlinedButton(
            onClick = { showClearDatabaseConfirmation = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Delete, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("清除本地数据库")
        }
        OutlinedButton(onClick = onViewCrashLogs, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.ErrorOutline, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("查看异常日志")
        }
        Text("使用局域网或生产服务时，建议启用 HTTPS 并保护服务端访问凭据。", style = MaterialTheme.typography.bodySmall)
        Text(
            "CofCool@TheSourceBox ${BuildConfig.BUILD_DATE}(${BuildConfig.COMMIT_ID})",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    if (showClearDatabaseConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearDatabaseConfirmation = false },
            title = { Text("清除本地数据库？") },
            text = {
                Text("将清除本地缓存、同步信息和所有待上传的离线修改，此操作不可撤销")
            },
            confirmButton = {
                TextButton(onClick = {
                    showClearDatabaseConfirmation = false
                    onClearLocalDatabase()
                }) {
                    Text("清除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDatabaseConfirmation = false }) {
                    Text("取消")
                }
            }
        )
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
                error = "加载异常日志失败：${exception.message ?: "未知错误"}"
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
            Text("异常日志（${logs.size}）", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
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
                            error = "清理异常日志失败：${exception.message ?: "未知错误"}"
                        }
                    }
                },
                enabled = logs.isNotEmpty() && !loading
            ) { Text("清空") }
        }
        Text(
            "包含应用崩溃和 HTTP API 请求失败记录，仅保存在本机，最多保留最近 20 份。",
            style = MaterialTheme.typography.bodySmall
        )
        if (error.isNotBlank()) {
            Text(error, color = MaterialTheme.colorScheme.error)
        } else if (logs.isEmpty() && !loading) {
            EmptyState("暂无异常日志")
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
                                error = "读取异常日志失败：${exception.message ?: "未知错误"}"
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
                            Text(
                                if (log.isApiFailure) "API 请求失败 · ${log.name}" else "应用崩溃 · ${log.name}",
                                fontWeight = FontWeight.SemiBold
                            )
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
                                        error = "删除异常日志失败：${exception.message ?: "未知错误"}"
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
