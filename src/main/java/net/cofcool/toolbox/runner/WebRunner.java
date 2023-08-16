package net.cofcool.toolbox.runner;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import net.cofcool.toolbox.Tool.Args;
import net.cofcool.toolbox.ToolContext;
import net.cofcool.toolbox.ToolRunner;
import net.cofcool.toolbox.internal.simplenote.NoteIndex;

@CustomLog
public class WebRunner extends AbstractVerticle implements ToolRunner {

    @Override
    public boolean run(Args args) throws Exception {
        Vertx.vertx().deployVerticle(this);

        return true;
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

    @AllArgsConstructor
    private static class WebToolContext implements ToolContext {

        private final HttpServerResponse response;

        @Override
        public ToolContext write(Object val) {
            response.write(Objects.toString(val, ""));
            return this;
        }
    }
}
