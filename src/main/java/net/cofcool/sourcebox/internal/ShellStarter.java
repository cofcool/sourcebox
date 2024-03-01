package net.cofcool.sourcebox.internal;

import java.nio.charset.StandardCharsets;
import net.cofcool.sourcebox.Tool;
import net.cofcool.sourcebox.ToolName;
import org.apache.commons.io.IOUtils;

public class ShellStarter implements Tool {

    @Override
    public ToolName name() {
        return ToolName.shell;
    }

    @Override
    public void run(Args args) throws Exception {
        String cmd = args.readArg("cmd").val().replace("'", "");
        getLogger().info("shell command: " + cmd);
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
            getLogger().info(output);
        }
        String error = IOUtils.toString(exec.getErrorStream(), StandardCharsets.UTF_8);
        if (error != null && !error.isEmpty()) {
            getLogger().error(error);
        }
    }

    @Override
    public Args config() {
        return new Args().arg(new Arg("cmd", null, "shell command", true, "demo.sh test"));
    }
}
