package net.cofcool.sourcebox.internal;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.io.PrintWriter;
import net.cofcool.sourcebox.BaseTest;
import net.cofcool.sourcebox.Tool;
import net.cofcool.sourcebox.Tool.Args;
import net.cofcool.sourcebox.internal.api.entity.ActionRecord;
import net.cofcool.sourcebox.internal.api.entity.ActionType.Type;
import net.cofcool.sourcebox.runner.CLIRunner.ConsoleToolContext;
import net.cofcool.sourcebox.runner.CLIWebToolVerticle;
import net.cofcool.sourcebox.util.Repl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

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


    @Test
    void done(Vertx vertx, VertxTestContext testContext) throws Exception {
        testContext.verify(() -> {
            var t = ActionRecord.builder().name("test").type(Type.record.name()).build();
            instance().run(args.arg("done", t.id()));
            testContext.completeNow();
        });
    }

    @Test
    void comment(Vertx vertx, VertxTestContext testContext) throws Exception {
        testContext.verify(() -> {
            var t = ActionRecord.builder().name("test").type(Type.record.name()).build();
            instance().run(args.arg("comment", t.id() + ":" + "just test comment"));
            testContext.completeNow();
        });
    }

    @Test
    @Order(Integer.MAX_VALUE)
    void del(Vertx vertx, VertxTestContext testContext) throws Exception {
        testContext.verify(() -> {
            var t = ActionRecord.builder().name("test").type(Type.record.name()).build();
            instance().run(args.arg("del", t.id()));
            testContext.completeNow();
        });
    }

    @Test
    void replFind(Vertx vertx, VertxTestContext testContext) throws Exception {
        testContext.verify(() -> {
            var t = new HisRecord();
            var r = new FakeLineReader();
            t.setRepl(new Repl(r, new PrintWriter(System.out), false));
            r.addInput("find");
            r.addInput("test");
            r.addInput("record");
            t.run(args);
            testContext.completeNow();
        });
    }
}