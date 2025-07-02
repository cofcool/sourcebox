package net.cofcool.sourcebox.internal;

import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.io.File;
import java.nio.charset.StandardCharsets;
import net.cofcool.sourcebox.App;
import net.cofcool.sourcebox.BaseTest;
import net.cofcool.sourcebox.Tool;
import net.cofcool.sourcebox.Tool.Args;
import net.cofcool.sourcebox.Utils;
import net.cofcool.sourcebox.internal.api.NoteConfig;
import net.cofcool.sourcebox.runner.CLIRunner.ConsoleToolContext;
import net.cofcool.sourcebox.runner.CLIWebToolVerticle;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
class CommandHelperTest extends BaseTest {
    
    @Override
    protected Tool instance() {
        return new CommandHelper();
    }

    @BeforeAll
    static void deployVerticle(Vertx vertx, VertxTestContext testContext) throws Exception {
        var t = new CommandHelper();
        var args = new Args().copyConfigFrom(t.config())
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
            instance().run(args.arg("find", "ALL"));
            testContext.completeNow();
        });
    }

    @Test
    void runWithDel(Vertx vertx, VertxTestContext testContext) throws Exception {
        testContext.verify(() -> {
            instance().run(args.arg("del", "@demo"));
            testContext.completeNow();
        });
    }

    @Test
    void runWithStore(Vertx vertx, VertxTestContext testContext) throws Exception {
        testContext.verify(() -> {
            instance().run(args.arg("store", "#my"));
            instance().run(args.arg("store", "ALL"));
            testContext.completeNow();
        });
    }
    
    @Test
    void runWithImport(Vertx vertx, VertxTestContext testContext) throws Exception {
        testContext.verify(() -> {
            String f = App.globalCfgDir("runWithImport");
            FileUtils.writeStringToFile(new File(f), "aaa\nbbb\nccc", StandardCharsets.UTF_8);
            instance().run(args.arg("import", "local:bash:" + f));
            testContext.completeNow();
        });
    }

    @Test
    void runWithExport(Vertx vertx, VertxTestContext testContext) throws Exception {
        testContext.verify(() -> {
            String f = App.globalCfgDir("runWithExport");
            instance().run(args.arg("export", f));
            Assertions.assertTrue(new File(f).exists());
            testContext.completeNow();
        });
    }
}