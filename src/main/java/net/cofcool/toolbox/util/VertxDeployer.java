package net.cofcool.toolbox.util;

import io.vertx.core.Future;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import net.cofcool.toolbox.Tool.Args;

public interface VertxDeployer {

    String ARGS_KEY = "RUN_ARGS_KEY";

    default Future<String> deploy(Args args) {
        return deploy(null, null, args);
    }

    default Future<String> deploy() {
        return deploy(null, null, null);
    }

    default Future<String> deploy(Vertx vertx) {
        return deploy(vertx, null, null);
    }

    default Future<String> deploy(Vertx vertx, Verticle verticle) {
        return deploy(vertx, verticle, null);
    }

    default Future<String> deploy(Vertx vertx, Verticle verticle, Args args) {
        if (vertx == null) {
            vertx = Vertx.vertx();
        }
        if (verticle == null) {
            verticle = (Verticle) this;
        }
        if (args != null) {
            sharedArgs(vertx, getClass().getSimpleName(), args);
        }
        return vertx.deployVerticle(verticle);
    }

    static Args getSharedArgs(String toolName,Vertx vertx) {
        return (Args) vertx.sharedData().getLocalMap(getName(toolName)).get(ARGS_KEY);
    }

    static void sharedArgs(Vertx vertx, String toolName, Args args) {
        vertx.sharedData().getLocalMap(getName(toolName)).put(ARGS_KEY, args);
    }

    static String getName(String toolName) {
        return toolName + "." + ARGS_KEY;
    }

}
