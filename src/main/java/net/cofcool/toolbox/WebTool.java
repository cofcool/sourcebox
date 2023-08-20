package net.cofcool.toolbox;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;

public interface WebTool extends Tool {

    Router router(Vertx vertx);

}
