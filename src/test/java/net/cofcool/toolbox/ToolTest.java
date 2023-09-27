package net.cofcool.toolbox;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import net.cofcool.toolbox.Tool.Arg;
import net.cofcool.toolbox.Tool.Args;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.io.TempDir;

class ToolTest {

    @TempDir
    File file;

    @Test
    void toHelpString() {
        System.out.println(new Args(new String[]{"--tool=test", "--cmd=md5", "--in=asc"}).alias("md5", ToolName.converts, "cmd", "cmd=md5 --in").alias("rename", ToolName.rename, "path", null).toHelpString());
    }

    @Test
    void args() {
        Args args = new Args(new String[]{"--tool=test", "--cmd=md5 sas"});
        Assertions.assertEquals("test", args.readArg("tool").val());
        Assertions.assertEquals("md5 sas", args.readArg("cmd").val());
    }

    @Test
    void fileArgs() throws IOException {
        File cfgFile = new File(file, "mytool.cfg");
        Files.write(
            cfgFile.toPath(),
            """
            cfg1=val1
            cfg2=val2 sdfs
            """.getBytes(StandardCharsets.UTF_8)
        );
        Args args = new Args(cfgFile);
        Assertions.assertEquals("val1", args.readArg("cfg1").val());
        Assertions.assertEquals("val2 sdfs", args.readArg("cfg2").val());
    }

    @Test
    void prefixArgs() {
        Args args = new Args(new String[]{"--demo.tool=test", "--demo.cmd=md5"});
        Assertions.assertEquals("test", args.readArg("demo.tool").val());
        Assertions.assertEquals("md5", args.removePrefix("demo").readArg("cmd").val());
    }

    @Test
    void argsAlias() {
        var args = new Args(new String[]{"--md5=sas", "--md6=111"});
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
                return new Args()
                    .arg("cmd", "md5")
                    .arg("cmd1", "md6")
                    .arg("in", "sas")
                    .alias("md5", name(), "cmd", "arg", (before, arg, alias) -> {
                        before.put(alias.val(), Arg.of(alias.val(), arg.key()));
                        before.put("in", Arg.of("in", arg.val()));
                    })
                    .alias("md6", name(), "cmd1",  "arg1");
            }
        };
        args.copyAliasFrom(tool.config()).copyConfigFrom(tool.config());
        Assertions.assertEquals(ToolName.converts.name(), args.readArg("tool").val());
        Assertions.assertEquals("md5", args.readArg("cmd").val());
        Assertions.assertEquals("sas", args.readArg("in").val());
        Assertions.assertEquals("111", args.readArg("cmd1").val());
    }

    @Test
    void argsWithConfig() {
        Args args = new Args()
            .arg(new Arg("name", "required", "test command", true, "demo"))
            .arg(new Arg("name1", "optional", "test command", false, "demo1"));
        new Args().arg("name", null).copyConfigFrom(args);
        Executable executable = () -> new Args().arg("name1", null).copyConfigFrom(args);
        Assertions.assertThrows(IllegalArgumentException.class, executable);
        try {
            executable.execute();
        } catch (Throwable throwable) {
            System.out.println(throwable.getMessage());
        }
    }

}