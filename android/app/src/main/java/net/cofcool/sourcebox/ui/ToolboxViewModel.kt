package net.cofcool.sourcebox.ui

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
import net.cofcool.sourcebox.data.ActionTypeResponse
import net.cofcool.sourcebox.data.CommandRecord
import net.cofcool.sourcebox.data.HabitDefinition
import net.cofcool.sourcebox.data.HabitRecord
import net.cofcool.sourcebox.data.HabitValueChange
import net.cofcool.sourcebox.data.NoteRecord
import net.cofcool.sourcebox.data.ServerResponseException
import net.cofcool.sourcebox.data.SourceboxApi
import net.cofcool.sourcebox.data.TodoRecord
import net.cofcool.sourcebox.data.ToolInfo
import net.cofcool.sourcebox.data.isHabitRequiredOnDate
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.LocalDate
import java.util.UUID

private const val DIRTY_SYNC_RETRY_INTERVAL = 30_000L
private const val TIMER_NOTIFICATION_CHANNEL = "pomodoro_timer_alarm"
private const val TIMER_NOTIFICATION_ID = 1001

data class PomodoroTimerState(
    val workMinutes: Int = 40,
    val breakMinutes: Int = 5,
    val isWorkPhase: Boolean = true,
    val remainingSeconds: Long = 40 * 60L,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val completedCycles: Int = 0
)

data class LanShareUploadState(
    val totalFiles: Int = 0,
    val currentFileIndex: Int = 0,
    val currentFileName: String = "",
    val bytesSent: Long = 0,
    val totalBytes: Long = -1,
    val completedFiles: List<String> = emptyList(),
    val isComplete: Boolean = false,
    val error: String? = null
)

data class LanShareDownloadState(
    val files: List<String> = emptyList(),
    val selectedFiles: Set<String> = emptySet(),
    val isListing: Boolean = false,
    val totalFiles: Int = 0,
    val currentFileIndex: Int = 0,
    val currentFileName: String = "",
    val bytesReceived: Long = 0,
    val totalBytes: Long = -1,
    val completedFiles: List<String> = emptyList(),
    val isComplete: Boolean = false,
    val error: String? = null
)

private data class ExportData(
    val actions: List<ActionExport>,
    val notes: List<NoteRecord>,
    val types: ActionTypeResponse
)

class ToolboxViewModel(application: Application) : AndroidViewModel(application) {
    private val api = SourceboxApi(application)
    private val actionDatabase = ActionExportDatabase(application)
    private val timerPreferences = application.getSharedPreferences("pomodoro_timer", Context.MODE_PRIVATE)
    private val notificationManager =
        application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val syncMutex = Mutex()
    private var timerJob: Job? = null
    private var timerDeadlineElapsed = 0L
    var timerState by mutableStateOf(
        timerPreferences.getInt("work_minutes", 40).coerceIn(1, 240).let { workMinutes ->
            val breakMinutes = timerPreferences.getInt("break_minutes", 5).coerceIn(1, 240)
            PomodoroTimerState(
                workMinutes = workMinutes,
                breakMinutes = breakMinutes,
                remainingSeconds = workMinutes * 60L
            )
        }
    )
        private set
    var timerRecords by mutableStateOf<List<ActionRecord>>(emptyList())
        private set
    var lanShareUploadState by mutableStateOf(LanShareUploadState())
        private set
    var lanShareDownloadState by mutableStateOf(LanShareDownloadState())
        private set
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
    var todoHasNextPage by mutableStateOf(false)
        private set
    var todayActions by mutableStateOf<List<ActionRecord>>(emptyList())
        private set
    var commands by mutableStateOf<List<CommandRecord>>(emptyList())
        private set
    var records by mutableStateOf<List<ActionRecord>>(emptyList())
        private set
    var habits by mutableStateOf<List<ActionRecord>>(emptyList())
        private set
    var habitExports by mutableStateOf<Map<String, ActionExport>>(emptyMap())
        private set
    var books by mutableStateOf<List<ActionRecord>>(emptyList())
        private set
    var bookHasNextPage by mutableStateOf(false)
        private set
    var habitRecordDetails by mutableStateOf<HabitRecord?>(null)
        private set
    var recordCandidates by mutableStateOf<List<ActionRecord>>(emptyList())
        private set
    private var recordCandidateQuery = ""
    private var currentTodoQuery = ""
    private var currentTodoPage = 1
    private var currentTodoState = "todo"
    var recordCategories by mutableStateOf<List<String>>(emptyList())
        private set
    var recordLabels by mutableStateOf<List<String>>(emptyList())
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
    var pendingChangeCount by mutableStateOf(actionDatabase.pendingChangeCount())
        private set
    var lastTypeSync by mutableStateOf(actionDatabase.lastSyncedAt("types"))
        private set

