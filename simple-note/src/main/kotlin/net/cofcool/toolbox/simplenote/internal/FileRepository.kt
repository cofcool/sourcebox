package net.cofcool.toolbox.simplenote.internal

import io.vertx.core.Vertx
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.core.json.Json
import net.cofcool.toolbox.simplenote.Config
import net.cofcool.toolbox.simplenote.Note
import net.cofcool.toolbox.simplenote.NoteRepository
import net.cofcool.toolbox.simplenote.NoteState

class FileRepository(private val path: String, private val vertx: Vertx): NoteRepository {

    private val logger = LoggerFactory.getLogger(this.javaClass)
    private var noteCache: MutableMap<String, Note> = mutableMapOf()
    private val eventBus = vertx.eventBus()

    init {
        val fileSystem = vertx.fileSystem()
        if (!fileSystem.existsBlocking(path)) {
            fileSystem.createFileBlocking(path)
        }
        logger.info("Init $path success")

        fileSystem.readFile(path) { result ->
            if (result.succeeded()) {
                if (result.result().length() > 0) {
                    val notes: List<Note> = Config.toPojoList(result.result(), Note::class.java)
                    noteCache = notes.associateBy {
                        it.id
                    }.toMutableMap()
                }
            }
        }
        eventBus.consumer<Note>(Config.DIRTY) {
            fileSystem.writeFile(path, Json.encodeToBuffer(noteCache.values.filter { it.state != NoteState.MEMORY || it.state != NoteState.DELETED }.toList()))
            logger.info("Update ${noteCache.size} notes cache")
        }
    }

    override fun save(note: Note): Note {
        noteCache[note.id] = note
        eventBus.publish(Config.DIRTY, note)

        return note
    }

    override fun save(notes: List<Note>) {
        noteCache.putAll(notes.associateBy { it.id })
        eventBus.publish(Config.DIRTY, notes.size)
    }

    override fun delete(id: String) {
        noteCache.remove(id)
        eventBus.publish(Config.DIRTY, id)
    }

    override fun find(note: Note): List<Note> {
        if (noteCache[note.id] != null) {
            return listOf(noteCache[note.id]!!)
        }
         if (note.date != null) {
            return find().filter { it.date!! <= note.date }
        }
        if (note.content != null) {
            return find().filter { it.content!!.contains(note.content!!) }
        }
        if (note.state != null) {
            return find().filter { it.state == note.state }
        }
        return listOf()
    }

    override fun find(id: String): Note? {
        return noteCache[id]
    }

    override fun find(): List<Note> {
        return noteCache.values.toList()
    }

}