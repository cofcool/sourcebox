package net.cofcool.toolbox.internal.simplenote;

import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import lombok.CustomLog;
import net.cofcool.toolbox.internal.simplenote.NoteConfig.NoteCodec;
import net.cofcool.toolbox.internal.simplenote.NoteRepository.Note;
import net.cofcool.toolbox.util.JsonUtil;
import net.cofcool.toolbox.util.VertxUtils;

@CustomLog
public class NoteIndex {

    private final Vertx vertx;
    private final NoteService noteService;

    public NoteIndex(Vertx vertx) {
        this.vertx = vertx;
        this.noteService = new NoteService(vertx);
        vertx.eventBus().registerDefaultCodec(Note.class, new NoteCodec());
    }

    public Router router() {
        var router = Router.router(vertx);

        router.route("/").handler(it -> it.redirect(it.request().path() + "static/"));
        router.route("/static/*").handler(VertxUtils.webrootHandler());
        router.route().handler(VertxUtils.bodyHandler(null));

        router.get("/list").respond(context ->
            noteService.find(null).andThen(it -> Json.encodeToBuffer(it.result()))
        );

        router.post("/note").respond(context ->
            noteService.save(context.body().asPojo(Note.class))
                .andThen(it -> Json.encodeToBuffer(it.result()))
        );

        router.delete("/note/:id").handler(context ->
            noteService
                .logicDelete(new Note(context.pathParam("id"), null, null, null))
                .andThen(a -> context.response().end("ok"))
        );

        VertxUtils.uploadRoute(
            router,
            (f, r) -> {
                r.vertx().fileSystem().readFile(f.uploadedFileName(), it ->
                    noteService.save(JsonUtil.toPojoList(it.result().getBytes(), Note.class))
                );
                return "OK";
            },
            (e, f) -> log.error("Parsing note file error", e)
        );

        return router;
    }
}
