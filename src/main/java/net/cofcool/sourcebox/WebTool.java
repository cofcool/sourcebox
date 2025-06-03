package net.cofcool.sourcebox;

import io.vertx.core.Future;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpRequest.Builder;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import net.cofcool.sourcebox.util.JsonUtil;
import net.cofcool.sourcebox.util.Utils;
import net.cofcool.sourcebox.util.VertxDeployer;

public interface WebTool extends Tool, VertxDeployer {

    interface WebRouter {

        Router router(Vertx vertx);
    }

    Class<? extends WebRouter> routerType();

    default boolean supportCommand() {
        return false;
    }

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

    default String getPort() {
        throw new UnsupportedOperationException();
    }

    default <T> T requestLocalData(String methodPath, Class<T> bodyType,
        Function<Builder, Builder> requestAction) {
        return Utils.requestLocalData(getPort(), methodPath, bodyType, requestAction, e -> {
            if (e != null) {
                throw new IllegalStateException("request " + methodPath + " error", e);
            }
        });
    }

    default <T> T getRequestLocalData(String methodPath, Class<T> bodyType) {
        return requestLocalData(methodPath, bodyType, Builder::GET);
    }

    default <T> T postRequestLocalData(String methodPath, Object params, Class<T> bodyType) {
        return requestLocalData(methodPath, bodyType, b -> {
            b.POST(BodyPublishers.ofString(JsonUtil.toJson(params)));
            return b;
        });
    }

    default boolean deleteRequestLocalData(String methodPath) {
        var f = new AtomicBoolean();
        Utils.requestLocalData(getPort(), methodPath, Map.class, Builder::DELETE, e -> {
            if (e != null) {
                getLogger().error("request " + methodPath + " error", e);
                f.set(true);
            }
        });

        return f.get();
    }

    class RouterTypeManger {

        private final Map<Class<? extends WebRouter>, Router> routerMap = new ConcurrentHashMap<>();

        public Router registerWebRouter(Class<? extends WebRouter> clazz, Vertx vertx,
            Consumer<Router> action) {
            return routerMap.computeIfAbsent(clazz, k -> {
                var r = Utils.instance(clazz).router(vertx);
                action.accept(r);
                return r;
            });
        }
    }
}
