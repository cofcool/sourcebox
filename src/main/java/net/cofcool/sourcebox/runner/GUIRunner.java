package net.cofcool.sourcebox.runner;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import java.util.concurrent.TimeUnit;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import net.cofcool.sourcebox.Tool;
import net.cofcool.sourcebox.Tool.Args;
import net.cofcool.sourcebox.Tool.RunnerType;
import net.cofcool.sourcebox.ToolContext;
import net.cofcool.sourcebox.ToolRunner;
import net.cofcool.sourcebox.runner.CLIRunner.ConsoleToolContext;
import net.cofcool.sourcebox.util.VertxUtils;

@CustomLog
public class GUIRunner implements ToolRunner {

    @Override
    public boolean run(Args args) throws Exception {
        Vertx v = Vertx.vertx(
            new VertxOptions()
                .setWarningExceptionTimeUnit(TimeUnit.SECONDS)
                .setWarningExceptionTime(120)
        );
        new WebVerticle(RunnerType.GUI, GUIContext::new).deploy(v, null, args)
            .onComplete(VertxUtils.logResult(log, e -> v.close()));
        return true;
    }

    @RequiredArgsConstructor
    private static class GUIContext extends ConsoleToolContext {
        private final Tool tool;

        @Override
        public RunnerType runnerType() {
            return RunnerType.GUI;
        }

        @Override
        public ToolContext write(Object val) {
            super.write(val);
            WebVerticle.EVENT_QUEUE.offer(new ActionEvent(val, tool.name().name(), "write"));
            return this;
        }

        @Override
        public ToolContext write(String name, String in) {
             super.write(name, in);
            WebVerticle.EVENT_QUEUE.offer(new ActionEvent(name, tool.name().name(), "writeFile"));
             return this;
        }

        @Override
        public Object toObject() {
            return JsonObject.of("status", true);
        }

        @Override
        public Tool owner() {
            return tool;
        }
    }
}
