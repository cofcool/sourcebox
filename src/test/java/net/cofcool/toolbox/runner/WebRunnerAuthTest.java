package net.cofcool.toolbox.runner;

import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import net.cofcool.toolbox.Tool.Args;
import net.cofcool.toolbox.ToolName;
import net.cofcool.toolbox.internal.simplenote.NoteConfig;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
public class WebRunnerAuthTest {

    static int port = RandomUtils.nextInt(38000, WebRunner.PORT_VAL);

    @BeforeEach
    void deployVerticle(Vertx vertx, VertxTestContext testContext) throws Exception {
        System.setProperty("logging.debug", "true");
        System.setProperty("upload.dir", "target/file-uploads");
        new WebRunner()
            .deploy(
                vertx,
                null,
                new Args()
                    .arg(ToolName.note.name() + "." + NoteConfig.PATH_KEY, "./target/")
                    .arg(WebRunner.USER_KEY, "demo")
                    .arg(WebRunner.PASSWD_KEY, "demo")
                    .arg(WebRunner.PORT_KEY, port + "")
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
