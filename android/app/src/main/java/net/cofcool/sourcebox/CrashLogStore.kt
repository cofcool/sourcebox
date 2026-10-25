package net.cofcool.sourcebox

import android.content.Context
import android.os.Process
import java.io.File
import java.io.IOException
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CrashLog(
    val file: File,
    val timestamp: Long
) {
    val name: String get() = file.name
}

class CrashLogStore(context: Context) {
    private val directory = File(context.applicationContext.filesDir, DIRECTORY_NAME)

    fun list(): List<CrashLog> {
        val files = directory.listFiles()
            ?: if (directory.exists()) throw IOException("无法读取崩溃日志目录") else return emptyList()
        return files.filter { it.isFile && it.extension == "log" }
            .sortedByDescending(File::lastModified)
            .map { CrashLog(it, it.lastModified()) }
    }

    fun read(log: CrashLog): String {
        if (log.file.parentFile != directory || !log.file.isFile) {
            throw IOException("崩溃日志不存在")
        }
        return log.file.readText(Charsets.UTF_8)
    }

    fun delete(log: CrashLog) {
        if (log.file.parentFile != directory || !log.file.delete()) {
            throw IOException("删除崩溃日志失败：${log.name}")
        }
    }

    fun deleteAll() {
        if (!directory.exists()) return
        val logs = directory.listFiles()
            ?: throw IOException("无法读取崩溃日志目录")
        logs.filter { it.isFile && it.extension == "log" }.forEach {
            if (!it.delete()) throw IOException("删除崩溃日志失败：${it.name}")
        }
    }

    fun record(thread: Thread, throwable: Throwable) {
        if (!directory.exists() && !directory.mkdirs()) {
            throw IOException("无法创建崩溃日志目录")
        }
        val stackTrace = StringWriter().also { writer ->
            throwable.printStackTrace(PrintWriter(writer))
        }.toString().take(MAX_LOG_CHARS)
        val timestamp = System.currentTimeMillis()
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date(timestamp))
        val log = File(directory, "crash-$timestamp.log")
        log.writeText(
            "Time: $date\nThread: ${thread.name}\n\n$stackTrace",
            Charsets.UTF_8
        )
        list().drop(MAX_LOG_COUNT).forEach(::delete)
    }

    companion object {
        private const val DIRECTORY_NAME = "crash-logs"
        private const val MAX_LOG_COUNT = 20
        private const val MAX_LOG_CHARS = 1_000_000

        fun install(context: Context) {
            val store = CrashLogStore(context)
            val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                try {
                    store.record(thread, throwable)
                } catch (loggingError: Exception) {
                    android.util.Log.e("CrashLogStore", "Failed to save crash log", loggingError)
                } finally {
                    if (previousHandler != null) {
                        previousHandler.uncaughtException(thread, throwable)
                    } else {
                        Process.killProcess(Process.myPid())
                        kotlin.system.exitProcess(10)
                    }
                }

            }
        }
    }
}

class SourceboxApplication : android.app.Application() {
    override fun onCreate() {
        super.onCreate()
        CrashLogStore.install(this)
    }
}
