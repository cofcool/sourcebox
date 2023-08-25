package net.cofcool.toolbox.util;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.jackson.DatabindCodec;
import net.cofcool.toolbox.Logger;

public final class VertxUtils {

    static {
        JsonUtil.enableTimeModule(DatabindCodec.mapper());
        JsonUtil.enableTimeModule(DatabindCodec.prettyMapper());
    }

    public static <T> Handler<AsyncResult<T>> logResult(Logger log) {
        return a -> {
            if (a.failed()) {
                log.error("Server run error", a.cause());
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

}