    init {
        createTimerNotificationChannel()
        connectivityManager.registerDefaultNetworkCallback(networkCallback)
        refreshTools()
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

    fun refreshTodayActions() = perform {
        loadTodayActions()
    }

    private suspend fun loadTodayActions() {
        val today = java.time.LocalDate.now()
        todayActions = withContext(Dispatchers.IO) {
            val date = today.toString()
            val todayTodos = actionDatabase.actionsStartingOn(date, "todo")
            val activeRecords = actionDatabase.randomActions("record", "doing", 5)
            val dueHabits = actionDatabase.allHabitExports()
                .filter { export ->
                    export.record.state == "doing"
                }
                .filter { export ->
                    val habit = export.habit
                    val ck = export.checklists
                    val completedDate = ck
                        .asSequence()
                        .filter { it.current >= 1 }
                        .mapNotNull { entry ->
                            entry.createTime?.take(10)?.let {
                                runCatching {
                                    LocalDate.parse(it)
                                }.getOrNull()
                            }
                        }
                        .maxOrNull()
                    val d = completedDate ?: LocalDate.parse(habit?.createTime!!.take(10))

                    isHabitRequiredOnDate(
                        d,
                        habit?.freqNum,
                        habit?.freqDen,
                        today
                    )
                }
                .map { it.record }
            var n = todayTodos + activeRecords + dueHabits
            n = n.sortedByDescending { it
                it.start
            }
            n
        }
    }

    fun clearLocalDatabase() = perform {
        syncMutex.withLock {
            withContext(Dispatchers.IO) { actionDatabase.clearLocalData() }
        }
        notes = emptyList()
        todos = emptyList()
        todayActions = emptyList()
        commands = emptyList()
        records = emptyList()
        habits = emptyList()
        habitExports = emptyMap()
        books = emptyList()
        bookHasNextPage = false
        habitRecordDetails = null
        recordCandidates = emptyList()
        recordCandidateQuery = ""
        recordCategories = emptyList()
        recordLabels = emptyList()
        recordComments = emptyList()
        recordChecklists = emptyList()
        recordHasNextPage = false
        savedRecord = null
        timerRecords = emptyList()
        localActionCount = 0
        localNoteCount = 0
        lastActionSync = null
        lastNoteSync = null
        lastTypeSync = null
        pendingChangeCount = 0
        message = "本地数据库和离线待同步修改已清除"
    }

    fun showMessage(text: String) {
        message = text
    }

    fun uploadTextFile(content: String) = perform {
        api.uploadTextFile("text-editor.txt", content)
        message = "文本已上传"
    }

    fun uploadLanShareFiles(receiverUrl: String, token: String, files: List<Uri>) = perform {
        require(files.isNotEmpty()) { "请先选择要上传的文件" }
        lanShareUploadState = LanShareUploadState(totalFiles = files.size)
        files.forEachIndexed { index, uri ->
            lanShareUploadState = lanShareUploadState.copy(
                currentFileIndex = index + 1,
                currentFileName = uri.lastPathSegment?.substringAfterLast('/') ?: "文件 ${index + 1}",
                bytesSent = 0,
                totalBytes = -1,
                error = null
            )
            try {
                val result = api.uploadLanShareFile(getApplication(), receiverUrl, token, uri) { name, sent, total ->
                    viewModelScope.launch {
                        lanShareUploadState = lanShareUploadState.copy(
                            currentFileName = name,
                            bytesSent = sent,
                            totalBytes = total
                        )
                    }
                }
                lanShareUploadState = lanShareUploadState.copy(
                    completedFiles = lanShareUploadState.completedFiles + result,
                    bytesSent = lanShareUploadState.totalBytes.coerceAtLeast(lanShareUploadState.bytesSent)
                )
            } catch (error: Exception) {
                lanShareUploadState = lanShareUploadState.copy(error = error.message ?: "未知错误")
                throw IOException(
                    "已上传 ${lanShareUploadState.completedFiles.size}/${files.size} 个文件；第 ${index + 1} 个文件失败：${error.message ?: "未知错误"}",
                    error
                )
            }
        }
        lanShareUploadState = lanShareUploadState.copy(isComplete = true)
        message = "已上传 ${lanShareUploadState.completedFiles.size} 个文件"
    }

    fun listLanShareFiles(receiverUrl: String, token: String, path: String) = perform {
        lanShareDownloadState = LanShareDownloadState(isListing = true)
        try {
            val files = api.listLanShareFiles(receiverUrl, token, path)
            lanShareDownloadState = LanShareDownloadState(files = files)
            message = "找到 ${files.size} 个文件"
        } catch (error: Exception) {
            lanShareDownloadState = lanShareDownloadState.copy(isListing = false, error = error.message ?: "获取文件列表失败")
            throw error
        }
    }

    fun resetLanShareDownloadState() {
        lanShareDownloadState = LanShareDownloadState()
    }

    fun toggleLanShareDownloadFile(path: String, selected: Boolean) {
        val selection = lanShareDownloadState.selectedFiles.toMutableSet()
        if (selected) selection.add(path) else selection.remove(path)
        lanShareDownloadState = lanShareDownloadState.copy(selectedFiles = selection)
    }

    fun downloadLanShareFiles(receiverUrl: String, token: String, files: List<String>) = perform {
        require(files.isNotEmpty()) { "请先选择要下载的文件" }
        lanShareDownloadState = LanShareDownloadState(
            files = lanShareDownloadState.files,
            selectedFiles = files.toSet(),
            totalFiles = files.size
        )
        files.forEachIndexed { index, path ->
            lanShareDownloadState = lanShareDownloadState.copy(
                currentFileIndex = index + 1,
                currentFileName = path,
                bytesReceived = 0,
                totalBytes = -1,
                error = null
            )
            try {
                val destination = api.downloadLanShareFile(getApplication(), receiverUrl, token, path) { name, received, total ->
                    viewModelScope.launch {
                        lanShareDownloadState = lanShareDownloadState.copy(
                            currentFileName = name,
                            bytesReceived = received,
                            totalBytes = total
                        )
                    }
                }
                lanShareDownloadState = lanShareDownloadState.copy(
                    completedFiles = lanShareDownloadState.completedFiles + destination,
                    bytesReceived = lanShareDownloadState.totalBytes.coerceAtLeast(lanShareDownloadState.bytesReceived)
                )
            } catch (error: Exception) {
                lanShareDownloadState = lanShareDownloadState.copy(error = error.message ?: "下载失败")
                throw IOException(
                    "已下载 ${lanShareDownloadState.completedFiles.size}/${files.size} 个文件；第 ${index + 1} 个文件失败：${error.message ?: "未知错误"}",
                    error
                )
            }
        }
        lanShareDownloadState = lanShareDownloadState.copy(isComplete = true)
        message = "已下载 ${lanShareDownloadState.completedFiles.size} 个文件到 Downloads/Sourcebox"
    }

    fun refreshTimerRecords() = perform {
        timerRecords = withContext(Dispatchers.IO) {
            actionDatabase.allActions("timer")
        }
    }

    fun startPomodoro(workMinutes: Int, breakMinutes: Int) {
        if (timerState.isRunning) return
        if (timerState.isPaused) {
            beginTimerCountdown(timerState.remainingSeconds)
            return
        }
        if (workMinutes !in 1..240 || breakMinutes !in 1..240) {
            message = "专注和休息时长必须在 1 到 240 分钟之间"
            return
        }
        timerPreferences.edit()
            .putInt("work_minutes", workMinutes)
            .putInt("break_minutes", breakMinutes)
            .apply()
        timerState = timerState.copy(
            workMinutes = workMinutes,
            breakMinutes = breakMinutes,
            isWorkPhase = true,
            remainingSeconds = workMinutes * 60L,
            isPaused = false
        )
        beginTimerCountdown(timerState.remainingSeconds)
    }

    fun pausePomodoro() {
        if (!timerState.isRunning) return
        val remaining = ((timerDeadlineElapsed - SystemClock.elapsedRealtime() + 999) / 1000)
            .coerceIn(0, timerState.remainingSeconds)
        timerJob?.cancel()
        timerJob = null
        timerState = timerState.copy(
            remainingSeconds = remaining,
            isRunning = false,
            isPaused = true
        )
    }

    fun resetPomodoro() {
        val current = timerState
        if (current.isRunning || current.isPaused) {
            val targetSeconds = phaseDurationSeconds(current)
            val elapsedSeconds = (targetSeconds - current.remainingSeconds).coerceAtLeast(0)
            if (elapsedSeconds > 0) {
                saveTimerPhase(current.isWorkPhase, elapsedSeconds, "reset")
            }
        }
        timerJob?.cancel()
        timerJob = null
        timerState = timerState.copy(
            isWorkPhase = true,
            remainingSeconds = timerState.workMinutes * 60L,
            isRunning = false,
            isPaused = false
        )
    }

    private fun beginTimerCountdown(seconds: Long) {
        timerJob?.cancel()
        timerDeadlineElapsed = SystemClock.elapsedRealtime() + seconds * 1000
        timerState = timerState.copy(isRunning = true, isPaused = false)
        timerJob = viewModelScope.launch {
            while (timerState.isRunning) {
                delay(1_000)
                val remaining = ((timerDeadlineElapsed - SystemClock.elapsedRealtime() + 999) / 1000)
                    .coerceAtLeast(0)
                timerState = timerState.copy(remainingSeconds = remaining)
                if (remaining == 0L) {
                    completeTimerPhase()
                    break
                }
            }
        }
    }

    private fun completeTimerPhase() {
        val finished = timerState
        val durationSeconds = phaseDurationSeconds(finished)
        saveTimerPhase(finished.isWorkPhase, durationSeconds, "finish")
        if (finished.isWorkPhase) {
            notifyTimer("专注时间结束", "休息时间到了，请放松一下。")
            timerState = finished.copy(
                isWorkPhase = false,
                remainingSeconds = finished.breakMinutes * 60L,
                isRunning = false,
                isPaused = false
            )
            beginTimerCountdown(timerState.remainingSeconds)
        } else {
            notifyTimer("休息时间结束", "本轮番茄钟已完成，可以开始下一轮专注。")
            timerState = finished.copy(
                isWorkPhase = true,
                remainingSeconds = finished.workMinutes * 60L,
                isRunning = false,
                isPaused = false,
                completedCycles = finished.completedCycles + 1
            )
        }
    }

    private fun phaseDurationSeconds(state: PomodoroTimerState): Long =
        (if (state.isWorkPhase) state.workMinutes else state.breakMinutes) * 60L

    private fun saveTimerPhase(isWorkPhase: Boolean, durationSeconds: Long, result: String) {
        if (durationSeconds <= 0) return
        viewModelScope.launch {
            try {
                val end = java.time.LocalDateTime.now()
                val start = end.minusSeconds(durationSeconds)
                val endText = end.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                val startText = start.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                val createdAt = currentRecordDateTime()
                val phase = if (isWorkPhase) "workTime" else "breakTime"
                val action = ActionRecord(
                    id = UUID.randomUUID().toString().replace("-", ""),
                    name = "$phase-${System.currentTimeMillis() / 1000}",
                    type = "timer",
                    state = "done",
                    start = startText,
                    end = endText,
                    duration = durationSeconds.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
                    createTime = createdAt,
                    remark = when {
                        result == "reset" -> "reset"
                        isWorkPhase -> "finish"
                        else -> "break-finish"
                    }
                )
                val (_, isDirty) = writeWithOfflineFallback(
                    server = {
                        api.saveAction(action).also { saved ->
                            withContext(Dispatchers.IO) { actionDatabase.saveAction(saved) }
                        }
                    },
                    offline = {
                        withContext(Dispatchers.IO) { actionDatabase.markActionDirty(action) }
                        action
                    }
                )
                timerRecords = withContext(Dispatchers.IO) {
                    actionDatabase.allActions("timer")
                }
                localActionCount = withContext(Dispatchers.IO) { actionDatabase.actionCount() }
                refreshPendingCount()
                message = if (isDirty) "番茄阶段数据已保存到本地，等待联网同步" else "番茄阶段数据已保存并上传"
            } catch (error: Exception) {
                message = "番茄阶段数据保存失败：${error.message ?: "未知错误"}"
            }
        }
    }

    private fun createTimerNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: throw IllegalStateException("系统未提供默认闹钟铃声")
            val alarmAudioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    TIMER_NOTIFICATION_CHANNEL,
                    "番茄钟提醒",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "番茄钟专注和休息阶段结束提醒"
                    setSound(alarmSound, alarmAudioAttributes)
                    enableVibration(true)
                }
            )
        }
    }

    private fun notifyTimer(title: String, text: String) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            getApplication<Application>().checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            message = "阶段已结束；请开启通知权限以接收番茄钟提醒"
            return
        }
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(getApplication(), TIMER_NOTIFICATION_CHANNEL)
        } else {
            Notification.Builder(getApplication()).apply {
                setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
                setPriority(Notification.PRIORITY_MAX)
            }
        }
        val notification = builder
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(text)
            .setCategory(Notification.CATEGORY_ALARM)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(TIMER_NOTIFICATION_ID, notification)
    }

    fun syncAllData() {
        if (api.baseUrl().isBlank()) return
        perform {
            syncMutex.withLock {
                uploadPendingChanges()
                val exported = coroutineScope {
                    val actions = async {
                        api.exportAllActions().map { exportedAction ->
                            if (exportedAction.record.type != "habit") {
                                exportedAction
                            } else {
                                val habit = api.findHabitRecord(exportedAction.record.id)
                                exportedAction.copy(
                                    record = habit.action,
                                    checklists = habit.checkLists,
                                    habit = habit.habit
                                )
                            }
                        }
                    }
                    val notes = async { api.exportAllNotes() }
                    val types = async { api.actionTypes() }
                    ExportData(actions.await(), notes.await(), types.await())
                }
                val syncedAt = currentRecordDateTime()
                withContext(Dispatchers.IO) {
                    actionDatabase.replaceExportData(
                        actions = exported.actions,
                        notes = exported.notes,
                        actionTypes = exported.types,
                        syncedAt = syncedAt
                    )
                }
                localActionCount = exported.actions.size
                lastActionSync = syncedAt
                localNoteCount = exported.notes.size
                lastNoteSync = syncedAt
                lastTypeSync = syncedAt
                refreshPendingCount()
                recordCategories = withContext(Dispatchers.IO) { actionDatabase.actionCategories() }
                loadTodayActions()
                timerRecords = withContext(Dispatchers.IO) {
                    actionDatabase.allActions("timer")
                }
                message = "已同步 ${exported.actions.size} 条行动、${exported.notes.size} 条便笺、${exported.types.types.size} 个类型"
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
            Log.d("API", "call server error: $error")
            if (error !is IOException && (error !is ServerResponseException || !error.isRetryable)) {
                throw error
            }
            offline() to true
        }
    }

    private suspend fun <T> writeRecordDetailWithOfflineFallback(
        server: suspend () -> T,
        offline: suspend () -> T
    ): Pair<T, Boolean> = syncMutex.withLock {
        try {
            server() to false
        } catch (error: Exception) {
            Log.d("API", "save record detail error: $error")
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
                "command" to "save", "command" to "delete", "command" to "store" -> Unit
                "action" to "save" -> {
                    val action = json.decodeFromString<ActionExport>(change.payload).record
                    val saved = api.saveAction(action)
                    val local = withContext(Dispatchers.IO) { actionDatabase.actionDetails(change.entityId) }
                        ?: ActionExport(saved)
                    canonicalPayload = json.encodeToString(local.copy(record = saved))
                }
                "action" to "habit_save" -> {
                    val habit = json.decodeFromString<HabitRecord>(change.payload)
                    val saved = api.saveHabit(habit)
                    val local = withContext(Dispatchers.IO) { actionDatabase.actionDetails(change.entityId) }
                        ?: ActionExport(saved)
                    canonicalPayload = json.encodeToString(
                        local.copy(
                            record = saved,
                            habit = habit.habit.copy(actionId = saved.id)
                        )
                    )
                }
                "action" to "habit_value" -> {
                    val value = json.decodeFromString<HabitValueChange>(change.payload)
                    val saved = api.saveHabitValue(change.entityId, value.checklist)
                    withContext(Dispatchers.IO) {
                        actionDatabase.replaceHabitValue(change.entityId, value.checklistId, saved)
                    }
                }
                "action" to "habit_delete" -> api.deleteHabit(change.entityId)
                "action" to "comment" -> {
                    val comment = json.decodeFromString<ActionComment>(change.payload)
                    api.addRecordComment(change.entityId, comment)
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

    fun refreshTodos(query: String = "", page: Int = 1, state: String = "todo") = perform {
        require(page > 0) { "页码必须大于 0" }
        require(state == "todo" || state == "done") { "待办状态无效" }
        currentTodoQuery = query
        currentTodoPage = page
        currentTodoState = state
        loadTodos(query, page, state)
    }

    private suspend fun loadTodos(query: String, page: Int, state: String) {
        val actionQuery = buildString {
            append("state=$state")
            if (query.isNotBlank()) append("&name=${java.net.URLEncoder.encode(query, "UTF-8")}")
        }
        val result = withContext(Dispatchers.IO) {
            actionDatabase.actions("todo", actionQuery, page, 11)
        }
        todos = result.take(10).map { it.toTodoRecord() }
        todoHasNextPage = result.size > 10
    }

    fun addTodo(name: String, remark: String) = perform {
        require(name.isNotBlank()) { "待办名称不能为空" }
        val now = currentRecordDateTime()
        val pendingAction = ActionRecord(
            id = UUID.randomUUID().toString().replace("-", ""),
            name = name.trim(),
            remark = remark.trim(),
            state = "todo",
            type = "todo",
            start = now,
            end = now,
            createTime = now
        )
        val (saved, isDirty) = writeWithOfflineFallback(
            server = {
                api.addTodo(pendingAction)
                withContext(Dispatchers.IO) { actionDatabase.saveAction(pendingAction) }
                pendingAction
            },
            offline = {
                withContext(Dispatchers.IO) { actionDatabase.markActionDirty(pendingAction) }
                pendingAction
            }
        )
        localActionCount = withContext(Dispatchers.IO) { actionDatabase.actionCount() }
        refreshPendingCount()
        loadTodos(currentTodoQuery, currentTodoPage, currentTodoState)
        loadTodayActions()
        message = if (isDirty) "待办已添加到本地，等待联网同步" else "待办已添加"
    }

    fun createTodo(todo: TodoRecord, onCreated: () -> Unit = {}) = perform(
        onSuccess = { (_, isDirty) ->
            refreshPendingCount()
            loadTodos(currentTodoQuery, currentTodoPage, currentTodoState)
            loadTodayActions()
            message = if (isDirty) "待办已添加到本地，等待联网同步" else "待办已添加"
            onCreated()
        }
    ) {
        require(todo.name.isNotBlank()) { "待办名称不能为空" }
        val now = currentRecordDateTime()
        val pendingAction = todo.toActionRecord().copy(
            id = UUID.randomUUID().toString().replace("-", ""),
            name = todo.name.trim(),
            state = "todo",
            type = "todo",
            start = todo.start ?: now,
            end = todo.end ?: now,
            createTime = now
        )
        val (saved, isDirty) = writeWithOfflineFallback(
            server = {
                api.addTodo(pendingAction)
                withContext(Dispatchers.IO) { actionDatabase.saveAction(pendingAction) }
                pendingAction
            },
            offline = {
                withContext(Dispatchers.IO) { actionDatabase.markActionDirty(pendingAction) }
                pendingAction
            }
        )
        localActionCount = withContext(Dispatchers.IO) { actionDatabase.actionCount() }
        saved.toTodoRecord() to isDirty
    }

    fun completeTodo(todo: TodoRecord) = perform {
        val current = withContext(Dispatchers.IO) { actionDatabase.actionDetails(todo.id) }
            ?.record ?: todo.toActionRecord()
        val updatedLocal = current.copy(state = "done")
        val (updated, isDirty) = writeWithOfflineFallback(
            server = {
                api.completeTodo(updatedLocal).also { saved ->
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
        loadTodos(currentTodoQuery, currentTodoPage, currentTodoState)
        loadTodayActions()
        message = if (isDirty) "待办已在本地完成，等待联网同步" else "待办已完成"
    }

    fun saveTodo(todo: TodoRecord, onSaved: () -> Unit = {}) = perform(
        onSuccess = { (saved, isDirty) ->
            todos = todos.map { if (it.id == saved.id) saved else it }
            loadTodayActions()
            message = if (isDirty) "待办已保存到本地，等待联网同步" else "待办已保存"
            onSaved()
        }
    ) {
        require(todo.name.isNotBlank()) { "待办名称不能为空" }
        val current = withContext(Dispatchers.IO) { actionDatabase.actionDetails(todo.id) }
            ?.record ?: todo.toActionRecord()
        val updatedLocal = current.copy(
            name = todo.name.trim(),
            remark = todo.remark?.trim()?.ifBlank { null },
            start = todo.start,
            category = todo.category,
            refs = todo.refs
        )
        val (saved, isDirty) = writeWithOfflineFallback(
            server = {
                api.saveAction(updatedLocal).also { action ->
                    withContext(Dispatchers.IO) { actionDatabase.saveAction(action) }
                }
            },
            offline = {
                withContext(Dispatchers.IO) { actionDatabase.markActionDirty(updatedLocal) }
                updatedLocal
            }
        )
        refreshPendingCount()
        saved.toTodoRecord() to isDirty
    }

    fun refreshCommands(query: String = "") = perform {
        commands = withContext(Dispatchers.IO) { actionDatabase.commands(query) }
    }

    fun saveCommand(command: CommandRecord) = perform {
        require(command.cmd.isNotBlank()) { "命令内容不能为空" }
        val prepared = command.withStableId()
        val saved = api.saveCommand(prepared)
        withContext(Dispatchers.IO) { actionDatabase.saveCommand(saved) }
        commands = withContext(Dispatchers.IO) { actionDatabase.commands("") }
        message = "命令已保存"
    }

    fun deleteCommand(id: String) = perform {
        api.deleteCommand(id)
        withContext(Dispatchers.IO) { actionDatabase.deleteCommand(id) }
        commands = withContext(Dispatchers.IO) { actionDatabase.commands("") }
        message = "命令已删除"
    }

    fun refreshRecords(
        query: String = "",
        page: Int = 1,
        category: String? = null,
        label: String? = null,
        state: String? = null,
        startYear: Int? = null,
        endYear: Int? = null
    ) = perform {
        val filters = buildList {
            query.takeIf(String::isNotBlank)?.let {
                if (it.startsWith("#")) {
                    add("label=${java.net.URLEncoder.encode(it.substring(1), "UTF-8")}")
                } else {
                    add("name=${java.net.URLEncoder.encode(it, "UTF-8")}")
                }
            }
            category?.let { add("category=${java.net.URLEncoder.encode(it, "UTF-8")}") }
            label?.let { add("label=${java.net.URLEncoder.encode(it, "UTF-8")}") }
            state?.let { add("state=${java.net.URLEncoder.encode(it, "UTF-8")}") }
            startYear?.let { add("startYear=$it") }
            endYear?.let { add("endYear=$it") }
        }
        val localQuery = filters.joinToString("&")
        val result = withContext(Dispatchers.IO) {
            actionDatabase.actions("record", localQuery, page, 11)
        }
        records = result.take(10)
        recordHasNextPage = result.size > 10
    }

    fun refreshHabits() = perform {
        val exports = withContext(Dispatchers.IO) { actionDatabase.allHabitExports() }
        habits = exports.map { it.record }
        habitExports = exports.associateBy { it.record.id }
    }

    fun refreshBooks(query: String = "", category: String = "", page: Int = 1) = perform {
        require(page > 0) { "页码必须大于 0" }
        val filters = buildList {
            query.trim().takeIf(String::isNotBlank)?.let {
                add("name=${java.net.URLEncoder.encode(it, "UTF-8")}")
            }
            category.takeIf(String::isNotBlank)?.let {
                add("category=${java.net.URLEncoder.encode(it, "UTF-8")}")
            }
        }.joinToString("&")
        val result = withContext(Dispatchers.IO) {
            actionDatabase.actions("book", filters, page, 11)
        }
        books = result.take(10)
        bookHasNextPage = result.size > 10
    }

    fun createHabit(
        name: String,
        category: String,
        note: String,
        freqNum: Int,
        freqDen: Int,
        unit: String,
        question: String,
        targetValue: Int?,
        targetType: String?,
        onCreated: () -> Unit = {}
    ) = perform(
        onSuccess = { (_, isDirty) ->
            val exports = withContext(Dispatchers.IO) {
                actionDatabase.allHabitExports()
            }
            habits = exports.map { it.record }
            habitExports = exports.associateBy { it.record.id }
            localActionCount = withContext(Dispatchers.IO) { actionDatabase.actionCount() }
            message = if (isDirty) "习惯已保存到本地，等待联网同步" else "习惯已创建"
            onCreated()
        }
    ) {
        require(name.isNotBlank()) { "习惯名称不能为空" }
        require(category == "check" || category == "value") { "习惯类别无效" }
        require(freqNum > 0 && freqDen > 0) { "习惯频率必须是正整数" }
        if (category == "value") {
            require(targetValue != null) { "数值习惯必须设置目标值" }
            require(targetType == "AtLeast" || targetType == "AtMost") { "目标类型无效" }
        }
        val now = currentRecordDateTime()
        val action = ActionRecord(
            id = UUID.randomUUID().toString().replace("-", ""),
            name = name.trim(),
            type = "habit",
            state = "doing",
            category = category,
            remark = note.trim().ifBlank { null },
            start = now,
            createTime = now
        )
        val definition = HabitDefinition(
            category = category,
            note = note.trim().ifBlank { null },
            unit = if (category == "value") unit.trim().ifBlank { null } else null,
            freqNum = freqNum,
            freqDen = freqDen,
            question = if (category == "check") question.trim().ifBlank { null } else null,
            targetValue = if (category == "value") targetValue else null,
            targetType = if (category == "value") targetType else null,
            createTime = now
        )
        val habitRecord = HabitRecord(action, definition)
        writeWithOfflineFallback(
            server = {
                val saved = api.saveHabit(habitRecord)
                val savedHabit = habitRecord.copy(action = saved, habit = definition.copy(actionId = saved.id))
                withContext(Dispatchers.IO) { actionDatabase.saveHabitRecord(savedHabit) }
                saved
            },
            offline = {
                withContext(Dispatchers.IO) { actionDatabase.markHabitDirty(habitRecord) }
                action
            }
        )
    }

    fun loadHabitDetails(action: ActionRecord) = perform {
        habitRecordDetails = null
        val local = withContext(Dispatchers.IO) { actionDatabase.actionDetails(action.id) }
        habitRecordDetails = if (local?.habit != null) {
            HabitRecord(local.record, local.habit, local.checklists)
        } else {
            api.findHabitRecord(action.id).also { details ->
                withContext(Dispatchers.IO) { actionDatabase.saveHabitRecord(details) }
            }
        }
    }

    fun saveHabitDetails(habit: HabitRecord) = perform(
        onSuccess = { (saved, isDirty, prepared) ->
            habitRecordDetails = prepared.copy(action = saved)
            habits = habits.map { if (it.id == saved.id) saved else it }
            val existing = habitExports[saved.id] ?: ActionExport(saved)
            habitExports = habitExports + (saved.id to existing.copy(
                record = saved,
                checklists = prepared.checkLists,
                habit = prepared.habit
            ))
            message = if (isDirty) "习惯修改已保存到本地，等待联网同步" else "习惯修改已保存"
        }
    ) {
        require(habit.action.name.isNotBlank()) { "习惯名称不能为空" }
        require(habit.habit.category == "check" || habit.habit.category == "value") { "习惯类别无效" }
        require((habit.habit.freqNum ?: 0) > 0 && (habit.habit.freqDen ?: 0) > 0) { "习惯频率必须是正整数" }
        if (habit.habit.category == "value") {
            require(habit.habit.targetValue != null) { "数值习惯必须设置目标值" }
            require(habit.habit.targetType == "AtLeast" || habit.habit.targetType == "AtMost") { "目标类型无效" }
        }
        val now = currentRecordDateTime()
        val prepared = habit.copy(
            action = habit.action.copy(createTime = habit.action.createTime ?: now),
            habit = habit.habit.copy(createTime = habit.habit.createTime ?: now)
        )
        val (saved, isDirty) = writeWithOfflineFallback(
            server = {
                val saved = api.saveHabit(prepared)
                val savedHabit = prepared.copy(
                    action = saved,
                    habit = prepared.habit.copy(actionId = saved.id)
                )
                withContext(Dispatchers.IO) { actionDatabase.saveHabitRecord(savedHabit) }
                saved
            },
            offline = {
                withContext(Dispatchers.IO) { actionDatabase.markHabitDirty(prepared) }
                prepared.action
            }
        )
        Triple(saved, isDirty, prepared)
    }

    fun saveHabitValue(actionId: String, value: Int) = perform(
        onSuccess = { (saved, isDirty) ->
            habitRecordDetails = habitRecordDetails?.let { current ->
                val savedDay = saved.createTime?.take(10)
                current.copy(
                    checkLists = current.checkLists.filterNot {
                        it.id == saved.id || (savedDay != null && it.createTime?.take(10) == savedDay)
                    } + saved
                )
            }
            habitExports = habitRecordDetails?.let { current ->
                habitExports + (actionId to ActionExport(
                    record = current.action,
                    checklists = current.checkLists,
                    habit = current.habit
                ))
            } ?: habitExports
            message = if (isDirty) "习惯记录已保存到本地，等待联网同步" else "习惯记录已保存"
        }
    ) {
        val details = habitRecordDetails ?: throw IllegalStateException("尚未加载习惯详情")
        if (details.habit.category == "check") {
            require(value == 0 || value == 1) { "打卡值只能是 0 或 1" }
        }
        writeWithOfflineFallback(
            server = {
                api.saveHabitValue(
                    actionId,
                    ActionCheckList(
                        actionId = actionId,
                        content = "",
                        first = 0,
                        last = 0,
                        current = value,
                        createTime = currentRecordDateTime()
                    )
                ).also { saved ->
                    withContext(Dispatchers.IO) { actionDatabase.saveActionChecklist(actionId, saved) }
                }
            },
            offline = {
                withContext(Dispatchers.IO) { actionDatabase.markHabitValueDirty(actionId, value) }
            }
        )
    }

    fun refreshRecordCategories() = perform {
        recordCategories = withContext(Dispatchers.IO) { actionDatabase.actionCategories() }
        recordLabels = withContext(Dispatchers.IO) { actionDatabase.actionLabels() }
    }

    fun searchRecordCandidates(name: String) {
        val query = name.trim()
        recordCandidateQuery = query
        if (query.isBlank()) {
            recordCandidates = emptyList()
            return
        }
        viewModelScope.launch {
            val candidates = withContext(Dispatchers.IO) {
                actionDatabase.actionCandidates(query)
            }
            if (recordCandidateQuery == query) recordCandidates = candidates
        }
    }

    fun deleteHabit(actionId: String, onDeleted: () -> Unit = {}) = perform(
        onSuccess = { isDirty ->
            habits = habits.filterNot { it.id == actionId }
            habitExports = habitExports - actionId
            habitRecordDetails = null
            localActionCount = withContext(Dispatchers.IO) { actionDatabase.actionCount() }
            refreshPendingCount()
            message = if (isDirty) "习惯删除已保存到本地，等待联网同步" else "习惯已删除"
            onDeleted()
        }
    ) {
        writeDeleteWithOfflineFallback(
            server = {
                api.deleteHabit(actionId)
                withContext(Dispatchers.IO) { actionDatabase.deleteAction(actionId) }
            },
            offline = { withContext(Dispatchers.IO) { actionDatabase.markHabitDeleted(actionId) } }
        )
    }

    fun loadRecordDetails(record: ActionRecord) = perform {
        val details = withContext(Dispatchers.IO) {
            actionDatabase.actionDetails(record.id)
                ?: throw IllegalStateException("本地找不到行动记录：${record.id}，请先同步数据")
        }
        recordComments = details.comments
        recordChecklists = details.checklists
    }

    fun createRecord(
        name: String,
        query: String,
        page: Int,
        category: String? = null,
        label: String? = null,
        copySource: ActionRecord? = null,
        onCreated: () -> Unit = {}
    ) = perform(
        onSuccess = {
            savedRecord = it.first
            refreshRecords(query, page, category, label)
            message = if (it.second) "记录已添加到本地，等待联网同步" else "记录已添加"
            onCreated()
        }
    ) {
        require(name.isNotBlank()) { "记录名称不能为空" }
        val now = currentRecordDateTime()
        val copyRecordFields = copySource?.type == "record"
        val pendingRecord = ActionRecord(
            id = UUID.randomUUID().toString().replace("-", ""),
            name = name.trim(),
            type = "record",
            category = copySource?.takeIf { copyRecordFields }?.category,
            labels = copySource?.takeIf { copyRecordFields }?.labels.orEmpty(),
            refs = copySource?.id?.let { "action:$it" },
            remark = copySource?.takeIf { copyRecordFields }?.remark,
            start = now,
            createTime = now
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

    fun saveRecord(
        record: ActionRecord,
        query: String,
        page: Int,
        category: String? = null,
        label: String? = null
    ) = perform(
        onSuccess = {
            savedRecord = it.first
            refreshRecords(query, page, category, label)
            message = if (it.second) "记录已保存到本地，等待联网同步" else "记录已保存"
        }
    ) {
        require(record.name.isNotBlank()) { "记录名称不能为空" }
        val prepared = record.copy(
            type = "record",
            labels = record.labels.orEmpty(),
            createTime = record.createTime ?: currentRecordDateTime()
        )
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
        val (saved, isDirty) = writeRecordDetailWithOfflineFallback(
            server = {
                api.addRecordComment(actionId, comment).also { saved ->
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
        onSuccess = { (saved, isDirty) ->
            recordChecklists = recordChecklists.map { if (it.id == saved.id) saved else it }
            if (recordChecklists.none { it.id == saved.id }) recordChecklists = recordChecklists + saved
            message = if (isDirty) "Checklist 已保存到本地，等待联网同步" else "Checklist 已保存"
        }
    ) {
        require(checklist.content.isNotBlank()) { "Checklist 名称不能为空" }
        require(checklist.first <= checklist.current && checklist.current <= checklist.last) {
            "当前值必须在开始和结束范围内"
        }
        val prepared = checklist.copy(
            id = checklist.id.ifBlank { UUID.randomUUID().toString().replace("-", "") },
            actionId = actionId,
            createTime = checklist.createTime ?: currentRecordDateTime()
        )
        val (saved, isDirty) = writeRecordDetailWithOfflineFallback(
            server = {
                api.saveRecordChecklist(actionId, prepared).also { saved ->
                    withContext(Dispatchers.IO) {
                        actionDatabase.saveActionChecklist(actionId, saved)
                    }
                }
            },
            offline = {
                withContext(Dispatchers.IO) {
                    actionDatabase.markActionChecklistDirty(actionId, prepared)
                }
                prepared
            }
        )
        refreshPendingCount()
        Pair(saved, isDirty)
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

    private fun <T> perform(onSuccess: suspend (T) -> Unit = {}, action: suspend () -> T) {
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
        timerJob?.cancel()
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
        category = category,
        refs = refs,
        start = start,
        end = end
    )

    private fun TodoRecord.toActionRecord() = ActionRecord(
        id = id,
        name = name,
        state = state,
        remark = remark,
        type = type,
        category = category,
        refs = refs,
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
