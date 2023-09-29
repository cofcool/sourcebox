package net.cofcool.toolbox.internal.commandhelper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import net.cofcool.toolbox.logging.LoggerFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CommandManagerTest {

    @TempDir
    static File file;
    static CommandManager commandManager;

    @BeforeAll
    static void init() {
        LoggerFactory.setDebug(true);
        commandManager = new CommandManager(
            FilenameUtils.concat(file.getAbsolutePath(), "cmd.json"),
            FilenameUtils.concat(file.getAbsolutePath(), ".mytools")
        );
    }

    @BeforeEach
    void initData() {
        commandManager.save("@mymd5 mytool --tool=converts --cmd=md5 #md5 #my");
        commandManager.save("kafka --list --cmd=md5 #kafka");
    }

    @Test
    void save() {
        commandManager.save("@kafka --list --cmd=md5");
        assertEquals("--list --cmd=md5", commandManager.findByAlias("@kafka").get(0).cmd());
    }

    @Test
    void hasAlias() {
        assertFalse(commandManager.findByTag("#kafka").get(0).hasAlias());
    }

    @Test
    void findByTag() {
        assertFalse(commandManager.findByTag("#md5").isEmpty());
    }

    @Test
    void findByAlias() {
        assertFalse(commandManager.findByAlias("@mymd5").isEmpty());
    }

    @Test
    void findByAT() {
        assertFalse(commandManager.findByAT("@mymd5 #my #md5").isEmpty());
        assertFalse(commandManager.findByAT("ALL").isEmpty());
        assertTrue(commandManager.findByAT("xxx").isEmpty());
    }

    @Test
    void store() throws IOException {
        commandManager.store("ALL");
        var strings = FileUtils.readLines(
            new File(FilenameUtils.concat(file.getAbsolutePath(), ".mytools")),
            StandardCharsets.UTF_8
        );
        assertFalse(strings.isEmpty());

        var flag = false;
        for (String s : strings) {
            if (!flag) {
                flag = s.equals("alias mymd5='mytool --tool=converts --cmd=md5'");
            }
        }
        assertTrue(flag);
    }

    @Test
    void storeSingle() throws IOException {
        var aliasFile = new File(FilenameUtils.concat(file.getAbsolutePath(), ".mytools"));
        FileUtils.writeStringToFile(aliasFile, "alias mynow='mytool --tool=converts --cmd=now'", StandardCharsets.UTF_8);
        commandManager.store("@mymd5");
        var strings = FileUtils.readLines(
            aliasFile,
            StandardCharsets.UTF_8
        );
        assertFalse(strings.isEmpty());
        assertEquals("alias mymd5='mytool --tool=converts --cmd=md5'", strings.get(0));
        assertEquals("alias mynow='mytool --tool=converts --cmd=now'", strings.get(1));
    }

    @Test
    void delete() {
        commandManager.delete("@mymd5");
        assertTrue(commandManager.findByAlias("@mymd5").isEmpty());
    }
}