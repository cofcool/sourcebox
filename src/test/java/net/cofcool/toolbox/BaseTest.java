package net.cofcool.toolbox;

import net.cofcool.toolbox.Tool.Args;
import net.cofcool.toolbox.Tool.RunnerType;
import org.junit.jupiter.api.BeforeEach;

public abstract class BaseTest {

    protected Args args;

    @BeforeEach
    public void setup() {
        args = instance().config().context(new TestToolContext());
        LoggerFactory.setDebug(true);
        init();
    }

    protected abstract Tool instance();

    protected void init() {

    }

    private static class TestToolContext implements ToolContext {

        @Override
        public ToolContext write(Object val) {
            System.out.println(val);
            return this;
        }

        @Override
        public RunnerType runnerType() {
            return RunnerType.CLI;
        }
    }

}
