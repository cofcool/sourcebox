package net.cofcool.toolbox;

import io.vertx.core.Future;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import net.cofcool.toolbox.util.VertxDeployer;

public interface WebTool extends Tool, VertxDeployer {

    Router router(Vertx vertx);

    @Override
    default Future<String> deploy(Vertx vertx, Verticle verticle, Args args) {
        if (vertx == null) {
            vertx = Vertx.vertx();
        }
        if (verticle == null) {
            verticle = (Verticle) this;
        }
        if (args == null) {
            args = config();
        }
        if (args != null) {
            VertxDeployer.sharedArgs(vertx, name().name(), args);
        }
        return vertx.deployVerticle(verticle);
    }
}
