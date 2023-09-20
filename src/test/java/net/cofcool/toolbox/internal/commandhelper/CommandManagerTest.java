package net.cofcool.toolbox.internal.commandhelper;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
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
        commandManager = new CommandManager(FilenameUtils.concat(file.getAbsolutePath(), "cmd.json"));
    }

    @BeforeEach
    void initData() {
        commandManager.save("@mymd5 mytool --tool=converts --cmd=md5 #md5 #my");
    }

    @Test
    void save() {
        assertFalse(commandManager.findByTag("#my").isEmpty());
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
    }

    @Test
    void store() {
        commandManager.store("@mymd5");
    }

    @Test
    void delete() {
        commandManager.delete("@mymd5");
        assertTrue(commandManager.findByAlias("@mymd5").isEmpty());
    }
}