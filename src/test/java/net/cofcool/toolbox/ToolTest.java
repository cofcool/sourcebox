package net.cofcool.toolbox;

import net.cofcool.toolbox.Tool.Args;
import org.junit.jupiter.api.Test;

class ToolTest {

    @Test
    void args() {
        System.out.println(new Args(new String[]{"--name=test", "--cmd=md5", "sas"}));
    }

}