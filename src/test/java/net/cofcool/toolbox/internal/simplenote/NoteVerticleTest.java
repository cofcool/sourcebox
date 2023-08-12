package net.cofcool.toolbox.internal.simplenote;

import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
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
}