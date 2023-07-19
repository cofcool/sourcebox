package net.cofcool.toolbox.simplenote.internal

import io.vertx.core.Future
import io.vertx.core.Vertx
import net.cofcool.toolbox.simplenote.Config
import net.cofcool.toolbox.simplenote.Note
import net.cofcool.toolbox.simplenote.NoteRepository
import net.cofcool.toolbox.simplenote.NoteState
import kotlin.io.path.Path


class NoteService(vertx: Vertx) {

    private val noteRepository: NoteRepository;

    init {
        val config = vertx.orCreateContext.config()
        val path = Path(
            config.getString(Config.PATH_KEY, Config.PATH_VAL),
            config.getString(Config.FILE_KEY, Config.FILE_NAME)
        )
            .toAbsolutePath().toString()
        noteRepository = FileRepository(path, vertx)
    }

    fun save(note: Note): Future<Note> {
        return Future.future() {
            it.complete(noteRepository.save(note))
        }
    }

    fun save(notes: List<Note>): Future<Void> {
        return Future.future() {
            noteRepository.save(notes)
            it.complete()
        }
    }

    fun delete(note: Note): Future<Void> {
        return Future.future {
            noteRepository.delete(note.id)
            it.complete()
        }
    }

    fun logicDelete(note: Note): Future<Void> {
        return Future.future {
            noteRepository.find(note.id)?.let {
                it.state = NoteState.DELETE_FLAG
                noteRepository.save(it)
            }
            it.complete()
        }
    }

    fun find(note: Note?): Future<List<Note>> {
        return Future.future {
            it.complete(
                if (note == null) {
                    noteRepository.find()
                } else {
                    noteRepository.find(note)
                }
            )
        }
    }

}
