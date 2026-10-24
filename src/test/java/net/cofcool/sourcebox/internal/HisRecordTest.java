package net.cofcool.sourcebox.internal;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import net.cofcool.sourcebox.BaseTest;
import net.cofcool.sourcebox.Tool;
import net.cofcool.sourcebox.Tool.Args;
import net.cofcool.sourcebox.runner.CLIRunner.ConsoleToolContext;
import net.cofcool.sourcebox.runner.CLIWebToolVerticle;
import net.cofcool.sourcebox.util.Repl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.PrintWriter;

@ExtendWith(VertxExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class HisRecordTest extends BaseTest {

    static final FakeLineReader reader = new FakeLineReader();

    @Override
    protected Tool instance() {
        return new HisRecord();
    }

    @BeforeAll
    static void deployVerticle(Vertx vertx, VertxTestContext testContext) throws Exception {
        var t = new HisRecord();
        t.setRepl(new Repl(reader, new PrintWriter(System.out), false));
        var args = new Args().copyConfigFrom(t.config())
            .context(new ConsoleToolContext());
        t.deploy(vertx, new CLIWebToolVerticle(t), args)
            .compose(a -> {
                try {
                    reader.addInput("add");
                    reader.addInput("test");
                    reader.addInput("web");
                    reader.addInput("video");
                    reader.addInput("todo");
                    reader.addInput("video,funny");
                    reader.addInput("");
                    reader.addInput("just a video");
                    t.run(args);
                } catch (Exception e) {
                    testContext.failNow(e);
                }
                return Future.succeededFuture();
            })
            .onComplete(testContext.succeeding(v -> testContext.completeNow()));
    }


    @Test
    void list(Vertx vertx, VertxTestContext testContext) throws Exception {
        testContext.verify(() -> {
            instance().run(args.arg("find", ""));
            instance().run(args.arg("find", "name=test"));
            testContext.completeNow();
        });
    }
}