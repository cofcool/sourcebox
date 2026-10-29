package net.cofcool.sourcebox.internal;

import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import net.cofcool.sourcebox.BaseTest;
import net.cofcool.sourcebox.Tool;
import net.cofcool.sourcebox.Tool.Args;
import net.cofcool.sourcebox.runner.CLIWebToolVerticle;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class HisRecordTest extends BaseTest {

    private static String ID;

    @Override
    protected Tool instance() {
        return new HisRecord();
    }

    @BeforeAll
    static void deployVerticle(Vertx vertx, VertxTestContext testContext) throws Exception {
        var t = new HisRecord();
        var idContext = new ToDoTest.IdContext();
        var args = new Args().copyConfigFrom(t.config())
            .context(idContext);
        t.deploy(vertx, new CLIWebToolVerticle(t), args)
            .onSuccess(a -> {
                try {
                    t.run(args.arg("add", "buy something"));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                ID = idContext.id();
            })
            .onComplete(testContext.succeeding(v -> {
                testContext.completeNow();
            }));
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