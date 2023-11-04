package net.cofcool.toolbox.internal.simplenote;

import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import lombok.CustomLog;
import net.cofcool.toolbox.internal.simplenote.entity.ActionRecord;
import net.cofcool.toolbox.util.VertxUtils;

@CustomLog
public class ActionIndex {

    private final ActionService actionService;
    private final Vertx vertx;

    public ActionIndex(Vertx vertx) {
        this.actionService = new ActionService(vertx);
        this.vertx = vertx;
    }

    public Router router() {
        var router = Router.router(vertx);

        router.route().handler(VertxUtils.bodyHandler(null));

        router.get("/").respond(r -> actionService.find());

        router.post("/").respond(r -> actionService.saveAction(r.body().asPojo(ActionRecord.class)));

        return router;
    }
}
