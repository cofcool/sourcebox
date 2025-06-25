package net.cofcool.sourcebox;

import java.io.File;
import net.cofcool.sourcebox.Tool.RunnerType;
import net.cofcool.sourcebox.logging.Logger;
import net.cofcool.sourcebox.logging.LoggerFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AppTest {

    @TempDir
    static File file;

    @BeforeAll
    static void init() {
        App.GLOBAL_CFG_DIR = file.getAbsolutePath();
    }

    @Test
    void runWithNoName() throws Exception {
        App.main(new String[]{"--tool=test"});
    }

    @Test
    void run() throws Exception {
        App.main(new String[]{"--tool=" + ToolName.converts.name(),  "--cmd=now"});
    }

    @Test
    void runWithCfg() throws Exception {
        File cfg = new File(file, "mytool.cfg");
        App.main(new String[]{"--cfg=" + cfg.getAbsolutePath()});
        Assertions.assertTrue(new File(App.globalCfgDir("mytool.cfg")).exists());
    }

    @Test
    void runWithDefaultCfg() throws Exception {
        File cfg = new File(file, "mytool.cfg");
        App.main(new String[]{"--defaultConfig="});
        Assertions.assertTrue(new File(App.globalCfgDir("mytool.cfg")).exists());
    }

    @Test
    void runWithHelp() throws Exception {
        App.main(new String[]{"--tool=" + ToolName.converts.name()});
    }

    @Test
    void runWithArchive() throws Exception {
        App.main(new String[]{"--archive=true"});
        var file1 = new File(App.ZIP_FILE);
        Assertions.assertTrue(file1.exists());
        file1.delete();
    }

    @Test
    void runWithHelp1() throws Exception {
        App.main(new String[]{"--help=" + ToolName.converts.name()});
    }

    @Test
    void printAllHelp() {
        for (Tool tool : App.supportTools(RunnerType.CLI)) {
            System.out.println(tool.name());
            System.out.println(tool.config().toHelpString());
            System.out.println("-------");
        }
    }

    @Test
    void logAllHelp() {
        Logger log = LoggerFactory.getLogger(this.getClass());
        for (Tool tool : App.supportTools(RunnerType.CLI)) {
            log.info(tool.config().toHelpString());
        }
    }


}