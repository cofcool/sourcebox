package net.cofcool.toolbox;

import net.cofcool.toolbox.Tool.Args;
import org.junit.jupiter.api.BeforeEach;

public abstract class BaseTest {

    protected Args args;

    @BeforeEach
    public void setup() {
        args = instance().config();
        init();
    }

    protected abstract Tool instance();

    protected void init() {

    }

}
