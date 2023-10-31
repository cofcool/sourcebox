package net.cofcool.toolbox.internal.simplenote;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import java.util.List;
import lombok.CustomLog;
import net.cofcool.toolbox.internal.simplenote.entity.Note;
import net.cofcool.toolbox.util.SqlRepository;

@CustomLog
public class NoteService {

    private final SqlRepository<Note> noteRepository;

    public NoteService(Vertx vertx) {
        noteRepository = SqlRepository.create(vertx, Note.class);
    }

    public Future<Note> save(Note note)  {
        var n = note;
        if (n.id() == null) {
            n = Note.init(n.content());
        }
        return noteRepository.save(n);
    }

    public Future<Void> save(List<Note> notes) {
        return noteRepository.save(notes);
    }

    Future<Void> delete(Note note) {
        return noteRepository.delete(note.id());
    }

    Future<Void> logicDelete(Note note) {
        return noteRepository.find(note.id())
            .compose(n ->
                noteRepository.save(new Note(n.id(), n.content(), n.date(), Note.NoteState.DELETE_FLAG.name()))
            )
            .compose(n -> n == null ? Future.failedFuture("Update error") : Future.succeededFuture());
    }

    Future<List<Note>> find(Note note) {
        if (note == null) {
            return noteRepository.find();
        } else {
            return noteRepository.find(note);
        }
    }

}
