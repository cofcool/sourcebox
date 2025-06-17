import kotlinx.datetime.*
import kotlinx.serialization.json.Json
import java.time.format.DateTimeFormatter

val globalJson = Json { ignoreUnknownKeys = true }

val datetimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

fun LocalDateTime.Companion.now(timeZone: TimeZone = TimeZone.currentSystemDefault()): LocalDateTime {
    return Clock.System.now().toLocalDateTime(timeZone)
}

fun LocalDateTime.formatFullStyle() : String {
    return datetimeFormatter.format(toJavaLocalDateTime())
}