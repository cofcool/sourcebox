package net.cofcool.sourcebox.internal.api;

import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import java.util.List;
import lombok.CustomLog;
import net.cofcool.sourcebox.App;
import net.cofcool.sourcebox.WebTool.WebRouter;
import net.cofcool.sourcebox.internal.api.entity.CommandRecord;
import net.cofcool.sourcebox.util.VertxUtils;

@CustomLog
public class CommandIndex implements WebRouter {

    private final CommandService commandService;

    public CommandIndex() {
        this.commandService = new CommandService(
            App.globalCfgDir( "commands.json"),
            App.globalCfgDir( "alias")
        );
    }

    public Router bind(Vertx vertx) {
        var router = VertxUtils.createBodyRouter(vertx);

        router.get("/quick").respond(r -> {
            var p = r.queryParams();
            String cmd = p.get("cmd");
            if (cmd != null) {
                return commandService.findByCmd(cmd);
            }

            return commandService.find(p.get("q"));
        });

        router.get("/import").respond(r -> {
            vertx.executeBlocking(commandService::importHis);
            return Future.succeededFuture(true);
        });

        router.post("/enter/:id")
            .respond(r ->  commandService.enter(r.pathParam("id")));

        router.post("/quick")
            .respond(r -> commandService.save(r.body().asPojo(CommandRecord.class).cmd()));

        router.delete("/:id").respond(r -> commandService.delete(r.pathParam("id")));

        router.get("/store/:q").respond(r -> commandService.store(r.pathParam("q")));

        router.get().respond(this::findCmd);
        router.post().respond(r -> commandService.save(r.body().asPojo(CommandRecord.class)));

        Router root = Router.router(vertx);
        root.route("/cmd/*").subRouter(router);

        return root;
    }

    private Future<List<CommandRecord>> findCmd(RoutingContext r) {
        MultiMap map = r.queryParams();
        var condition = CommandRecord
            .builder()
            .cmd(map.get("cmd"))
            .id(map.get("id"))
            .alias(map.get("alias"))
            .tags(map.getAll("tags"))
            .build();
        return commandService.find(condition);
    }

    @Override
    public Router router(Vertx vertx) {
        return bind(vertx);
    }
}
