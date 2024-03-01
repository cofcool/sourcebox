package net.cofcool.sourcebox.internal;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.FileSystemAccess;
import io.vertx.ext.web.handler.LoggerHandler;
import io.vertx.ext.web.handler.StaticHandler;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.CustomLog;
import net.cofcool.sourcebox.ToolName;
import net.cofcool.sourcebox.WebTool;
import net.cofcool.sourcebox.runner.WebRunner;
import net.cofcool.sourcebox.util.VertxDeployer;
import net.cofcool.sourcebox.util.VertxUtils;
import org.apache.commons.io.FilenameUtils;

@CustomLog
public class DirWebServer implements WebTool {

    @Override
    public ToolName name() {
        return ToolName.dirWebServer;
    }

    @Override
    public void run(Args args) throws Exception {
        deploy(args);
    }

    @Override
    public Future<String> deploy(Vertx vertx, Verticle verticle, Args args) {
        if (verticle == null) {
            verticle = new DirVerticle();
        }
        return WebTool.super.deploy(vertx, verticle, args).onComplete(VertxUtils.logResult(log, e -> vertx.close()));
    }

    @Override
    public Args config() {
        return new Args()
            .arg(new Arg("port", WebRunner.PORT_VAL + "", "web server listen port", false, null))
            .arg(new Arg("root", System.getProperty("user.dir"), "web server root directory", false, null))
            .alias("dir", name(), "root", null);
    }

    @Override
    public Router router(Vertx vertx) {
        return buildRouter(vertx);
    }

    private static Router buildRouter(Vertx vertx) {
        var router = Router.router(vertx);
        String path;
        try {
            path = Paths.get(VertxDeployer.getSharedArgs(ToolName.dirWebServer.name(), vertx).readArg("root").val()).toRealPath().toString();
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
        router.errorHandler(500, r -> {
            log.error("Request error", r.failure());
            r.json(new JsonObject().put("error", r.failure().getMessage()));
        });

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

    private static class DirVerticle extends AbstractVerticle {

        @Override
        public void start(Promise<Void> startPromise) throws Exception {
            VertxUtils
                .initHttpServer(
                    vertx,
                    startPromise,
                    buildRouter(vertx),
                    Integer.parseInt(VertxDeployer.getSharedArgs(ToolName.dirWebServer.name(), vertx).readArg("port").val()),
                    log
                );
        }
    }

}
