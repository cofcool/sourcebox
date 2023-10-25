package net.cofcool.toolbox.runner;

import net.cofcool.toolbox.Tool.Args;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

class GUIRunnerTest {

    @Test
    @EnabledIfSystemProperty(named = "gui.test", matches = "true")
    void run() throws Exception {
        new GUIRunner().run(new Args());
    }
}