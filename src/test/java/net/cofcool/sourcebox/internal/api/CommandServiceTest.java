package net.cofcool.sourcebox.internal.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import net.cofcool.sourcebox.BaseTest;
import net.cofcool.sourcebox.internal.api.CommandService.HistoryProcessor;
import net.cofcool.sourcebox.logging.LoggerFactory;
import net.cofcool.sourcebox.util.SqlRepository;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

@ExtendWith(VertxExtension.class)
class CommandServiceTest extends BaseTest {

    @TempDir
    static File file;
    static CommandService commandManager;

    @BeforeAll
    static void setup(Vertx vertx, VertxTestContext testContext) {
        LoggerFactory.setDebug(true);
        SqlRepository.init(vertx);
        commandManager = new CommandService(
            FilenameUtils.concat(file.getAbsolutePath(), "cmd.json"),
            FilenameUtils.concat(file.getAbsolutePath(), ".mytools")
        );
        commandManager
            .save("@mymd5 mytool --tool=converts --cmd=md5 #md5 #my")
            .compose(i -> commandManager.save("kafka --list --cmd=md5 #kafka"))
            .onComplete(testContext.succeeding(r -> {
                testContext.verify(testContext::completeNow);
            }));
    }

    @Test
    void hasAlias(Vertx vertx, VertxTestContext testContext) {
        commandManager
            .find("#kafka")
            .onComplete(testContext.succeeding(r -> testContext.verify(() -> {
                assertFalse(r.isEmpty());
                Assertions.assertFalse(r.getFirst().hasAlias());
                testContext.completeNow();
            })));
    }

    @Test
    void findByTag(Vertx vertx, VertxTestContext testContext) {
        commandManager
            .find("#md5")
            .onComplete(testContext.succeeding(r -> testContext.verify(() -> {
                Assertions.assertFalse(r.isEmpty());
                testContext.completeNow();
            })));
    }

    @Test
    void findByAlias(Vertx vertx, VertxTestContext testContext) {
        commandManager
            .find("@mymd5")
            .onComplete(testContext.succeeding(r -> testContext.verify(() -> {
                Assertions.assertFalse(r.isEmpty());
                testContext.completeNow();
            })));
    }

    @Test
    void store(Vertx vertx, VertxTestContext testContext) throws IOException {
        commandManager.store("ALL").onComplete(testContext.succeeding(r -> testContext.verify(() -> {
            assertTrue(r);
            List<String> strings;
            try {
                strings = FileUtils.readLines(
                    new File(FilenameUtils.concat(file.getAbsolutePath(), ".mytools")),
                    StandardCharsets.UTF_8
                );
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            assertFalse(strings.isEmpty());

            var flag = false;
            for (String s : strings) {
                if (!flag) {
                    flag = s.equals("alias mymd5='mytool --tool=converts --cmd=md5'");
                }
            }
            assertTrue(flag);
            testContext.completeNow();
        })));

    }

    @Test
    void storeSingle(Vertx vertx, VertxTestContext testContext) throws IOException {
        var aliasFile = new File(FilenameUtils.concat(file.getAbsolutePath(), ".mytools"));
        FileUtils.writeStringToFile(aliasFile, "alias mynow='mytool --tool=converts --cmd=now'", StandardCharsets.UTF_8);
        commandManager
            .store("@mymd5")
            .onComplete(testContext.succeeding(r -> testContext.verify(() -> {
                var strings = FileUtils.readLines(
                    aliasFile,
                    StandardCharsets.UTF_8
                );
                assertFalse(strings.isEmpty());
                assertEquals("alias mymd5='mytool --tool=converts --cmd=md5'", strings.get(0));
                assertEquals("alias mynow='mytool --tool=converts --cmd=now'", strings.get(1));
                testContext.completeNow();
            })));
    }

    @Test
    void delete(Vertx vertx, VertxTestContext testContext) {
        commandManager.delete("@mymd5")
            .compose(i -> commandManager.find("@mymd5"))
            .onComplete(testContext.succeeding(r -> testContext.verify(() -> {
                assertTrue(r.isEmpty());
            })))
            .compose(i -> commandManager
                .save("@mymd5 mytool --tool=converts --cmd=md5 #md5 #my"))
            .onComplete(i -> testContext.completeNow());
    }

    @Test
    void findByCmd(Vertx vertx, VertxTestContext testContext) {
        commandManager
            .findByCmd("kafka")
            .onComplete(testContext.succeeding(r -> testContext.verify(() -> {
                Assertions.assertFalse(r.isEmpty());
                testContext.completeNow();
            })));
    }

    @Test
    void enter(Vertx vertx, VertxTestContext testContext) {
        commandManager
            .enter("@mymd5")
            .onComplete(testContext.succeeding(r -> testContext.verify(() -> {
                Assertions.assertTrue(r.frequency() > 0);
                testContext.completeNow();
            })));
    }

    @Test
    void hisProcessor(Vertx vertx, VertxTestContext testContext) {
        HistoryProcessor
            .process()
            .onComplete(testContext.succeeding(r -> testContext.verify(() -> {
                Assertions.assertFalse(r.isEmpty());
                testContext.completeNow();
            })));
    }

    @Test
    @Disabled
    void importHisProcessor(Vertx vertx, VertxTestContext testContext) {
        commandManager.importHis()
            .onComplete(testContext.succeeding(r -> testContext.verify(testContext::completeNow)));
    }
}