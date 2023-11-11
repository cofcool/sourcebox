package net.cofcool.toolbox.runner;

import net.cofcool.toolbox.App;
import net.cofcool.toolbox.Tool.Args;
import net.cofcool.toolbox.Tool.RunnerType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

class GUIRunnerTest {

    @Test
    @EnabledIfSystemProperty(named = "gui.test", matches = "true")
    void run() throws Exception {
        App.getRunner(RunnerType.GUI).run(new Args());
    }
}