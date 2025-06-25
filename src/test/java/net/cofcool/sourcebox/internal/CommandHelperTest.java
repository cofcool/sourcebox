package net.cofcool.sourcebox.internal;

import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import net.cofcool.sourcebox.BaseTest;
import net.cofcool.sourcebox.Tool;
import net.cofcool.sourcebox.Tool.Args;
import net.cofcool.sourcebox.Utils;
import net.cofcool.sourcebox.internal.api.NoteConfig;
import net.cofcool.sourcebox.runner.CLIRunner.ConsoleToolContext;
import net.cofcool.sourcebox.runner.CLIWebToolVerticle;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
class CommandHelperTest extends BaseTest {

    static final String PATH = "./target/";
    static String port = Utils.randomPort();

    @Override
    protected Tool instance() {
        return new CommandHelper();
    }

    @BeforeAll
    static void deployVerticle(Vertx vertx, VertxTestContext testContext) throws Exception {
        var t = new CommandHelper();
        var args = new Args().copyConfigFrom(t.config())
            .arg(NoteConfig.PATH_KEY, PATH)
            .arg(NoteConfig.PORT_KEY, port)
            .context(new ConsoleToolContext());
        t.deploy(vertx, new CLIWebToolVerticle(t), args)
            .onSuccess(a -> {
                try {
                    t.run(args.arg("add", "@demo mytool -tool=cHelper #my #help"));
                } catch (Exception e) {
                    testContext.failNow(e);
                }
            })
            .onComplete(testContext.succeeding(v -> {
                testContext.completeNow();
            }));
    }

    @Test
    void run(Vertx vertx, VertxTestContext testContext) throws Exception {
        testContext.verify(() -> {
            instance().run(args.arg(NoteConfig.PORT_KEY, port));
            testContext.completeNow();
        });
    }

    @Test
    void runWithDel(Vertx vertx, VertxTestContext testContext) throws Exception {
        testContext.verify(() -> {
            instance().run(args.arg(NoteConfig.PORT_KEY, port).arg("del", "@demo"));
            testContext.completeNow();
        });
    }

    @Test
    void runWithStore(Vertx vertx, VertxTestContext testContext) throws Exception {
        testContext.verify(() -> {
            instance().run(args.arg(NoteConfig.PORT_KEY, port).arg("store", "#my"));
            instance().run(args.arg(NoteConfig.PORT_KEY, port).arg("store", "ALL"));
            testContext.completeNow();
        });
    }
}