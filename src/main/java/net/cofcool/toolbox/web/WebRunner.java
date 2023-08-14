package net.cofcool.toolbox.web;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import lombok.CustomLog;
import net.cofcool.toolbox.Tool.Args;
import net.cofcool.toolbox.ToolRunner;
import net.cofcool.toolbox.internal.simplenote.NoteIndex;

@CustomLog
public class WebRunner extends AbstractVerticle implements ToolRunner {

    @Override
    public void run(Args args) throws Exception {
        Vertx.vertx().deployVerticle(this);
    }

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        var server = vertx.createHttpServer();
        server
            .requestHandler(new NoteIndex(vertx).router())
            .listen(
                8888,
                http -> {
                    if (http.succeeded()) {
                        startPromise.complete();
                        log.info(String.format("Toolbox server started on port %s", http.result().actualPort()));
                    } else {
                        startPromise.fail(http.cause());
                        log.error("Toolbox server start error", http.cause());
                    }
                });
    }
}
