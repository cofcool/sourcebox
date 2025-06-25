package net.cofcool.sourcebox.internal.api;

import java.util.List;
import java.util.Optional;
import net.cofcool.sourcebox.internal.api.entity.Note;

public interface NoteRepository {

    Note save(Note note);

    void save(List<Note> notes);

    void delete(String id);

    List<Note> find(Note note);

    List<Note> find();

    Optional<Note> find(String id);



}
