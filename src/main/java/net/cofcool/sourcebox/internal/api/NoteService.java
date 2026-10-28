package net.cofcool.sourcebox.internal.api;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import lombok.CustomLog;
import net.cofcool.sourcebox.internal.api.entity.Note;
import net.cofcool.sourcebox.util.RepositoryUtil;
import net.cofcool.sourcebox.util.SqlRepository;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

@CustomLog
public class NoteService {

    private final SqlRepository<Note> noteRepository;
    private final LuceneSearchService searchService;

    public NoteService(Vertx vertx) {
        noteRepository = RepositoryUtil.getRepository(Note.class);
        searchService = LuceneSearchService.get(vertx);
    }

    public Future<Note> save(Note note)  {
        var n = note;
        if (StringUtils.isBlank(n.id())) {
            n = Note.init(n.content());
        }
        return noteRepository.save(n)
            .compose(saved -> searchService.indexNote(saved).map(saved));
    }

    public Future<Void> save(List<Note> notes) {
        return noteRepository.save(notes)
            .compose(ignored -> Future.all(notes.stream().map(searchService::indexNote).toList()))
            .mapEmpty();
    }

    Future<Boolean> delete(Note note) {
        return noteRepository.delete(note.id())
            .compose(deleted -> searchService.delete("note", note.id()).map(deleted));
    }

    Future<Void> logicDelete(Note note) {
        return noteRepository.find(note.id())
            .compose(n ->
                noteRepository.save(new Note(n.id(), n.content(), n.date(), Note.NoteState.DELETE_FLAG.name()))
            )
            .compose(n -> n == null ? Future.failedFuture("Update error") : searchService.indexNote(n));
    }

    public Future<List<Note>> find() {
        return noteRepository.find();
    }

    Future<List<Note>> find(Note note) {
        if (note == null) {
            return noteRepository.find();
        } else {
            return noteRepository.find(note);
        }
    }

}
