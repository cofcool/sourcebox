package net.cofcool.toolbox.internal.simplenote;

import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import lombok.CustomLog;
import net.cofcool.toolbox.internal.simplenote.NoteRepository.Note;
import net.cofcool.toolbox.util.JsonUtil;

@CustomLog
public class NoteIndex {

    private final Vertx vertx;
    private final NoteService noteService;

    public NoteIndex(Vertx vertx) {
        this.vertx = vertx;
        this.noteService = new NoteService(vertx);
    }

    public Router router() {
        var router = Router.router(vertx);

        var staticHandler = StaticHandler.create();
        router.route("/").handler(it -> it.redirect("/static/"));
        router.route("/static/*").handler(staticHandler);

        router.get("/list").handler(context ->
            noteService.find(null)
                .andThen(it ->
                    context.request()
                        .response()
                        .putHeader("Content-Type", "application/json")
                        .end(Json.encodeToBuffer(it.result()))
                )
        );

        router.post("/note").handler(BodyHandler.create()).handler(context ->
            noteService.save(context.body().asPojo(Note.class))
                .andThen(it ->
                    context
                        .request()
                        .response()
                        .putHeader("Content-Type", "application/json")
                        .end(Json.encodeToBuffer(it.result()))
                )
        );

        router.delete("/note/:id").handler(context ->
            noteService
                .logicDelete(new Note(context.pathParam("id"), null, null, null))
                .andThen(a -> context.request().response().end("ok"))
        );

        router.post("/upload").handler(BodyHandler.create().setDeleteUploadedFilesOnEnd(true))
            .handler(context ->
                context.fileUploads().forEach(e ->
                    context.vertx().fileSystem().readFile(e.uploadedFileName(), it ->
                        noteService.save(JsonUtil.toPojoList(it.result().getBytes(), Note.class))
                            .andThen(a -> context.request().response().end("Ok"))
                    )
                )
            );

        return router;
    }
}
