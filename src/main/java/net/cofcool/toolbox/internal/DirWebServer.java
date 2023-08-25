package net.cofcool.toolbox.internal;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.file.FileSystemException;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.FileSystemAccess;
import io.vertx.ext.web.handler.LoggerHandler;
import io.vertx.ext.web.handler.StaticHandler;
import java.nio.file.Path;
import java.util.ArrayList;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import net.cofcool.toolbox.ToolName;
import net.cofcool.toolbox.WebTool;
import net.cofcool.toolbox.util.VertxUtils;
import org.apache.commons.io.FilenameUtils;

@CustomLog
public class DirWebServer implements WebTool {

    @Override
    public ToolName name() {
        return ToolName.dirWebServer;
    }

    @Override
    public void run(Args args) throws Exception {
        var port = Integer.parseInt(args.readArg("port").val());
        var rootPath = Path.of(args.readArg("root").val()).toRealPath();

        Vertx.vertx()
            .deployVerticle(new DirVerticle(port, rootPath.toString()))
            .onComplete(VertxUtils.logResult(log));
    }

    @Override
    public Args config() {
        return new Args()
            .arg(new Arg("port", "8080", "web server listen port", false, null))
            .arg(new Arg("root", System.getProperty("user.dir"), "web server root directory", false, null))
            .alias("dir", name(), "root", null);
    }

    @Override
    public Router router(Vertx vertx) {
        return router(vertx, config().readArg("root").val());
    }

    private static Router router(Vertx vertx, String path) {
        var router = Router.router(vertx);

        router.route().handler(LoggerHandler.create());
        router.get().handler(
            StaticHandler
                .create(FileSystemAccess.ROOT, path)
                .setDirectoryTemplate("webroot/vertx-web-directory.html")
                .setDirectoryListing(true)
        );
        router.errorHandler(500, r -> {
            log.error("Request error", r.failure());
            r.json(new JsonObject().put("error", r.failure().getMessage()));
        });

        router.post("/upload").handler(BodyHandler.create().setUploadsDirectory(path))
            .respond(context -> {
                var files = new ArrayList<>();
                var err = new ArrayList<>();
                context.fileUploads().forEach(e -> {
                    try {
                        var newFile = Path.of(path, e.fileName()).toString();
                        context.vertx().fileSystem().moveBlocking(e.uploadedFileName(), newFile);
                        log.debug("rename " + e.uploadedFileName() + " to " + newFile);
                        files.add(e.fileName());
                    } catch (FileSystemException ex) {
                        log.error("rename " + e.uploadedFileName(), ex);
                        err.add(ex.getMessage());
                    }
                });
                return Future.succeededFuture(JsonObject.of("result", files, "error", err));
            });

        router.get("/files").respond(r ->
            vertx.fileSystem()
                .readDir(path)
                .compose(a -> Future.succeededFuture(a.stream().map(FilenameUtils::getName).toList()))
        );

        return router;
    }

    @AllArgsConstructor
    private static class DirVerticle extends AbstractVerticle {

        private final int port;
        private final String path;

        @Override
        public void start(Promise<Void> startPromise) throws Exception {
            VertxUtils
                .initHttpServer(vertx, startPromise, router(vertx, path), port, log);
        }
    }

}
