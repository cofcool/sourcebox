package net.cofcool.toolbox.internal.simplenote;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import lombok.CustomLog;
import net.cofcool.toolbox.util.VertxUtils;

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
        VertxUtils
            .initHttpServer(
                vertx,
                startPromise,
                new NoteIndex(vertx).router(),
                context.config().getInteger(NoteConfig.PORT_KEY, NoteConfig.PORT_VAL),
                log
            );
        vertx.eventBus().publish(NoteConfig.STARTED, context.config());
    }

}
