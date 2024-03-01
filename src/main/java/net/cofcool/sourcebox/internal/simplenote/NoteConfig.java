package net.cofcool.sourcebox.internal.simplenote;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;
import io.vertx.core.json.Json;
import net.cofcool.sourcebox.internal.simplenote.entity.Note;
import net.cofcool.sourcebox.runner.WebRunner;

public interface NoteConfig {
    String PORT_KEY = "port";
    int PORT_VAL = WebRunner.PORT_VAL;

    String PATH_KEY = "filepath";
    String PATH_VAL = "./";

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
