package net.cofcool.sourcebox.internal;

import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import net.cofcool.sourcebox.BaseTest;
import net.cofcool.sourcebox.Tool;
import net.cofcool.sourcebox.Tool.Args;
import net.cofcool.sourcebox.internal.api.entity.ActionRecord;
import net.cofcool.sourcebox.internal.api.entity.ActionType.Type;
import net.cofcool.sourcebox.runner.CLIRunner.ConsoleToolContext;
import net.cofcool.sourcebox.runner.CLIWebToolVerticle;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

@ExtendWith(VertxExtension.class)
class ToDoTest extends BaseTest {

    static final String PATH = "./target/";


    @TempDir
    File tmpDir;

    @Override
    protected Tool instance() {
        return new ToDo();
    }

    @BeforeAll
    static void deployVerticle(Vertx vertx, VertxTestContext testContext) throws Exception {
        var t = new ToDo();
        var args = new Args().copyConfigFrom(t.config())
            .context(new ConsoleToolContext());
        t.deploy(vertx, new CLIWebToolVerticle(t), args)
            .onSuccess(a -> {
                try {
                    t.run(args.arg("add", "buy something"));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
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
            var t = ActionRecord.builder().name("buy something").type(Type.todo.name())
                .state("todo").build();
            instance().run(args.arg("done", t.id()));
            testContext.completeNow();
        });
    }

    @Test
    void add(Vertx vertx, VertxTestContext testContext) throws Exception {
        InputStream originalIn = System.in;
        testContext.verify(() -> {
            String input = "buy something\napple,pear and so on\n";
            System.setIn(new ByteArrayInputStream(input.getBytes()));
            var t = ActionRecord.builder().name("").type(Type.todo.name())
                .state("todo").build();
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
            var t = ActionRecord.builder().name("buy something").type(Type.todo.name())
                .state("todo").build();
            instance().run(args.arg("cancel", t.id()));
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
}