package net.cofcool.toolbox.simplenote

import io.vertx.core.Vertx
import io.vertx.core.json.Json
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.StaticHandler
import net.cofcool.toolbox.simplenote.internal.NoteService

class Index {
    lateinit var noteService: NoteService

    fun router(vertx: Vertx): Router {
        noteService = NoteService(vertx)

        val router = Router.router(vertx)

        val staticHandler = StaticHandler.create()
        router.route("/").handler { it.redirect("/static/") }
        router.route("/static/*").handler(staticHandler)

        router.get("/list").handler { context ->
            noteService.find(null).andThen {
                context.request()
                    .response()
                    .putHeader("Content-Type", "application/json")
                    .end(Json.encodeToBuffer(it.result()))
            }
        }

        router.post("/note").handler(BodyHandler.create()).handler { context ->
            noteService.save(context.body().asPojo(Note::class.java)).andThen {
                context.request().response().end(Json.encodeToBuffer(it.result()))
            }
        }

        router.delete("/note/:id").handler { context ->
            val note = Note()
            note.id = context.pathParam("id")
            noteService.logicDelete(note).andThen {
                context.request().response().end("ok")
            }
        }

        router.post("/upload").handler(BodyHandler.create().setDeleteUploadedFilesOnEnd(true)).handler { context ->
            context.fileUploads().forEach {
                context.vertx().fileSystem().readFile(it.uploadedFileName()) {
                    noteService.save(Config.toPojoList(it.result(), Note::class.java)).andThen {
                        context.request().response().end("Ok")
                    }
                }
            }
        }

        return router
    }

}