package net.cofcool.toolbox.internal;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import java.util.concurrent.TimeUnit;
import net.cofcool.toolbox.Tool;
import net.cofcool.toolbox.ToolName;
import net.cofcool.toolbox.runner.WebRunner;

public class NoteClient implements Tool {


    @Override
    public ToolName name() {
        return ToolName.noteCli;
    }

    @Override
    public void run(Args args) throws Exception {
        var client = WebClient.create(
            Vertx.vertx(),
            new WebClientOptions()
                .setDefaultPort(Integer.parseInt(args.readArg("port").val()))
                .setLocalAddress(args.readArg("addr").val())
        );

        var ret = client.request(HttpMethod.valueOf(args.readArg("method").val()), args.readArg("action").val())
            .send()
            .toCompletionStage()
            .toCompletableFuture()
            .get(10, TimeUnit.SECONDS)
            .bodyAsString();
        args.getContext().write(ret);
    }

    @Override
    public Args config() {
        return new Args()
            .arg(new Arg("port", WebRunner.PORT_VAL + "", "note server port", false, null))
            .arg(new Arg("addr", "127.0.0.1", "note server address", false, null))
            .arg(new Arg("action", null, "note action", true, "/list"))
            .arg(new Arg("method", HttpMethod.GET.name(), "request method", false, null));
    }
}
