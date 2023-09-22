package net.cofcool.toolbox.internal.commandhelper;

import java.io.File;
import net.cofcool.toolbox.BaseTest;
import net.cofcool.toolbox.Tool;
import net.cofcool.toolbox.Tool.Args;
import org.apache.commons.io.FilenameUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

class CommandHelperTest extends BaseTest {

    @TempDir
    File file;

    @Override
    protected Tool instance() {
        return new CommandHelper();
    }

    @Override
    protected void init() throws Exception {
        super.init();
        args.arg("filepath", FilenameUtils.concat(file.getAbsolutePath(), "cmd.json"));
        instance().run(new Args().copyConfigFrom(args).arg("add", "@demo mytool -tool=cHelper #my #help"));
    }

    @Test
    void run() throws Exception {
        instance().run(args);
    }

    @Test
    void runWithAdd() throws Exception {
        CommandHelper instance = (CommandHelper) instance();
        instance.run(args.arg("add", "mytool --tool=cHelper #my"));
        Assertions.assertFalse(instance.getCommandManager(null).findByAT(null).isEmpty());
    }

    @Test
    void runWithDel() throws Exception {
        CommandHelper instance = (CommandHelper) instance();
        instance.run(new Args().copyConfigFrom(args).arg("add", "demo --tool=cHelper #demo"));
        instance.run(new Args().copyConfigFrom(args).arg("del", "#demo"));
        Assertions.assertTrue(instance.getCommandManager(null).findByAT("#demo").isEmpty());
    }

    @Test
    @EnabledIfSystemProperty(named = "mytool.sptest", matches = "true")
    void runWithStore() throws Exception {
        instance().run(args.arg("store", "#my"));
        Assertions.assertTrue(new File(CommandManager.MY_TOOL_ALIAS).exists());
    }

    @Test
    @EnabledIfSystemProperty(named = "mytool.sptest", matches = "true")
    void runWithStoreALL() throws Exception {
        instance().run(args.arg("store", "ALL"));
        Assertions.assertTrue(new File(CommandManager.MY_TOOL_ALIAS).exists());
    }
}