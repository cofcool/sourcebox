package net.cofcool.toolbox.runner;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.LoggerHandler;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import lombok.CustomLog;
import net.cofcool.toolbox.App;
import net.cofcool.toolbox.Tool;
import net.cofcool.toolbox.Tool.Args;
import net.cofcool.toolbox.Tool.RunnerType;
import net.cofcool.toolbox.ToolContext;
import net.cofcool.toolbox.ToolRunner;
import net.cofcool.toolbox.WebTool;
import net.cofcool.toolbox.util.VertxUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
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
        run(Vertx.vertx(), args).onComplete(VertxUtils.logResult(log));
        return true;
    }

    Future<String> run(Vertx vertx, Args args) {
        this.args = args;
        return vertx.deployVerticle(this);
    }

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        var port = System.getProperty(PORT_KEY);
        VertxUtils
            .initHttpServer(
                vertx,
                startPromise,
                Routers.build(vertx, args),
                StringUtils.isEmpty(port) ? 8080 : Integer.parseInt(port),
                log
            );
    }

    private static class WebToolContext implements ToolContext {

        Map<String, String> out = new ConcurrentHashMap<>();

        @Override
        public ToolContext write(String name, String in) {
            out.put(name, in);
            return this;
        }

        @Override
        public RunnerType runnerType() {
            return RunnerType.WEB;
        }

        public JsonObject result() {
            String name;
            if (out.size() == 1) {
                name = out.values().toArray(String[]::new)[0];
            } else {
                name = VertxUtils.resourcePath(RandomStringUtils.randomAlphabetic(10) + ".zip");
                try (var zipOut = new ZipOutputStream(new FileOutputStream(name))) {
                    for (Entry<String, String> entry : out.entrySet()) {
                        zipOut.putNextEntry(new ZipEntry(entry.getKey()));
                        IOUtils.write(entry.getValue(), zipOut, StandardCharsets.UTF_8);
                    }
                } catch (IOException e) {
                    log.error("Write zip file error", e);
                    throw new RuntimeException(e);
                }
            }
            log.info("Generate file " + name + " ok");
            return JsonObject.of("result", name);
        }

    }

    private static class Routers {

        public static Router build(Vertx vertx, Args globalArgs) {
            var router = Router.router(vertx);
            var tools = App.supportTools(RunnerType.WEB);

            router.route().handler(VertxUtils.bodyHandler(null));
            router.route().handler(LoggerHandler.create());

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

            router.get("/resource/:name")
                .handler(r -> {
                    var name = r.pathParam("name");
                    r.attachment(name).response().sendFile(VertxUtils.resourcePath(name));
                });

            VertxUtils.uploadRoute(
                router,
                (f, r) -> f.uploadedFileName(),
                null
            );

            for (Tool tool : tools) {
                var path = "/" + tool.name().name();
                if (tool instanceof WebTool) {
                    router.route(path + "/*").subRouter(((WebTool) tool).router(vertx));
                } else {
                    router.post(path).respond(r -> {
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
            }

            return router;
        }
    }
}
