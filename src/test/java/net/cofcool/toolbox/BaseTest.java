package net.cofcool.toolbox;

import net.cofcool.toolbox.Tool.Args;
import net.cofcool.toolbox.runner.CLIRunner.ConsoleToolContext;
import org.junit.jupiter.api.BeforeEach;

public abstract class BaseTest {

    protected Args args;

    @BeforeEach
    public void setup() throws Exception {
        System.setProperty("logging.debug", "true");
        args = instance().config().context(new ConsoleToolContext());
        init();
    }

    protected abstract Tool instance();

    protected void init() throws Exception {

    }

}
