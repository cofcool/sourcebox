package net.cofcool.toolbox.runner;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.Json;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import net.cofcool.toolbox.Tool.Args;
import net.cofcool.toolbox.ToolName;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
class WebRunnerTest {

    @BeforeEach
    void deployVerticle(Vertx vertx, VertxTestContext testContext) throws Exception {
        System.setProperty("logging.debug", "true");
        new WebRunner().run(vertx, new Args()).onComplete(a -> testContext.succeeding(t -> testContext.completeNow()));
    }

    @Test
    void run(Vertx vertx, VertxTestContext testContext) {
        testContext.completeNow();
    }

    @Test
    void tools(Vertx vertx, VertxTestContext testContext) {
        vertx.createHttpClient()
            .request(HttpMethod.GET, 8080, "127.0.0.1", "/")
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
            .request(HttpMethod.POST, 8080, "127.0.0.1", "/" + ToolName.converts.name())
            .compose(r -> r.send(Buffer.buffer("{\"cmd\": \"now\"}")))
            .onComplete(testContext.succeeding(r -> testContext.verify(() -> {
                Assertions.assertEquals(200, r.statusCode());
                Assertions.assertEquals("application/json", r.getHeader("Content-Type"));
                r.body(res -> System.out.println(res.map(Json::decodeValue).result()));
                testContext.completeNow();
            })));
    }

    @Test
    void reqHelp(Vertx vertx, VertxTestContext testContext) {
        vertx.createHttpClient()
            .request(HttpMethod.GET, 8080, "127.0.0.1", "/help")
            .compose(HttpClientRequest::send)
            .onComplete(testContext.succeeding(r -> testContext.verify(() -> {
                Assertions.assertEquals(200, r.statusCode());
                Assertions.assertEquals("application/json", r.getHeader("Content-Type"));
                r.body(res -> System.out.println(res.map(Json::decodeValue).result()));
                testContext.completeNow();
            })));
    }
}