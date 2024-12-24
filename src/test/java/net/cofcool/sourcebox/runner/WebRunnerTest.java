package net.cofcool.sourcebox.runner;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.multipart.MultipartForm;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.Queue;
import net.cofcool.sourcebox.Tool.Args;
import net.cofcool.sourcebox.Tool.RunnerType;
import net.cofcool.sourcebox.ToolName;
import net.cofcool.sourcebox.Utils;
import net.cofcool.sourcebox.internal.JsonFormatterTest;
import net.cofcool.sourcebox.internal.JsonToPojoTest;
import net.cofcool.sourcebox.internal.TrelloToLogseqImporterTest;
import net.cofcool.sourcebox.internal.simplenote.NoteConfig;
import net.cofcool.sourcebox.runner.WebRunner.WebToolContext;
import net.cofcool.sourcebox.util.VertxUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
class WebRunnerTest {

    static String port = Utils.randomPort();

    @BeforeAll
    static void deployVerticle(Vertx vertx, VertxTestContext testContext) throws Exception {
        System.setProperty("logging.debug", "true");
        System.setProperty("upload.dir", "target/file-uploads");
        new WebVerticle(RunnerType.WEB, a -> new WebToolContext())
            .deploy(
                vertx,
                null,
                new Args()
                    .arg(ToolName.note.name() + "." + NoteConfig.PATH_KEY, "./target/")
                    .arg(WebVerticle.PORT_KEY, port)
            )
            .onComplete(testContext.succeeding(t -> testContext.completeNow()));
    }

    @Test
    void run(Vertx vertx, VertxTestContext testContext) {
        testContext.completeNow();
    }

    @Test
    void tools(Vertx vertx, VertxTestContext testContext) {
        vertx.createHttpClient()
            .request(HttpMethod.GET, Integer.parseInt(port), "127.0.0.1", "/")
            .compose(HttpClientRequest::send)
            .onComplete(testContext.succeeding(r -> testContext.verify(() -> {
                Assertions.assertEquals(200, r.statusCode());
                Assertions.assertEquals("application/json", r.getHeader("Content-Type"));
                r.body(res -> System.out.println(res.map(Json::decodeValue).result()));
                testContext.completeNow();
            })));
    }

    @Test
    void reqConverts(Vertx vertx, VertxTestContext testContext) {
        vertx.createHttpClient()
            .request(HttpMethod.POST, Integer.parseInt(port), "127.0.0.1",
                "/" + ToolName.converts.name())
            .compose(r -> r.send(Buffer.buffer("{\"cmd\": \"now\"}")))
            .onComplete(testContext.succeeding(r -> testContext.verify(() -> {
                Assertions.assertEquals(200, r.statusCode());
                Assertions.assertEquals("application/json", r.getHeader("Content-Type"));
                r.body(res -> System.out.println(res.map(Json::decodeValue).result()));
                testContext.completeNow();
            })));
    }

    @Test
    void reqJson2POJO(Vertx vertx, VertxTestContext testContext) {
        WebClient.create(vertx)
            .post(Integer.parseInt(port), "127.0.0.1", "/" + ToolName.json2POJO.name())
            .putHeader(HttpHeaders.CONTENT_TYPE.toString(), "application/json")
            .sendJson(JsonObject.of("json", JsonToPojoTest.JSON_STR).toBuffer())
            .onComplete(testContext.succeeding(r -> testContext.verify(() -> {
                queryEvents("writeFile").onSuccess(event -> {
                Assertions.assertEquals(200, r.statusCode());
                Assertions.assertEquals("application/json", r.getHeader("Content-Type"));
                Assertions.assertTrue(
                    vertx.fileSystem().existsBlocking((String) event.source()));
                testContext.completeNow();
            })
                    .onComplete(c ->testContext.completeNow());
    })));
    }

    @Test
    void reqJson(Vertx vertx, VertxTestContext testContext) {
        WebClient.create(vertx)
            .post(Integer.parseInt(port), "127.0.0.1", "/" + ToolName.json.name())
            .putHeader(HttpHeaders.CONTENT_TYPE.toString(), "application/json")
            .sendJson(JsonObject.of("json", JsonFormatterTest.JSON_STR).toBuffer())
            .onComplete(testContext.succeeding(r -> testContext.verify(() -> {
                Assertions.assertEquals(200, r.statusCode());
                Assertions.assertEquals("application/json", r.getHeader("Content-Type"));
                Assertions.assertNotNull(r.bodyAsJsonObject().getString("result"));
                testContext.completeNow();
            })));
    }

