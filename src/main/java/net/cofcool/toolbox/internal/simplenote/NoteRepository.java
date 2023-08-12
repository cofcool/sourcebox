package net.cofcool.toolbox.internal.simplenote;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NoteRepository {

    Note save(Note note);

    void save(List<Note> notes);

    void delete(String id);

    List<Note> find(Note note);

    List<Note> find();

    Optional<Note> find(String id);

    record Note(
        String id,
        String content,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime date,
        NoteState state
    ) {

        static Note init(String content) {
            return new Note(UUID.randomUUID().toString(), content, LocalDateTime.now(), NoteState.NORMAL);
        }

    }

    enum NoteState {
        NORMAL, DELETED, DELETE_FLAG, MEMORY
    }
}
