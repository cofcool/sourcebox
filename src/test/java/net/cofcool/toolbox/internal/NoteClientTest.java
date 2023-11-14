package net.cofcool.toolbox.internal;

import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import net.cofcool.toolbox.BaseTest;
import net.cofcool.toolbox.Tool;
import net.cofcool.toolbox.Tool.Args;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
class NoteClientTest extends BaseTest {

    @BeforeAll
    static void deployVerticle(Vertx vertx, VertxTestContext testContext) throws Exception {
        var note = new SimpleNote();
        note.deploy(vertx, null, new Args().copyConfigFrom(note.config()))
            .onComplete(testContext.succeeding(t -> testContext.completeNow()));
    }

    @Override
    protected Tool instance() {
        return new NoteClient();
    }

    @Test
    void run() throws Exception {
        instance().run(args.arg("action", "/list"));
    }
}