package net.cofcool.toolbox.simplenote

import io.vertx.core.AbstractVerticle
import io.vertx.core.Context
import io.vertx.core.Promise
import io.vertx.core.Vertx

fun main(args: Array<String>) {
  Vertx.vertx().deployVerticle(SimpleNoteVerticle())
}

class SimpleNoteVerticle : AbstractVerticle() {

  override fun init(vertx: Vertx, context: Context?) {
    super.init(vertx, context)
    Config.init()
    vertx.eventBus().registerDefaultCodec(Note::class.java, NoteCodec())
  }

  override fun start(startPromise: Promise<Void>) {
    val server = vertx.createHttpServer();
    server
      .requestHandler(Index().router(vertx))
      .listen(context.config().getInteger(Config.PORT_KEY, Config.PORT_VAL)) { http ->
        if (http.succeeded()) {
          startPromise.complete()
          println("HTTP server started on port 8888")
        } else {
          startPromise.fail(http.cause());
        }
      }
    vertx.eventBus().publish(Config.STARTED, context.config())
  }
}
