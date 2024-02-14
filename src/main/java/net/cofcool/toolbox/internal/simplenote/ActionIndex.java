package net.cofcool.toolbox.internal.simplenote;

import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import java.util.Set;
import lombok.CustomLog;
import net.cofcool.toolbox.internal.simplenote.entity.ActionRecord;
import net.cofcool.toolbox.util.JsonUtil;
import net.cofcool.toolbox.util.VertxUtils;

@CustomLog
public class ActionIndex {

    private final ActionService actionService;
    private final Vertx vertx;

    public ActionIndex(Vertx vertx,NoteService noteService) {
        this.actionService = new ActionService(vertx, noteService);
        this.vertx = vertx;
    }

    public Router mountRoute(Router parentRouter) {
        var router = Router.router(vertx);

        router.route().handler(VertxUtils.bodyHandler(null));

        router.get("/example").respond(r -> actionService.example());
        router.get("/types").respond(r -> actionService.findAllType());

        router.get("/comments/:actionId").respond(r -> actionService.findComment(r.pathParam("actionId")));

        router.get("/:actionId").respond(r -> actionService.find(r.pathParam("actionId")));

        router.get("/refs/:actionId").respond(r -> actionService.findAllRefs(r.pathParam("actionId")));

        router.post("/actions").respond(r ->
            actionService.saveAll(JsonUtil.toPojoList(r.body().buffer().getBytes(), ActionRecord.class))
        );

        router.get().respond(r -> actionService.find());
        router.post().respond(r -> actionService.saveAction(r.body().asPojo(ActionRecord.class)));

        router.delete("/:actionId").respond(r -> actionService.deleteActions(Set.of(r.pathParam("actionId"))));
        router.delete("/comment/:id").respond(r -> actionService.deleteComments(Set.of(r.pathParam("id"))));

        parentRouter.route("/action/*").subRouter(router);

        return router;
    }
}
