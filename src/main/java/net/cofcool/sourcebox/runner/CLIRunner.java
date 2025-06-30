package net.cofcool.sourcebox.runner;

import io.vertx.core.Vertx;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.CustomLog;
import lombok.SneakyThrows;
import net.cofcool.sourcebox.App;
import net.cofcool.sourcebox.Tool.Args;
import net.cofcool.sourcebox.Tool.RunnerType;
import net.cofcool.sourcebox.ToolContext;
import net.cofcool.sourcebox.ToolRunner;
import net.cofcool.sourcebox.WebTool;
import org.apache.commons.io.FileUtils;

@CustomLog
public class CLIRunner implements ToolRunner {

    @Override
    public String help() {
        return String.join(", ", ToolRunner.ADDRESS_KEY, ToolRunner.PORT_KEY);
    }

    @Override
    public boolean run(Args args) throws Exception {
        args.context(new ConsoleToolContext());
        var run = new AtomicBoolean(false);

        args.readArg("tool").ifPresent(a -> {
            var name = a.val();
            App.getTool(name).ifPresent(tool -> {
                run.set(true);
                log.debug("Start run " + name);
                try {
                    var newArgs = args.removePrefix(name).copyConfigFrom(tool.config());
                    if (tool instanceof WebTool webTool && !ToolRunner.checkLocalAPIServer(
                        args.getArgVal(ADDRESS_KEY).orElse(null),
                        args.getArgVal(PORT_KEY).orElse(null)
                    )) {

                        var v = Vertx.vertx();
                        webTool.deploy(v, new CLIWebToolVerticle(webTool), newArgs)
                            .onComplete(r -> {
                                if (r.failed()) {
                                    log.error("Server run error", r.cause());
                                    v.close();
                                } else {
                                    log.info("Server run result is " + r.result());
                                }
                            }).toCompletionStage().toCompletableFuture().get();
                        if (webTool.supportCommand()) {
                            try {
                                webTool.run(newArgs);
                            } finally {
                                v.close();
                            }
                        }
                    } else {
                        tool.run(newArgs);
                    }
                } catch (Throwable e) {
                    args.getContext().write(e.getMessage());
                    args.getContext().write("Help");
                    args.getContext().write(tool.config().toHelpString());

                    log.error(e);
                }
            });
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
