package net.cofcool.sourcebox.runner;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import net.cofcool.sourcebox.App;
import net.cofcool.sourcebox.ToolRunner;
import net.cofcool.sourcebox.WebTool;
import net.cofcool.sourcebox.WebTool.RouterTypeManger;
import net.cofcool.sourcebox.util.SqlRepository;
import net.cofcool.sourcebox.util.VertxUtils;


@CustomLog
@RequiredArgsConstructor
public class CLIWebToolVerticle extends AbstractVerticle {

    private final WebTool tool;
    private final RouterTypeManger manger = new RouterTypeManger();

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        SqlRepository.init(vertx);
        VertxUtils
            .initHttpServer(
                vertx,
                startPromise,
                manger.registerWebRouter(
                    tool.routerType(),
                    vertx,
                    r -> {
                    }
                ),
                Integer.parseInt((String) App.getGlobalConfig(ToolRunner.PORT_KEY)),
                log
            );
    }

}
