package net.cofcool.sourcebox.internal;

import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.cofcool.sourcebox.BaseTest;
import net.cofcool.sourcebox.Tool;
import net.cofcool.sourcebox.Tool.Args;
import net.cofcool.sourcebox.internal.api.entity.ActionRecord;
import net.cofcool.sourcebox.runner.WebVerticle;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

@ExtendWith(VertxExtension.class)
class BookRecordTest extends BaseTest {

    static final String PATH = "./target/";
    private static String ID = "";


    @TempDir
    Path tmpDir;

    @Override
    protected Tool instance() {
        return new BookRecord();
    }

    @BeforeAll
    static void deployVerticle(Vertx vertx, VertxTestContext testContext) throws Exception {
        var t = new BookRecord();
        var idContext = new ToDoTest.IdContext();
        var args = new Args().copyConfigFrom(t.config())
            .context(idContext);
        t.deploy(vertx, new WebVerticle(), args)
            .onSuccess(a -> {
                try {
                    t.run(args.arg("add", "book1;ebook;a test book"));
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
    @Order(value = Integer.MAX_VALUE-10)
    void list(Vertx vertx, VertxTestContext testContext) throws Exception {
        testContext.verify(() -> {
            instance().run(args.arg("find", ""));
            testContext.completeNow();
        });
    }

    @Test
    void importTest(Vertx vertx, VertxTestContext testContext) throws Exception {
        Path nested = Files.createDirectories(tmpDir.resolve("series/volume-1"));
        Path book = Files.writeString(nested.resolve("book.epub"), "book");
        Files.writeString(tmpDir.resolve(".hidden.epub"), "hidden");
        Files.writeString(Files.createDirectories(tmpDir.resolve(".private")).resolve("secret.epub"), "hidden");
        Files.createSymbolicLink(tmpDir.resolve("linked.epub"), book);

        var files = BookRecord.scanBooks(tmpDir, false, "ebook");
        Assertions.assertEquals(1, files.size());
        Assertions.assertEquals("book.epub", files.getFirst().name());
        Assertions.assertEquals("path: series/volume-1/book.epub\nsize: 0.00 KB", files.getFirst().remark());
        Assertions.assertEquals("epub", files.getFirst().labels());
        Assertions.assertEquals("ebook", files.getFirst().category());
        Assertions.assertEquals("book", files.getFirst().type());
        Assertions.assertEquals("todo", files.getFirst().state());

        var folders = BookRecord.scanBooks(tmpDir, true, "comic");
        Assertions.assertEquals(List.of("series", "volume-1"), folders.stream().map(ActionRecord::name).toList());
        Assertions.assertTrue(folders.stream().allMatch(folder -> folder.labels() == null));
        testContext.verify(() -> {
            instance().run(args.arg("syncPath", tmpDir.toString()));
            testContext.completeNow();
        });
    }
}