package net.cofcool.toolbox.runner;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.CustomLog;
import lombok.SneakyThrows;
import net.cofcool.toolbox.App;
import net.cofcool.toolbox.Tool;
import net.cofcool.toolbox.Tool.Args;
import net.cofcool.toolbox.Tool.RunnerType;
import net.cofcool.toolbox.ToolContext;
import net.cofcool.toolbox.ToolRunner;
import org.apache.commons.io.FileUtils;

@CustomLog
public class CLIRunner implements ToolRunner {

    @Override
    public boolean run(Args args) throws Exception {
        args.context(new ConsoleToolContext());
        var run = new AtomicBoolean(false);

        args.readArg("tool").ifPresent(a -> {
            for (Tool tool : App.supportTools(RunnerType.CLI)) {
                var name = tool.name().name();
                if (name.equals(a.val())) {
                    run.set(true);
                    log.info("Start run " + name);
                    try {
                        tool.run(args.removePrefix(name).copyConfigFrom(tool.config()));
                    } catch (Throwable e) {
                        log.error(e);
                        log.info("Help");
                        log.info(tool.config().toHelpString());
                    }
                }
            }
        });

        return run.get();
    }

    public static class ConsoleToolContext implements ToolContext {

        @Override
        public ToolContext write(Object val) {
            System.out.println(val);
            return this;
        }

        @Override
        @SneakyThrows
        public ToolContext write(String name, String in) {
            if (name == null) {
                return write(in);
            }
            var file = new File(name);
            FileUtils.forceMkdirParent(file);
            FileUtils.write(file, in, StandardCharsets.UTF_8);

            return this;
        }

        @Override
        public RunnerType runnerType() {
            return RunnerType.CLI;
        }
    }
}
