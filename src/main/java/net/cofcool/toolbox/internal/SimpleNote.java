package net.cofcool.toolbox.internal;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import java.util.EnumSet;
import lombok.CustomLog;
import net.cofcool.toolbox.ToolName;
import net.cofcool.toolbox.WebTool;
import net.cofcool.toolbox.internal.simplenote.NoteConfig;
import net.cofcool.toolbox.internal.simplenote.NoteIndex;
import net.cofcool.toolbox.util.VertxDeployer;
import net.cofcool.toolbox.util.VertxUtils;

@CustomLog
public class SimpleNote implements WebTool {

    @Override
    public ToolName name() {
        return ToolName.note;
    }

    @Override
    public void run(Args args) throws Exception {
        deploy(args);
    }

    @Override
    public Future<String> deploy(Vertx vertx, Verticle verticle, Args args) {
        if (verticle == null) {
            verticle = new NoteVerticle();
        }
        return WebTool.super.deploy(vertx, verticle, args);
    }

    @Override
    public Args config() {
        return new Args()
            .arg(new Arg(NoteConfig.FILE_KEY, NoteConfig.FILE_NAME, "note filename", false, null))
            .arg(new Arg(NoteConfig.PATH_KEY, NoteConfig.PATH_VAL, "note file path", false, null))
            .arg(new Arg(NoteConfig.PORT_KEY, NoteConfig.PORT_VAL + "", "note web server listen port", false, null))
            .runnerTypes(EnumSet.of(RunnerType.CLI, RunnerType.WEB))
            .alias("note", name(), NoteConfig.PATH_KEY,  null);
    }

    @Override
    public Args defaultConfig(String globalDir) {
        return new Args()
            .arg(NoteConfig.PATH_KEY, globalDir);
    }

    @Override
    public Router router(Vertx vertx) {
        return new NoteIndex(vertx).router();
    }

    private static class NoteVerticle extends AbstractVerticle {

        @Override
        public void start(Promise<Void> startPromise) throws Exception {
            VertxUtils
                .initHttpServer(
                    vertx,
                    startPromise,
                    new NoteIndex(vertx).router(),
                    Integer.parseInt(VertxDeployer.getSharedArgs(ToolName.note.name(), vertx).readArg(NoteConfig.PORT_KEY).val()),
                    log
                );
        }

    }
}
