package request

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format
import kotlinx.datetime.format.char
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

enum class Tools(val tool: Tools?, val cmd: String) {
    C(null, "converts"),
    C_MD5(C, "md5"), C_NOW(C, "now"), C_HDATE(C, "hdate");

}

@Serializable
data class Params(val cmd: String, val input: String)

@Serializable
data class CommandItem(val id: String, val cmd: String, val tags: List<String>?)

@Serializable
data class Action(val action: String, val tool: String, val source: String, val success: Boolean)

@Serializable
data class Note(val id: String, val content: String, @Serializable(with = LocalDateTimeSerializer::class) val date: LocalDateTime?, val state: String)

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