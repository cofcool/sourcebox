package net.cofcool.toolbox.internal.simplenote;

import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import lombok.CustomLog;
import net.cofcool.toolbox.internal.simplenote.NoteConfig.NoteCodec;
import net.cofcool.toolbox.internal.simplenote.NoteRepository.Note;
import net.cofcool.toolbox.util.JsonUtil;

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

        router.route("/").handler(it -> it.redirect("/static/"));
        router.route("/static/*").handler(StaticHandler.create());
        router.route().handler(BodyHandler.create());

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

        router.post("/upload").handler(BodyHandler.create().setDeleteUploadedFilesOnEnd(true))
            .handler(context ->
                context.fileUploads().forEach(e ->
                    context.vertx().fileSystem().readFile(e.uploadedFileName(), it ->
                        noteService.save(JsonUtil.toPojoList(it.result().getBytes(), Note.class))
                            .andThen(a -> context.response().end("Ok"))
                    )
                )
            );

        return router;
    }
}
