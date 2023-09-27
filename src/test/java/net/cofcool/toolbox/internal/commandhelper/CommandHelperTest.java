package net.cofcool.toolbox.internal.commandhelper;

import java.io.File;
import net.cofcool.toolbox.BaseTest;
import net.cofcool.toolbox.Tool;
import net.cofcool.toolbox.Tool.Args;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
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
        args
            .arg("filepath", new File(file, "cmd.json").getAbsolutePath())
            .arg("aliasPath", new File(file, "alias").getAbsolutePath());
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
        Assertions.assertFalse(instance.getCommandManager(null, null).findByAT(null).isEmpty());
    }

    @Test
    void runWithDel() throws Exception {
        CommandHelper instance = (CommandHelper) instance();
        instance.run(new Args().copyConfigFrom(args).arg("add", "demo --tool=cHelper #demo"));
        instance.run(new Args().copyConfigFrom(args).arg("del", "#demo"));
        Assertions.assertTrue(instance.getCommandManager(null, null).findByAT("#demo").isEmpty());
    }

    @Test
    void runWithStore() throws Exception {
        instance().run(args.arg("store", "#my"));
        Assertions.assertTrue(new File(file, "alias").exists());
    }

    @Test
    void runWithStoreALL() throws Exception {
        instance().run(args.arg("store", "ALL"));
        Assertions.assertTrue(new File(file, "alias").exists());
    }
}