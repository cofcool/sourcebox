package net.cofcool.toolbox.internal.simplenote;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Context;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.jackson.DatabindCodec;
import lombok.CustomLog;
import net.cofcool.toolbox.internal.simplenote.NoteConfig.NoteCodec;
import net.cofcool.toolbox.internal.simplenote.NoteRepository.Note;
import net.cofcool.toolbox.util.JsonUtil;

@CustomLog
public class NoteVerticle extends AbstractVerticle {

    public static void main(String[] args) {
        Vertx.vertx().deployVerticle(new NoteVerticle());
    }

    public static void start(String json) {
        log.debug(json);
        Vertx.vertx().deployVerticle(new NoteVerticle(), new DeploymentOptions().setConfig(new JsonObject(json)));
    }

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        var server = vertx.createHttpServer();
        server
            .requestHandler(new NoteIndex(vertx).router())
            .listen(
                context.config().getInteger(NoteConfig.PORT_KEY, NoteConfig.PORT_VAL),
                http -> {
                    if (http.succeeded()) {
                        startPromise.complete();
                        log.info(String.format("HTTP server started on port %s", http.result().actualPort()));
                    } else {
                        startPromise.fail(http.cause());
                    }
                });
        vertx.eventBus().publish(NoteConfig.STARTED, context.config());
    }

    @Override
    public void init(Vertx vertx, Context context) {
        super.init(vertx, context);
        JsonUtil.enableTimeModule(DatabindCodec.mapper());
        JsonUtil.enableTimeModule(DatabindCodec.prettyMapper());
        vertx.eventBus().registerDefaultCodec(Note.class, new NoteCodec());
    }
}
