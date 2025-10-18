package net.cofcool.sourcebox.runner;

import static net.cofcool.sourcebox.ToolRunner.PASSWD_KEY;
import static net.cofcool.sourcebox.ToolRunner.USER_KEY;

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
import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import net.cofcool.sourcebox.App;
import net.cofcool.sourcebox.Tool;
import net.cofcool.sourcebox.Tool.Args;
import net.cofcool.sourcebox.Tool.RunnerType;
import net.cofcool.sourcebox.ToolContext;
import net.cofcool.sourcebox.ToolRunner;
import net.cofcool.sourcebox.WebTool;
import net.cofcool.sourcebox.WebTool.RouterTypeManger;
import net.cofcool.sourcebox.util.SqlRepository;
import net.cofcool.sourcebox.util.VertxDeployer;
import net.cofcool.sourcebox.util.VertxUtils;

@CustomLog
@RequiredArgsConstructor
public class WebVerticle extends AbstractVerticle implements VertxDeployer {

    static final Queue<ActionEvent> EVENT_QUEUE = new ConcurrentLinkedQueue<>();

    private Credentials usernamePasswordCredentials;
    private final RunnerType runnerType;
    private final Function<Tool, ToolContext> contextSupplier;

    private final RouterTypeManger manger = new RouterTypeManger();

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
                Integer.parseInt((String) App.getGlobalConfig(ToolRunner.PORT_KEY)),
                log
            );
        vertx.setPeriodic(600_000, l -> {
            if (EVENT_QUEUE.size() > 1000) {
                EVENT_QUEUE.clear();
            }
        });
    }

    @Override
    public Future<String> deploy(Vertx vertx, Verticle verticle, Args args) {
        args.getArgVal(USER_KEY).ifPresent(a -> {
            usernamePasswordCredentials = new UsernamePasswordCredentials(
                a,
                args.readArg(PASSWD_KEY)
                    .requiredVal("If username is not null, password must also not be null")
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

        router.get("/event")
            .handler(c -> {
                var a = new ArrayList<>();
                while (!EVENT_QUEUE.isEmpty()) {
                    a.add(EVENT_QUEUE.poll());
                }

                c.json(a);
            });

        VertxUtils.uploadRoute(
            router,
            (f, r) -> f.uploadedFileName(),
            null
        );

        var globalConfig = VertxDeployer.getSharedArgs(getClass().getSimpleName(), vertx);

        router.get("/config")
            .handler(c -> {
                c.json(globalConfig);
            });

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
                manger.registerWebRouter(
                    ((WebTool) tool).routerType(),
                    vertx,
                    r -> router.route("/*").subRouter(r)
                );
            } else {
                router.post(path).respond(r -> {
                    var args = new Args();
                    r.body().asJsonObject()
                        .forEach(e -> args.arg(e.getKey(), (String) e.getValue()));
                    var webToolContext = contextSupplier.apply(tool);
                    args.copyConfigFrom(globalConfig.removePrefix(toolName))
                        .copyConfigFrom(tool.config())
                        .context(webToolContext);

                    vertx.executeBlocking(() -> {
                            tool.run(args);
                            return Future.succeededFuture("success");
                        })
                        .onSuccess(a ->
                            EVENT_QUEUE.offer(new ActionEvent(a.result(), toolName, "finished")))
                        .onFailure(a -> {
                            EVENT_QUEUE.offer(
                                new ActionEvent("fail: " + a.getMessage(), toolName,
                                    "finished", false));
                            log.error("run tool error", a);
                        });
                    return Future.succeededFuture(webToolContext.toObject());
                });
            }
        }
        return router;
    }
}