    @Test
    void reqTrelloLogseqImporter(Vertx vertx, VertxTestContext testContext) {
        WebClient.create(vertx)
            .post(Integer.parseInt(port), "127.0.0.1", "/upload")
            .sendMultipartForm(
                MultipartForm.create()
                    .textFileUpload("file", "test.txt", TrelloToLogseqImporterTest.RESOURCE_PATH,
                        "multipart/form-data"))
            .onComplete(testContext.succeeding(r -> testContext.verify(() -> {
                var name = r.bodyAsJsonObject().getJsonArray("result").getString(0);
                WebClient.create(vertx)
                    .post(Integer.parseInt(port), "127.0.0.1",
                        "/" + ToolName.trelloLogseqImporter.name())
                    .putHeader(HttpHeaders.CONTENT_TYPE.toString(), "application/json")
                    .sendJson(JsonObject.of("path", name).toBuffer())
                    .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
                        queryEvents("writeFile").onSuccess(event -> {
                                Assertions.assertEquals(200, response.statusCode());
                                Assertions.assertEquals("application/json",
                                    response.getHeader("Content-Type"));
                                Assertions.assertTrue(
                                    vertx.fileSystem().existsBlocking((String) event.source()));
                            })
                            .onComplete(c -> testContext.completeNow());
                    })));
            })));
    }

    @Test
    void reqHelp(Vertx vertx, VertxTestContext testContext) {
        vertx.createHttpClient()
            .request(HttpMethod.GET, Integer.parseInt(port), "127.0.0.1", "/help")
            .compose(HttpClientRequest::send)
            .onComplete(testContext.succeeding(r -> testContext.verify(() -> {
                Assertions.assertEquals(200, r.statusCode());
                Assertions.assertEquals("application/json", r.getHeader("Content-Type"));
                r.body(res -> System.out.println(res.map(Json::decodeValue).result()));
                testContext.completeNow();
            })));
    }

    @Test
    void reqUpload(Vertx vertx, VertxTestContext testContext) {
        WebClient.create(vertx)
            .post(Integer.parseInt(port), "127.0.0.1", "/upload")
            .sendMultipartForm(
                MultipartForm.create()
                    .textFileUpload("file", "test.txt", Buffer.buffer("demo test txt"),
                        "multipart/form-data"))
            .onComplete(testContext.succeeding(r -> testContext.verify(() -> {
                Assertions.assertEquals(200, r.statusCode());
                Assertions.assertEquals("application/json", r.getHeader("Content-Type"));
                var result = r.bodyAsJsonObject().getJsonArray("result");
                var error = r.bodyAsJsonObject().getJsonArray("error");
                System.out.println(result);
                Assertions.assertEquals(1, result.size());
                Assertions.assertEquals(0, error.size());
                testContext.completeNow();
            })));
    }


    @Test
    void reqResource(Vertx vertx, VertxTestContext testContext) {
        var path = VertxUtils.resourcePath("reqResource.txt");
        if (!vertx.fileSystem().existsBlocking(path)) {
            vertx.fileSystem().createFileBlocking(path);
        }
        WebClient.create(vertx)
            .get(Integer.parseInt(port), "127.0.0.1", "/resource/reqResource.txt")
            .send()
            .onComplete(testContext.succeeding(r -> testContext.verify(() -> {
                Assertions.assertEquals(200, r.statusCode());
                Assertions.assertNotNull(r.getHeader("content-disposition"));
                testContext.completeNow();
            })));
    }

    @Test
    void reqEvent(Vertx vertx, VertxTestContext testContext) {
        for (int i = 0; i < 10; i++) {
            WebVerticle.EVENT_QUEUE.offer(new ActionEvent("id:" + i, "", "test"));
        }
        WebClient.create(vertx)
            .get(Integer.parseInt(port), "127.0.0.1", "/event")
            .send()
            .onComplete(testContext.succeeding(r -> testContext.verify(() -> {
                Assertions.assertEquals(200, r.statusCode());
                Assertions.assertNotNull(r.bodyAsJsonArray());
                testContext.completeNow();
            })));
    }

    private Future<ActionEvent> queryEvents(String key) {
        Queue<ActionEvent> queue = WebVerticle.EVENT_QUEUE;
        var i = 0;
        while (i < 10) {
            i++;
            var e = queue.poll();
            if (e == null) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
                continue;
            }
            if (e.action().equals(key)) {
                return Future.succeededFuture(e);
            }
        }
        return Future.failedFuture(new IllegalStateException("no events found"));
    }

}