package net.cofcool.toolbox.internal;

import java.nio.charset.StandardCharsets;
import net.cofcool.toolbox.Tool;
import net.cofcool.toolbox.ToolName;
import org.apache.commons.io.IOUtils;

public class ShellStarter implements Tool {

    @Override
    public ToolName name() {
        return ToolName.shell;
    }

    @Override
    public void run(Args args) throws Exception {
        String cmd = args.readArg("cmd").get().val().replace("'", "");
        System.out.println("shell command: " + cmd);
        Process process = Runtime
            .getRuntime()
            .exec(
                cmd.split(" "),
                null,
                null
            );
        Process exec = process.onExit().get();
        String output = IOUtils.toString(exec.getInputStream(), StandardCharsets.UTF_8);
        if (output != null && !output.isEmpty()) {
            System.out.println(output);
        }
        String error = IOUtils.toString(exec.getErrorStream(), StandardCharsets.UTF_8);
        if (error != null && !error.isEmpty()) {
            System.err.println(error);
        }
    }

    @Override
    public String help() {
        return "shell command: name=shell cmd='demo.sh test'";
    }
}
