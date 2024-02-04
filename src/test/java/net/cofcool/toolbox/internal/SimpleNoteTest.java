package net.cofcool.toolbox.internal;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.Json;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.time.LocalDateTime;
import java.util.List;
import net.cofcool.toolbox.BaseTest;
import net.cofcool.toolbox.Tool.Args;
import net.cofcool.toolbox.Utils;
import net.cofcool.toolbox.internal.simplenote.NoteConfig;
import net.cofcool.toolbox.internal.simplenote.entity.ActionRecord;
import net.cofcool.toolbox.internal.simplenote.entity.Note;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
class SimpleNoteTest extends BaseTest {

    static final String PATH = "./target/";
    static String port = Utils.randomPort();

    @BeforeAll
    static void deployVerticle(Vertx vertx, VertxTestContext testContext) throws Exception {
        var note = new SimpleNote();
        note.deploy(vertx, null, new Args().copyConfigFrom(note.config())
                .arg(NoteConfig.PATH_KEY, PATH)
                .arg(NoteConfig.PORT_KEY, port)
            )
            .onComplete(testContext.succeeding(t -> testContext.completeNow()));
    }

    @Test
    void list(Vertx vertx, VertxTestContext testContext) {
        vertx.createHttpClient()
            .request(HttpMethod.GET, Integer.parseInt(port), "127.0.0.1", "/list")
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
            .request(HttpMethod.POST, Integer.parseInt(port), "127.0.0.1", "/note")
            .compose(h -> h.send(Json.encodeToBuffer(Note.init("test content"))))
            .onComplete(testContext.succeeding(r -> testContext.verify(() -> {
                Assertions.assertEquals(200, r.statusCode());
                Assertions.assertEquals("application/json", r.getHeader("Content-Type"));
                testContext.completeNow();
            })));
    }

    @Test
    void addAction(Vertx vertx, VertxTestContext testContext) {
        var param = new ActionRecord(
            "test video",
            null,
            null,
            "mac",
            "video",
            "init",
            LocalDateTime.now(),
            null,
            null,
            5,
            List.of("first"),
            "test, demo",
            null,
            LocalDateTime.now()
        );
        vertx.createHttpClient()
            .request(HttpMethod.POST, Integer.parseInt(port), "127.0.0.1", "/action")
            .compose(h -> h.send(Json.encodeToBuffer(param)))
            .onComplete(testContext.succeeding(r -> testContext.verify(() -> {
                Assertions.assertEquals(200, r.statusCode());
                Assertions.assertEquals("application/json", r.getHeader("Content-Type"));
                testContext.completeNow();
            })));
    }

    @Test
    void addActions(Vertx vertx, VertxTestContext testContext) {
        var param = new ActionRecord(
            "test video",
            null,
            null,
            "mac",
            "video",
            "init",
            LocalDateTime.now(),
            null,
            null,
            5,
            List.of("first"),
            "test, demo",
            null,
            LocalDateTime.now()
        );
        vertx.createHttpClient()
            .request(HttpMethod.POST, Integer.parseInt(port), "127.0.0.1", "/action/actions")
            .compose(h -> h.send(Json.encodeToBuffer(List.of(param))))
            .onComplete(testContext.succeeding(r -> testContext.verify(() -> {
                Assertions.assertEquals(204, r.statusCode());
                testContext.completeNow();
            })));
    }

    @Test
    void listAction(Vertx vertx, VertxTestContext testContext) {
        vertx.createHttpClient()
            .request(HttpMethod.GET, Integer.parseInt(port), "127.0.0.1", "/action")
            .compose(HttpClientRequest::send)
            .onComplete(testContext.succeeding(r -> testContext.verify(() -> {
                Assertions.assertEquals(200, r.statusCode());
                Assertions.assertEquals("application/json", r.getHeader("Content-Type"));
                testContext.completeNow();
            })));
    }
}