package net.cofcool.sourcebox.internal.api;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import java.util.List;
import lombok.CustomLog;
import net.cofcool.sourcebox.internal.api.entity.Note;
import net.cofcool.sourcebox.util.SqlRepository;
import org.apache.commons.lang3.StringUtils;

@CustomLog
public class NoteService {

    private final SqlRepository<Note> noteRepository;

    public NoteService(Vertx vertx) {
        noteRepository = SqlRepository.create(Note.class);
    }

    public Future<Note> save(Note note)  {
        var n = note;
        if (StringUtils.isBlank(n.id())) {
            n = Note.init(n.content());
        }
        return noteRepository.save(n);
    }

    public Future<Void> save(List<Note> notes) {
        return noteRepository.save(notes);
    }

    Future<Boolean> delete(Note note) {
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
