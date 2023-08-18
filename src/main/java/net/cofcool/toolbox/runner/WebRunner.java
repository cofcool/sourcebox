package net.cofcool.toolbox.runner;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import java.util.Objects;
import lombok.CustomLog;
import net.cofcool.toolbox.App;
import net.cofcool.toolbox.Tool;
import net.cofcool.toolbox.Tool.Args;
import net.cofcool.toolbox.Tool.RunnerType;
import net.cofcool.toolbox.ToolContext;
import net.cofcool.toolbox.ToolRunner;
import org.apache.commons.lang3.StringUtils;

/**
 * config listen port by system env: {@link #PORT_KEY}
 */
@CustomLog
public class WebRunner extends AbstractVerticle implements ToolRunner {

    public static final String PORT_KEY = "toolbox.web.port";

    private Args args;

    @Override
    public boolean run(Args args) throws Exception {
        run(Vertx.vertx(), args);
        return true;
    }

    Future<String> run(Vertx vertx, Args args) {
        this.args = args;
        return vertx.deployVerticle(this);
    }

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        var server = vertx.createHttpServer();
        var port = System.getProperty(PORT_KEY);
        server
            .requestHandler(new Routers().build(vertx, args))
            .listen(
                StringUtils.isEmpty(port) ? 8080 : Integer.parseInt(port),
                http -> {
                    if (http.succeeded()) {
                        startPromise.complete();
                        log.info(String.format("Toolbox server started on port %s", http.result().actualPort()));
                    } else {
                        startPromise.fail(http.cause());
                        log.error("Toolbox server start error", http.cause());
                    }
                });
    }


    private static class WebToolContext implements ToolContext {

        private final StringBuilder sb = new StringBuilder();

        @Override
        public ToolContext write(Object val) {
            sb.append(Objects.toString(val, ""));
            return this;
        }

        public JsonObject result() {
            return new JsonObject().put("result", sb.toString());
        }

    }

    private static class Routers {

        public Router build(Vertx vertx, Args globalArgs) {
            var router = Router.router(vertx);
            var tools = App.supportTools(RunnerType.WEB);

            router.route().handler(BodyHandler.create());

            router.errorHandler(500, r -> {
                log.error("Request error", r.failure());
                r.json(new JsonObject().put("error", r.failure().getMessage()));
            });

            router.get("/")
                .respond(r -> Future.succeededFuture(tools.stream().map(Tool::name).toList()));

            router.get("/help")
                .respond(r ->
                    Future.succeededFuture(
                        tools.stream()
                            .map(t ->
                                new JsonObject().put(
                                    t.name().name(),
                                    JsonObject.of("desc", t.name().toString(), "help", t.config().toHelpString())
                                )
                            )
                            .reduce(new JsonObject(), JsonObject::mergeIn)
                    )
                );

            for (Tool tool : tools) {
                router.post("/" + tool.name().name()).respond(r -> {
                    var args = new Args();
                    r.body().asJsonObject().forEach(e -> args.arg(e.getKey(), (String) e.getValue()));
                    var webToolContext = new WebToolContext();
                    args.copyConfigFrom(globalArgs)
                        .copyConfigFrom(tool.config())
                        .context(webToolContext);

                    try {
                        tool.run(args);
                        return Future.succeededFuture(webToolContext.result());
                    } catch (Exception e) {
                        return Future.failedFuture(e);
                    }
                });
            }

            return router;
        }
    }
}
