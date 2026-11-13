package net.cofcool.sourcebox.internal;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.FileSystemAccess;
import io.vertx.ext.web.handler.LoggerHandler;
import io.vertx.ext.web.handler.StaticHandler;
import lombok.CustomLog;
import net.cofcool.sourcebox.App;
import net.cofcool.sourcebox.ToolName;
import net.cofcool.sourcebox.ToolRunner;
import net.cofcool.sourcebox.WebTool;
import net.cofcool.sourcebox.util.VertxDeployer;
import net.cofcool.sourcebox.util.VertxUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

@CustomLog
public class DirWebServer implements WebTool {

    @Override
    public ToolName name() {
        return ToolName.dirWebServer;
    }

    @Override
    public void run(Args args) throws Exception {
        var vertx = Vertx.vertx();
        deploy(vertx, new DirVerticle());
    }

    static class DirVerticle extends AbstractVerticle {
        @Override
        public void start(Promise<Void> startPromise) throws Exception {
            VertxUtils
                .initHttpServer(
                    vertx,
                    startPromise,
                    buildRouter(vertx),
                    Integer.parseInt((String) App.getGlobalConfig(ToolRunner.PORT_KEY)),
                    log
                );
        }
    }



    @Override
    public Args config() {
        return new Args()
            .arg(new Arg("root", System.getProperty("user.dir"), "web server root directory", false, null))
            .runnerTypes(Set.of(RunnerType.CLI, RunnerType.GUI))
            .alias("dir", name(), "root", null);
    }

    private static Router buildRouter(Vertx vertx) {
        var router = Router.router(vertx);
        String path;
        try {
            path = Paths.get(VertxDeployer.getSharedArgs(vertx).readArg("root").val()).toRealPath().toString();
        } catch (IOException e) {
            throw new RuntimeException("Read path error", e);
        }

        router.route().handler(LoggerHandler.create());
        router.get().handler(
            StaticHandler
                .create(FileSystemAccess.ROOT, path)
                .setDirectoryTemplate(VertxUtils.webrootPath() + "/vertx-web-directory.html")
                .setDirectoryListing(true)
        );

        VertxUtils.uploadRoute(
            router,
            path,
            (f, r) -> {
                var newFile = Path.of(path, f.fileName()).toString();
                r.vertx().fileSystem().moveBlocking(f.uploadedFileName(), newFile);
                log.debug("rename " + f.uploadedFileName() + " to " + newFile);
                return f.fileName();
            },
            (e, f) -> log.error("rename " + f.uploadedFileName(), e)
        );

        router.get("/files").respond(r ->
            vertx.fileSystem()
                .readDir(path)
                .compose(a -> Future.succeededFuture(a.stream().map(FilenameUtils::getName).toList()))
        );

        return router;
    }
}
