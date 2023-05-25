package net.cofcool.toolbox;

import net.cofcool.toolbox.Tool.Arg;
import net.cofcool.toolbox.Tool.Args;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class ToolTest {

    @Test
    void toHelpString() {
        System.out.println(new Args(new String[]{"--tool=test", "--cmd=md5 = dd", "sas"}).alias("md5", ToolName.converts, "cmd").toHelpString());
    }

    @Test
    void args() {
        Args args = new Args(new String[]{"--tool=test", "--cmd=md5", "sas"});
        Assertions.assertEquals("test", args.readArg("tool").val());
        Assertions.assertEquals("md5 sas", args.readArg("cmd").val());
    }

    @Test
    void argsAlias() {
        var args = new Args(new String[]{"--md5=sas"});
        var tool = new Tool() {
            @Override
            public ToolName name() {
                return ToolName.converts;
            }

            @Override
            public void run(Args args) throws Exception {
            }

            @Override
            public Args config() {
                return new Args().arg("cmd", "md5").alias("md5", name(), "cmd");
            }
        };
        args.copyAliasFrom(tool.config()).setupConfig(tool.config());
        Assertions.assertEquals(ToolName.converts.name(), args.readArg("tool").val());
        Assertions.assertEquals("sas", args.readArg("cmd").val());
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