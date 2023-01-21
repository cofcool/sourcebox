package net.cofcool.toolbox;

import org.junit.jupiter.api.Test;

class AppTest {

    @Test
    void runWithNoName() {
        App.main(new String[]{"--name=test"});
    }

    @Test
    void run() {
        App.main(new String[]{"--name=" + ToolName.converts,  "--cmd=now"});
    }
}