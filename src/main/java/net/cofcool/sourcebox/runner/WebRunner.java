package net.cofcool.sourcebox.runner;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.authentication.CredentialValidationException;
import io.vertx.ext.auth.authentication.Credentials;
import io.vertx.ext.auth.authentication.UsernamePasswordCredentials;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.LoggerHandler;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import net.cofcool.sourcebox.App;
import net.cofcool.sourcebox.Tool;
import net.cofcool.sourcebox.Tool.Args;
import net.cofcool.sourcebox.Tool.RunnerType;
import net.cofcool.sourcebox.ToolContext;
import net.cofcool.sourcebox.ToolRunner;
import net.cofcool.sourcebox.WebTool;
import net.cofcool.sourcebox.util.SqlRepository;
import net.cofcool.sourcebox.util.VertxDeployer;
import net.cofcool.sourcebox.util.VertxUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;

/**
 * config listen port: {@link #PORT_KEY}
 */
@CustomLog
public class WebRunner implements ToolRunner {

    public static final String PORT_KEY = "web.port";
    public static final String USER_KEY = "web.username";
    public static final String PASSWD_KEY = "web.password";
    public static final int PORT_VAL = 38080;

    @Override
    public boolean run(Args args) throws Exception {
        Vertx v = Vertx.vertx();
        new WebVerticle(RunnerType.WEB, WebToolContext::new).deploy(v, null, args)
            .onComplete(VertxUtils.logResult(log, e -> v.close()));
        return true;
    }

    @Override
    public String help() {
        return String.join(", ", USER_KEY, PASSWD_KEY, PORT_KEY);
    }


    @RequiredArgsConstructor
    static class WebVerticle extends AbstractVerticle implements VertxDeployer {


        private Credentials usernamePasswordCredentials;
        private int port = PORT_VAL;
        private final RunnerType runnerType;
        private final Supplier<ToolContext> contextSupplier;

        @Override
        public void init(Vertx vertx, Context context) {
            super.init(vertx, context);
            SqlRepository.init(vertx);
        }

        @Override
        public void start(Promise<Void> startPromise) throws Exception {
            VertxUtils
                .initHttpServer(
                    vertx,
                    startPromise,
                    build(usernamePasswordCredentials),
                    port,
                    log
                );
        }

        @Override
        public Future<String> deploy(Vertx vertx, Verticle verticle, Args args) {
            args.getArgVal(USER_KEY).ifPresent(a -> {
                usernamePasswordCredentials = new UsernamePasswordCredentials(
                    a,
                    args.readArg(PASSWD_KEY)
                        .getRequiredVal("If username is not null, password must also not be null")
                ) {
                    @Override
                    public <V> void checkValid(V arg) throws CredentialValidationException {
                        super.checkValid(arg);
                        if (arg instanceof JsonObject credentials) {
                            if (!(getUsername().equals(credentials.getString("username"))
                                && getPassword().equals(credentials.getString("password")))) {
                                throw new CredentialValidationException(
                                    "Username or password error");
                            }
                        }
                    }
                };
                log.info("Enable basicAuth");
            });
            args.getArgVal(PORT_KEY).ifPresent(a -> port = Integer.parseInt(a));
            return VertxDeployer.super.deploy(vertx, verticle, args);
        }

        private Router build(Credentials credentials) {
            var router = Router.router(vertx);
            var tools = App.supportTools(runnerType);

            router.route().handler(VertxUtils.bodyHandler(null));
            router.route().handler(LoggerHandler.create());

            if (credentials != null) {
                VertxUtils.basicAuth(router, vertx, credentials);
            }

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
                                    JsonObject.of("desc", t.name().toString(), "help",
                                        t.config().toHelpString())
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

            var globalConfig = VertxDeployer.getSharedArgs(getClass().getSimpleName(), vertx);
            for (Tool tool : tools) {
                String toolName = tool.name().name();
                var path = "/" + toolName;
                if (tool instanceof WebTool) {
                    VertxDeployer.sharedArgs(
                        vertx,
                        toolName,
                        new Args().copyConfigFrom(globalConfig.removePrefix(toolName))
                            .copyConfigFrom(tool.config())
                    );
                    router.route(path + "/*").subRouter(((WebTool) tool).router(vertx));
                } else {
                    router.post(path).respond(r -> {
                        var args = new Args();
                        r.body().asJsonObject()
                            .forEach(e -> args.arg(e.getKey(), (String) e.getValue()));
                        var webToolContext = contextSupplier.get();
                        args.copyConfigFrom(tool.config())
                            .copyConfigFrom(globalConfig.removePrefix(toolName))
                            .context(webToolContext);

                        try {
                            tool.run(args);
                            return Future.succeededFuture(webToolContext.toObject());
                        } catch (Exception e) {
                            return Future.failedFuture(e);
                        }
                    });
                }
            }
            return router;
        }
    }

    static class WebToolContext implements ToolContext {

        Map<String, String> out = new ConcurrentHashMap<>();

        @Override
        public ToolContext write(String name, String in) {
            if (name == null) {
                name = ToolContext.randomName();
            }
            out.put(name, in);
            return this;
        }

        @Override
        public RunnerType runnerType() {
            return RunnerType.WEB;
        }

        @Override
        public JsonObject toObject() {
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
                log.info("Generate file {0} ok", name);
            }
            return JsonObject.of("result", name);
        }


    }
}
