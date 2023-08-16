package net.cofcool.toolbox.internal.simplenote;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
class NoteVerticleTest {

    @BeforeEach
    void deployVerticle(Vertx vertx, VertxTestContext testContext) {
        vertx.deployVerticle(new NoteVerticle(), testContext.succeeding(a -> testContext.completeNow()));
    }

    @Test
    void verticleDeployed(Vertx vertx, VertxTestContext testContext) {
        testContext.completeNow();
    }

    @Test
    void list(Vertx vertx, VertxTestContext testContext) {
        vertx.createHttpClient()
            .request(HttpMethod.GET, NoteConfig.PORT_VAL, "127.0.0.1", "/list")
            .compose(HttpClientRequest::send)
            .onComplete(testContext.succeeding(r -> testContext.verify(() -> {
                Assertions.assertEquals(200, r.statusCode());
                Assertions.assertEquals("application/json", r.getHeader("Content-Type"));
            })));
    }
}