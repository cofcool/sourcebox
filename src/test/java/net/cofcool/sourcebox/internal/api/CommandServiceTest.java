package net.cofcool.sourcebox.internal.api;

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
import net.cofcool.sourcebox.internal.api.CommandService.ImportParam;
import net.cofcool.sourcebox.internal.api.entity.CommandRecord;
import net.cofcool.sourcebox.logging.LoggerFactory;
import net.cofcool.sourcebox.util.JsonUtil;
import net.cofcool.sourcebox.util.SqlRepository;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
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
    void importHisProcessor(Vertx vertx, VertxTestContext testContext) {
        commandManager.importHis(new ImportParam("zsh", "1716081742:0;brew install cmake\n1716083182:0;./build.sh\n1716108706:0;hdiutil -h", "test"))
            .onComplete(testContext.succeeding(r -> testContext.verify(testContext::completeNow)));
    }

    @Test
    void importHisJson(Vertx vertx, VertxTestContext testContext) {
        commandManager.importHis(new ImportParam("jsonline",
                JsonUtil.toJson(CommandRecord.builder().cmd("ls -al").alias("@lstest").frequency(0).tags(List.of("test")).build()),
                "test")
            )
            .onComplete(testContext.succeeding(r -> testContext.verify(testContext::completeNow)));
    }

    @Test
    void exportHis(Vertx vertx, VertxTestContext testContext) {
        commandManager.exportHis()
            .onComplete(testContext.succeeding(r -> testContext.verify(() -> {
                assertFalse(r.isEmpty());
                testContext.completeNow();
            })));
    }
}