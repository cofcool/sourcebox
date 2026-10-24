package net.cofcool.sourcebox.internal;

import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import net.cofcool.sourcebox.BaseTest;
import net.cofcool.sourcebox.Tool;
import net.cofcool.sourcebox.Tool.Args;
import net.cofcool.sourcebox.ToolContext;
import net.cofcool.sourcebox.runner.CLIWebToolVerticle;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@ExtendWith(VertxExtension.class)
class ToDoTest extends BaseTest {

    static final String PATH = "./target/";
    private static String ID = "";


    @TempDir
    File tmpDir;

    @Override
    protected Tool instance() {
        return new ToDo();
    }

    @BeforeAll
    static void deployVerticle(Vertx vertx, VertxTestContext testContext) throws Exception {
        var t = new ToDo();
        var idContext = new IdContext();
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
            instance().run(args);
            instance().run(args.arg("find", "state=todo"));
            testContext.completeNow();
        });
    }

    @Test
    void done(Vertx vertx, VertxTestContext testContext) throws Exception {
        testContext.verify(() -> {
            instance().run(args.arg("done", ID));
            testContext.completeNow();
        });
    }

    @Test
    void add(Vertx vertx, VertxTestContext testContext) throws Exception {
        InputStream originalIn = System.in;
        testContext.verify(() -> {
            String input = "buy something\napple,pear and so on\n";
            System.setIn(new ByteArrayInputStream(input.getBytes()));
            try {
                instance().run(args.arg("add", ""));
            } finally {
                System.setIn(originalIn);
            }
            testContext.completeNow();
        });
    }

    @Test
    void addLink(Vertx vertx, VertxTestContext testContext) throws Exception {
        testContext.verify(() -> {
            instance().run(args.arg("add", "https://baidu.com"));
            instance().run(args.arg("find", ""));
            testContext.completeNow();
        });
    }

    @Test
    void cancel(Vertx vertx, VertxTestContext testContext) throws Exception {
        testContext.verify(() -> {
            instance().run(args.arg("cancel", ID));
            testContext.completeNow();
        });
    }

    @Test
    void importTest(Vertx vertx, VertxTestContext testContext) throws Exception {
        testContext.verify(() -> {
            var p = FilenameUtils.concat(tmpDir.getAbsolutePath(), "importTest.csv");
            FileUtils.write(
                new File(p),
                """
                    test1,done
                    test2,todo
                    test3
                    test3,test3 remark,todo
                    "test4,com",test4 xxxx
                    """,
                StandardCharsets.UTF_8);
            instance().run(args.arg("import", p));
            testContext.completeNow();
        });
    }

    static class IdContext implements ToolContext {

        private final List<String> messages = new ArrayList<>();

        @Override
        public ToolContext write(Object value) {
            messages.add(String.valueOf(value));
            return this;
        }

        @Override
        public ToolContext write(String name, String value) {
            messages.add(value);
            return this;
        }

        @Override
        public Tool.RunnerType runnerType() {
            return Tool.RunnerType.CLI;
        }

        public String id() {
            return messages.get(0);
        }
    }
}