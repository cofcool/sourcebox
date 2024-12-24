package net.cofcool.sourcebox.runner;

import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import net.cofcool.sourcebox.Tool.Args;
import net.cofcool.sourcebox.Tool.RunnerType;
import net.cofcool.sourcebox.ToolName;
import net.cofcool.sourcebox.internal.simplenote.NoteConfig;
import net.cofcool.sourcebox.runner.WebRunner.WebToolContext;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
public class WebRunnerAuthTest {

    static int port = RandomUtils.nextInt(38000, WebRunner.PORT_VAL);

    @BeforeAll
    static void deployVerticle(Vertx vertx, VertxTestContext testContext) throws Exception {
        System.setProperty("logging.debug", "true");
        System.setProperty("upload.dir", "target/file-uploads");
        new WebVerticle(RunnerType.WEB, a -> new WebToolContext())
            .deploy(
                vertx,
                null,
                new Args()
                    .arg(ToolName.note.name() + "." + NoteConfig.PATH_KEY, "./target/")
                    .arg(WebVerticle.USER_KEY, "demo")
                    .arg(WebVerticle.PASSWD_KEY, "demo")
                    .arg(WebVerticle.PORT_KEY, port + "")
            )
            .onComplete(testContext.succeeding(t -> testContext.completeNow()));
    }


    @Test
    void run(Vertx vertx, VertxTestContext testContext) {
        var client = WebClient.create(vertx);
        client
            .get(port, "127.0.0.1", "/")
            .basicAuthentication("demo", "demo")
            .send()
            .onComplete(testContext.succeeding(r -> testContext.verify(() -> {
                Assertions.assertEquals(200, r.statusCode());
                testContext.completeNow();
            })));
    }

    @Test
    void runWithAuthFail(Vertx vertx, VertxTestContext testContext) {
        var client = WebClient.create(vertx);
        client
            .get(port, "127.0.0.1", "/")
            .send()
            .onComplete(testContext.succeeding(r -> testContext.verify(() -> {
                Assertions.assertEquals(401, r.statusCode());
                System.out.println(r.bodyAsString());
                testContext.completeNow();
            })));
    }

}
