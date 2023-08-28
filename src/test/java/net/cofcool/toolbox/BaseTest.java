package net.cofcool.toolbox;

import net.cofcool.toolbox.Tool.Args;
import net.cofcool.toolbox.runner.CLIRunner.ConsoleToolContext;
import org.junit.jupiter.api.BeforeEach;

public abstract class BaseTest {

    protected Args args;

    @BeforeEach
    public void setup() {
        args = instance().config().context(new ConsoleToolContext());
        LoggerFactory.setDebug(true);
        init();
    }

    protected abstract Tool instance();

    protected void init() {

    }

}
