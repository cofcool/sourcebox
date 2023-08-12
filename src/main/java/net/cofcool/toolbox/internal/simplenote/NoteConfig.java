package net.cofcool.toolbox.internal.simplenote;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;
import io.vertx.core.json.Json;
import net.cofcool.toolbox.internal.simplenote.NoteRepository.Note;

public interface NoteConfig {
    String PORT_KEY = "port";
    int PORT_VAL = 8888;

    String PATH_KEY = "filepath";
    String PATH_VAL = "/tmp";

    String FILE_KEY = "filename";
    String FILE_NAME = "notes.json";

    String NOTE_SERVICE = "NOTE_SERVICE";

    String STARTED = "STARTED";
    String LIST = "LIST";
    String SAVE = "SAVE";
    String DELETE = "DELETE";
    String DIRTY = "DIRTY";

    class NoteCodec implements MessageCodec<Note, Note> {

        @Override
        public void encodeToWire(Buffer buffer, Note note) {
            var encoded = Json.encodeToBuffer(note);
            buffer.appendInt(encoded.length());
            buffer.appendBuffer(encoded);
        }

        @Override
        public Note decodeFromWire(int pos, Buffer buffer) {
            var length = buffer.getInt(pos);
            return Json.decodeValue(buffer.slice(pos + 4, pos + 4 + length).toString(), Note.class);
        }

        @Override
        public Note transform(Note note) {
            return note;
        }

        @Override
        public String name() {
            return "notejson";
        }

        @Override
        public byte systemCodecID() {
            return -1;
        }
    }
}
