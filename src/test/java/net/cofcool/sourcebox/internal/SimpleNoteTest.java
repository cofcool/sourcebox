package net.cofcool.sourcebox.internal;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.Json;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.time.LocalDateTime;
import java.util.List;
import net.cofcool.sourcebox.BaseTest;
import net.cofcool.sourcebox.Tool.Args;
import net.cofcool.sourcebox.Utils;
import net.cofcool.sourcebox.internal.simplenote.NoteConfig;
import net.cofcool.sourcebox.internal.simplenote.entity.ActionRecord;
import net.cofcool.sourcebox.internal.simplenote.entity.Note;
import net.cofcool.sourcebox.runner.CLIWebToolVerticle;
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
        note.deploy(vertx, new CLIWebToolVerticle(note), new Args().copyConfigFrom(note.config())
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

    @Test
    void listRoute(Vertx vertx, VertxTestContext testContext) {
        vertx.createHttpClient()
            .request(HttpMethod.GET, Integer.parseInt(port), "127.0.0.1", "/develop/routes")
            .compose(HttpClientRequest::send)
            .onComplete(testContext.succeeding(r -> testContext.verify(() -> {
                Assertions.assertEquals(200, r.statusCode());
                Assertions.assertEquals("application/json", r.getHeader("Content-Type"));
                r.body(res -> System.out.println(res.map(Json::decodeValue).result()));
                testContext.completeNow();
            })));
    }
}