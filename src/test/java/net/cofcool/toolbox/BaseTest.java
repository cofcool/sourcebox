package net.cofcool.toolbox;

import java.io.File;
import net.cofcool.toolbox.Tool.Args;
import net.cofcool.toolbox.logging.LoggerFactory;
import net.cofcool.toolbox.runner.CLIRunner.ConsoleToolContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

public abstract class BaseTest {

    protected Args args;

    @TempDir
    static File gTmpDir;

    @BeforeAll
    static void config() {
        App.GLOBAL_CFG_DIR = gTmpDir.getAbsolutePath();
    }

    @BeforeEach
    public void setup() throws Exception {
        System.setProperty("logging.debug", "true");
        LoggerFactory.setDebug(true);
        var tool = instance();
        if (tool != null) {
            args = instance().config().context(new ConsoleToolContext());
        }
        init();
    }

    protected Tool instance() {
        return null;
    }

    protected void init() throws Exception {

    }

}
