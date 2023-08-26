package net.cofcool.toolbox.runner;

import java.util.concurrent.atomic.AtomicBoolean;
import lombok.CustomLog;
import net.cofcool.toolbox.App;
import net.cofcool.toolbox.Tool;
import net.cofcool.toolbox.Tool.Args;
import net.cofcool.toolbox.Tool.RunnerType;
import net.cofcool.toolbox.ToolContext;
import net.cofcool.toolbox.ToolRunner;

@CustomLog
public class CLIRunner implements ToolRunner {

    @Override
    public boolean run(Args args) throws Exception {
        args.context(new ConsoleToolContext());
        var run = new AtomicBoolean(false);

        args.readArg("tool").ifPresent(a -> {
            for (Tool tool : App.supportTools(RunnerType.CLI)) {
                if (tool.name().name().equals(a.val())) {
                    run.set(true);
                    log.info("Start run " + tool.name().name());
                    try {
                        tool.run(args.copyConfigFrom(tool.config()));
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

    private static class ConsoleToolContext implements ToolContext {

        @Override
        public ToolContext write(Object val) {
            System.out.println(val);
            return this;
        }

        @Override
        public RunnerType runnerType() {
            return RunnerType.CLI;
        }
    }
}
