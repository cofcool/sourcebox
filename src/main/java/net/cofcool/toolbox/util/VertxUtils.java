package net.cofcool.toolbox.util;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import net.cofcool.toolbox.Logger;

public final class VertxUtils {

    public static <T> Handler<AsyncResult<T>> logResult(Logger log) {
        return a -> {
            if (a.failed()) {
                log.error("Server run error", a.cause());
            } else {
                log.info("Server run result is " + a.result());
            }
        };
    }

}
