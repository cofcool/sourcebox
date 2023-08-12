package net.cofcool.toolbox.internal.simplenote;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.Json;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.CustomLog;
import net.cofcool.toolbox.internal.simplenote.NoteRepository.Note;
import net.cofcool.toolbox.internal.simplenote.NoteRepository.NoteState;
import net.cofcool.toolbox.util.JsonUtil;

@CustomLog
public class NoteService {

    private final Vertx vertx;
    private final NoteRepository noteRepository;

    public NoteService(Vertx vertx) {
        this.vertx = vertx;

        var config = vertx.getOrCreateContext().config();
        var path = Path.of(
                config.getString(NoteConfig.PATH_KEY, NoteConfig.PATH_VAL),
                config.getString(NoteConfig.FILE_KEY, NoteConfig.FILE_NAME)
            )
            .toAbsolutePath().toString();
        noteRepository = new FileRepository(path, vertx);
    }

    public Future<Note> save(Note note)  {
        return Future.future(it -> {
            var n = note;
            if (n.id() == null) {
                n = Note.init(n.content());
            }
            it.complete(noteRepository.save(n));
        });
    }

    public Future<Void> save(List<Note> notes) {
        return Future.future(it -> {
            noteRepository.save(notes);
            it.complete();
        });
    }

    Future<Void> delete(Note note) {
        return Future.future(it -> {
            noteRepository.delete(note.id());
            it.complete();
        });
    }

    Future<Void> logicDelete(Note note) {
        return Future.future(it -> {
            noteRepository.find(note.id()).ifPresent(n ->
                noteRepository.save(new Note(n.id(), n.content(), n.date(), NoteState.DELETE_FLAG))
            );
            it.complete();
        });
    }

    Future<List<Note>> find(Note note) {
        return Future.future(it ->
            it.complete(note == null ? noteRepository.find() : noteRepository.find(note))
        );
    }


    private static class FileRepository implements NoteRepository {

        private final String path;
        private final Vertx vertx;
        private final Map<String, Note> noteCache = new ConcurrentHashMap<>();
        private final EventBus eventBus;

        public FileRepository(String path, Vertx vertx) {
            this.path = path;
            this.vertx = vertx;
            eventBus = vertx.eventBus();

            var fileSystem = vertx.fileSystem();
            if (!fileSystem.existsBlocking(path)) {
                fileSystem.createFileBlocking(path);
            }
            log.info(String.format("Init %s success", path));

            fileSystem.readFile(path, result -> {
                if (result.succeeded()) {
                    if (result.result().length() > 0) {
                        var notes = JsonUtil.toPojoList(result.result().getBytes(), Note.class);
                        noteCache.putAll(notes.stream().collect(Collectors.toMap(Note::id, note -> note)));
                    }
                }
            });
            eventBus.consumer(NoteConfig.DIRTY, m -> {
                fileSystem.writeFile(
                    path,
                    Json.encodeToBuffer(
                        noteCache.values().stream()
                            .filter(n -> n.state() != NoteState.MEMORY || n.state() != NoteState.DELETED)
                            .toList()
                    )
                );
                log.info(String.format("Update %s notes cache", noteCache.size()));
            });
        }

        @Override
        public Note save(Note note) {
            noteCache.put(note.id(), note);
            eventBus.publish(NoteConfig.DIRTY,  note);

            return note;
        }

        @Override
        public void save(List<Note> notes) {
            for (Note note : notes) {
                noteCache.put(note.id(),  note);
            }
            eventBus.publish(NoteConfig.DIRTY, notes.size());
        }

        @Override
        public void delete(String id) {
            noteCache.remove(id);
            eventBus.publish(NoteConfig.DIRTY, id);
        }

        @Override
        public List<Note> find(Note note) {
            if (note.id() != null) {
                return find(note.id()).map(List::of).orElse(List.of());
            }
            var stream = noteCache.values().stream();
            if (note.date() != null) {
                stream = stream.filter(it -> it.date().isAfter(note.date()));
            }
            if (note.content() != null) {
                stream = stream.filter(it -> it.content().contains(note.content()));
            }
            if (note.state() != null) {
                stream = stream.filter(it -> it.state() == note.state());
            }
            return stream.toList();
        }

        @Override
        public Optional<Note> find(String id) {
            return Optional.ofNullable(noteCache.get(id));
        }

        @Override
        public List<Note> find() {
            return List.copyOf(noteCache.values());
        }
    }

}
