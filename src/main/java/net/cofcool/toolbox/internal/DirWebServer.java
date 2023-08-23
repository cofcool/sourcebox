package net.cofcool.toolbox.internal;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.FileSystemAccess;
import io.vertx.ext.web.handler.LoggerHandler;
import io.vertx.ext.web.handler.StaticHandler;
import java.nio.file.Path;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import net.cofcool.toolbox.ToolName;
import net.cofcool.toolbox.WebTool;
import net.cofcool.toolbox.util.VertxUtils;

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
                context.fileUploads().forEach(e ->
                    context.vertx().fileSystem().move(e.uploadedFileName(), e.fileName(), a -> {
                        if (a.failed()) {
                            log.error("rename " + e.uploadedFileName(), a.cause());
                        } else {
                            log.debug("rename " + e.uploadedFileName() + " to " + e.fileName());
                        }
                    })
                );
                return Future.succeededFuture(JsonObject.of("result", "ok"));
            });
        return router;
    }

    @AllArgsConstructor
    private static class DirVerticle extends AbstractVerticle {

        private final int port;
        private final String path;

        @Override
        public void start(Promise<Void> startPromise) throws Exception {
            vertx.createHttpServer()
                .requestHandler(router(vertx, path))
                .exceptionHandler(e -> log.error("Dir server socket error", e))
                .listen(
                    port,
                    http -> {
                        if (http.succeeded()) {
                            startPromise.complete();
                            log.info(String.format("Dir server started on port %s", http.result().actualPort()));
                        } else {
                            log.error("Dir server start error", http.cause());
                            startPromise.fail(http.cause());
                        }
                    });
        }
    }

}
