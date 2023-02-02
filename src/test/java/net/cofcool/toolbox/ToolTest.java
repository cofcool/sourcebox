package net.cofcool.toolbox;

import net.cofcool.toolbox.Tool.Arg;
import net.cofcool.toolbox.Tool.Args;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class ToolTest {

    @Test
    void args() {
        System.out.println(new Args(new String[]{"--tool=test", "--cmd=md5", "sas"}));
    }

    @Test
    void argsWithConfig() {
        Args args = new Args()
            .arg(new Arg("name", "required", "test command", true, "demo"))
            .arg(new Arg("name1", "optional", "test command", false, "demo1"));
        new Args().arg("name", null).setupConfig(args);
        Executable executable = () -> new Args().arg("name1", null).setupConfig(args);
        Assertions.assertThrows(IllegalArgumentException.class, executable);
        try {
            executable.execute();
        } catch (Throwable throwable) {
            System.out.println(throwable.getMessage());
        }
    }

}