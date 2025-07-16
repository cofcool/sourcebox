package net.cofcool.sourcebox.internal.api;

import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import java.util.List;
import lombok.CustomLog;
import net.cofcool.sourcebox.internal.api.entity.ActionRecord;
import net.cofcool.sourcebox.internal.api.entity.ActionType.Type;
import net.cofcool.sourcebox.internal.api.entity.Comment;
import net.cofcool.sourcebox.util.JsonUtil;
import net.cofcool.sourcebox.util.VertxUtils;

@CustomLog
public class ActionIndex {

    private final ActionService actionService;
    private final Vertx vertx;

    public ActionIndex(Vertx vertx,NoteService noteService) {
        this.actionService = new ActionService(vertx, noteService);
        this.vertx = vertx;
    }

    public Router mountRoute(Router parentRouter) {
        var router = VertxUtils.createBodyRouter(vertx);

        router.get("/example").respond(r -> actionService.example());
        router.get("/types").respond(r -> actionService.findAllType());

        router.get("/comment/:actionId").respond(r -> actionService.findComment(r.pathParam("actionId")));
        router.post("/comment/:actionId")
            .respond(r -> actionService.saveComment(r.pathParam("actionId"), r.body().asPojo(Comment.class)));
        router.delete("/comment/:id").respond(r -> actionService.deleteComments(r.pathParam("id")));

        router.get("/:actionId").respond(r -> actionService.find(r.pathParam("actionId")));

        router.get("/refs/:actionId").respond(r -> actionService.findAllRefs(r.pathParam("actionId")));

        router.post("/actions").respond(r ->
            actionService.saveAll(JsonUtil.toPojoList(r.body().buffer().getBytes(), ActionRecord.class))
        );

        router.get().respond(r -> finaAction(r, null));
        router.get("/todo").respond(r -> finaAction(r, Type.todo));
        router.get("/link").respond(r -> finaAction(r, Type.link));

        router.post().respond(r -> actionService.saveAction(r.body().asPojo(ActionRecord.class)));

        router.delete("/:actionId").respond(r -> actionService.deleteActions(r.pathParam("actionId")));

        parentRouter.route("/action/*").subRouter(router);

        return router;
    }

    private Future<List<ActionRecord>> finaAction(RoutingContext r, Type type) {
        MultiMap map = r.queryParams();
        String id = map.get("id");
        var condition = ActionRecord
            .builder()
            .type(type == null ? map.get("type") : type.name())
            .name(map.get("name"))
            .state(map.get("state"))
            .id(id == null ? ActionRecord.GENERATED : id)
            .build();
        return actionService.find(condition);
    }
}
