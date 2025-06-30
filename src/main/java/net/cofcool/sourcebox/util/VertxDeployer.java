package net.cofcool.sourcebox.util;

import io.vertx.core.Future;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import net.cofcool.sourcebox.App;
import net.cofcool.sourcebox.Tool.Args;

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
        return (Args) App.getGlobalConfig(getName(toolName));
    }

    static void sharedArgs(Vertx vertx, String toolName, Args args) {
        App.setGlobalConfig(getName(toolName), args);
    }

    static String getName(String toolName) {
        return String.join(".", toolName, ARGS_KEY);
    }

}
