package net.cofcool.toolbox.util;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.file.FileSystemException;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.jackson.DatabindCodec;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import java.util.ArrayList;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import net.cofcool.toolbox.Logger;
import org.apache.commons.io.FilenameUtils;

public final class VertxUtils {

    public static final String GLOBAL_UPLOAD_DIR = System.getProperty("upload.dir", BodyHandler.DEFAULT_UPLOADS_DIRECTORY);

    static {
        JsonUtil.enableTimeModule(DatabindCodec.mapper());
        JsonUtil.enableTimeModule(DatabindCodec.prettyMapper());
    }

    public static String resourcePath(String name) {
        return FilenameUtils.concat(GLOBAL_UPLOAD_DIR, name);
    }

    public static <T> Handler<AsyncResult<T>> logResult(Logger log) {
        return logResult(log, null);
    }

    public static <T> Handler<AsyncResult<T>> logResult(Logger log, Handler<Throwable> handler) {
        return a -> {
            if (a.failed()) {
                log.error("Server run error", a.cause());
                if (handler != null) {
                    handler.handle(a.cause());
                }
            } else {
                log.info("Server run result is " + a.result());
            }
        };
    }

    public static HttpServer initHttpServer(Vertx vertx, Promise<Void> startPromise, Handler<HttpServerRequest> handler, int port, Logger log) {
        return vertx.createHttpServer()
            .requestHandler(handler)
            .exceptionHandler(e -> log.error("HTTP server socket error", e))
            .listen(
                port,
                http -> {
                    if (http.succeeded()) {
                        startPromise.complete();
                        log.info(String.format("HTTP server started on port %s",
                            http.result().actualPort()));
                    } else {
                        log.error("HTTP server start error", http.cause());
                        startPromise.fail(http.cause());
                    }
                }
            );
    }

    public static BodyHandler bodyHandler(String persistencePath) {
        return BodyHandler
            .create()
            .setUploadsDirectory(persistencePath != null ? persistencePath : GLOBAL_UPLOAD_DIR);
    }

    public static Route uploadRoute(Router router, BiFunction<FileUpload, RoutingContext, String> fileHandler, BiConsumer<Exception, FileUpload> errorHandler) {
        return uploadRoute(router, null, fileHandler, errorHandler);
    }

    public static Route uploadRoute(Router router, String persistencePath, BiFunction<FileUpload, RoutingContext, String> fileHandler, BiConsumer<Exception, FileUpload> errorHandler) {
        var route = router.post("/upload");

        return route
            .handler(bodyHandler(persistencePath))
            .respond(context -> {
                var files = new ArrayList<>();
                var err = new ArrayList<>();
                context.fileUploads().forEach(e -> {
                    try {
                        files.add(fileHandler.apply(e, context));
                    } catch (FileSystemException ex) {
                        if (errorHandler != null) {
                            errorHandler.accept(ex, e);
                        }
                        err.add(ex.getMessage());
                    }
                });
                return Future.succeededFuture(JsonObject.of("result", files, "error", err));
            });
    }

}
