package net.cofcool.toolbox.internal;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.Json;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.io.File;
import net.cofcool.toolbox.Tool.Args;
import net.cofcool.toolbox.internal.simplenote.NoteConfig;
import net.cofcool.toolbox.internal.simplenote.NoteRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
class SimpleNoteTest {

    static final String PATH = "./target/";

    @BeforeEach
    void deployVerticle(Vertx vertx, VertxTestContext testContext) throws Exception {
        var note = new SimpleNote();
        note.deploy(vertx, null, new Args().copyConfigFrom(note.config()).arg(NoteConfig.PATH_KEY, PATH))
            .onComplete(testContext.succeeding(t -> testContext.completeNow()));
    }

    @Test
    void run(Vertx vertx, VertxTestContext testContext) throws Exception {
        testContext.verify(() -> {
            assertTrue(new File(PATH + "/" + NoteConfig.FILE_NAME).exists());
            testContext.completeNow();
        });
    }

    @Test
    void list(Vertx vertx, VertxTestContext testContext) {
        vertx.createHttpClient()
            .request(HttpMethod.GET, NoteConfig.PORT_VAL, "127.0.0.1", "/list")
            .compose(HttpClientRequest::send)
            .onComplete(testContext.succeeding(r -> testContext.verify(() -> {
                Assertions.assertEquals(200, r.statusCode());
                Assertions.assertEquals("application/json", r.getHeader("Content-Type"));
                testContext.completeNow();
            })));
    }

    @Test
    void addNote(Vertx vertx, VertxTestContext testContext) {
        vertx.createHttpClient()
            .request(HttpMethod.POST, NoteConfig.PORT_VAL, "127.0.0.1", "/note")
            .compose(h -> h.send(Json.encodeToBuffer(NoteRepository.Note.init("test content"))))
            .onComplete(testContext.succeeding(r -> testContext.verify(() -> {
                Assertions.assertEquals(200, r.statusCode());
                Assertions.assertEquals("application/json", r.getHeader("Content-Type"));
                testContext.completeNow();
            })));
    }
}