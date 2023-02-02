package net.cofcool.toolbox;

import org.junit.jupiter.api.Test;

class AppTest {

    @Test
    void runWithNoName() {
        App.main(new String[]{"--tool=test"});
    }

    @Test
    void run() {
        App.main(new String[]{"--tool=" + ToolName.converts,  "--cmd=now"});
    }

    @Test
    void runWithHelp() {
        App.main(new String[]{"--tool=" + ToolName.converts});
    }

    @Test
    void printAllHelp() {
        for (Tool tool : App.ALL_TOOLS) {
            System.out.println(tool.name());
            System.out.println(tool.config());
            System.out.println("-------");
        }
    }
}