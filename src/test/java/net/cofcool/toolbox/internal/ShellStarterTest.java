package net.cofcool.toolbox.internal;

import net.cofcool.toolbox.Tool.Args;
import org.junit.jupiter.api.Test;

class ShellStarterTest {

    @Test
    void run() throws Exception {
        new ShellStarter().run(new Args().arg("cmd", "git log 1.0.1..1.0.2 --oneline"));
    }
}