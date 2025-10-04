package request

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format
import kotlinx.datetime.format.char
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

enum class Tools(val tool: Tools?, val cmd: String) {
    C(null, "converts"),
    C_MD5(C, "md5"), C_NOW(C, "now"), C_HDATE(C, "hdate"),
    HtmlDown(null, "htmlDown"),
    Helper(null, "cHelper"),
    Note(null, "note"),
    Json(null, "json"),
    Todo(null, "todo"),
    Timer(null, "timer"),
    None(null, "")
    ;

    fun toolName() = if (this.tool == null) cmd else this.tool.cmd

    companion object {
        fun from(cmd: String) : Tools = Tools.entries.find { it.toolName() == cmd } ?: None
    }
}

@Serializable
data class Params(val cmd: String, val input: String)

@Serializable
data class CommandItem(val id: String, val cmd: String, val tags: List<String>?)

@Serializable
data class RecordStatistics(val cnt: Long, val total: Long, val day: String)

@Serializable
data class TodoItem(
    val id: String,
    var name: String,
    var state: String,
    var remark: String?,
    val createTime: String = "",
    val start: String = "",
    val end: String = ""
)

@Serializable
data class RecordItem(
    val id: String,
    var name: String,
    var state: String,
    var type: String,
    var remark: String?,
    val createTime: String = "",
    val start: String = "",
    val end: String = ""
)

@Serializable
data class Action(val action: String, val tool: String, val source: String, val success: Boolean)

@Serializable
data class Note(val id: String, val content: String, @Serializable(with = LocalDateTimeSerializer::class) val date: LocalDateTime?, val state: String)

@Serializable
data class Arg(val key: String, @SerialName("val") val value: String?, val desc: String?, val required: Boolean, val demo: String?);

val formatter = LocalDateTime.Format {
    year(); char('-'); monthNumber(); char('-'); dayOfMonth()
    char(' ')
    hour()
    char(':')
    minute()
    char(':')
    second()
}

object LocalDateTimeSerializer : KSerializer<LocalDateTime> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("kotlinx.datetime.LocalDateTime", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): LocalDateTime =
        formatter.parse(decoder.decodeString())

    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        encoder.encodeString(value.format(formatter))
    }
}