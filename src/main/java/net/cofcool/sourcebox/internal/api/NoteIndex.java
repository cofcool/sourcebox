package net.cofcool.sourcebox.internal.api;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import lombok.CustomLog;
import net.cofcool.sourcebox.WebTool.WebRouter;
import net.cofcool.sourcebox.internal.api.NoteConfig.NoteCodec;
import net.cofcool.sourcebox.internal.api.entity.Note;
import net.cofcool.sourcebox.util.JsonUtil;
import net.cofcool.sourcebox.util.VertxUtils;

import java.util.List;
import java.util.Map;

@CustomLog
public class NoteIndex implements WebRouter {

    public Router buildRouter(Vertx vertx) {
        var noteService = new NoteService(vertx);
        var actionIndex = new ActionIndex(vertx, noteService);
        var searchService = LuceneSearchService.get(vertx);
        vertx.eventBus().registerDefaultCodec(Note.class, new NoteCodec());

        var router = VertxUtils.createBodyRouter(vertx);

        router.get("/list").respond(context ->
            noteService.find(Note.builder().state(Note.NoteState.NORMAL.name()).build())
                .andThen(it -> Json.encodeToBuffer(it.result()))
        );

        router.get("/exportAll").respond(context -> noteService.find());

        router.post("/note").respond(context ->
            noteService.save(context.body().asPojo(Note.class))
                .andThen(it -> Json.encodeToBuffer(it.result()))
        );

        router.delete("/:id").handler(context ->
            noteService
                .logicDelete(new Note(context.pathParam("id"), null, null, null))
                .andThen(a -> context.response().end("ok"))
        );

        VertxUtils.uploadRoute(
            router,
            (f, r) -> {
                r.vertx().fileSystem().readFile(f.uploadedFileName()).onSuccess(it ->
                    noteService.save(JsonUtil.toPojoList(it.getBytes(), Note.class))
                );
                return "OK";
            },
            (e, f) -> log.error("Parsing note file error", e)
        );

        var rootRouter = Router.router(vertx);
        rootRouter.route("/note/*").subRouter(router);
        var actionRouter = actionIndex.mountRoute(rootRouter);

        rootRouter.get("/search").respond(r -> {
            var params = r.queryParams();
            return searchService.search(params.get("q"), params.get("type"));
        });

        rootRouter.get("/develop/routes").respond(r ->
            Future.succeededFuture(Map.of(
                "note", readRouteNames(router),
                "action", readRouteNames(actionRouter)
            ))
        );

        return rootRouter;
    }

    private static List<String> readRouteNames(Router router) {
        return router.getRoutes().stream().map(Route::getName).toList();
    }

    @Override
    public Router router(Vertx vertx) {
        return buildRouter(vertx);
    }
}
