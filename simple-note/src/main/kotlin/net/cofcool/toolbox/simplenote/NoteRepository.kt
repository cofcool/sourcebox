package net.cofcool.toolbox.simplenote

import com.fasterxml.jackson.annotation.JsonFormat
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.MessageCodec
import io.vertx.core.json.Json
import java.time.LocalDateTime
import java.util.*


interface NoteRepository {

    fun save(note: Note): Note
    fun save(notes: List<Note>)

    fun delete(id: String)

    fun find(note: Note): List<Note>

    fun find(): List<Note>

    fun find(id: String): Note?

}

class Note {
    var id = UUID.randomUUID().toString()
    var content: String? = null
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    var date: LocalDateTime? = LocalDateTime.now()
    var state: NoteState? = NoteState.NORMAL
}

enum class NoteState {
    NORMAL, DELETED, DELETE_FLAG, MEMORY
}

class NoteCodec: MessageCodec<Note, Note> {
    override fun encodeToWire(buffer: Buffer, s: Note?) {
        val encoded: Buffer = Json.encodeToBuffer(s)
        buffer.appendInt(encoded.length())
        buffer.appendBuffer(encoded)
    }

    override fun decodeFromWire(pos: Int, buffer: Buffer): Note {
        val length = buffer.getInt(pos)
        return Json.decodeValue(buffer.slice(pos + 4, pos + 4 + length).toString(), Note::class.java)
    }

    override fun name(): String {
        return "notejson"
    }

    override fun systemCodecID(): Byte {
        return -1
    }

    override fun transform(s: Note): Note {
        return s
    }

}